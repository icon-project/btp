package pra

import (
	"encoding/json"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/config"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
)

type receiverOptions struct {
	config.FileConfig
	RelayBtpAddress chain.BtpAddress `json:"relayBtpAddress"`
	RelayEndpoint   string           `json:"relayEndpoint"`
	RelayOffSet     int64            `json:"relayOffset"`
	MtaRootSize     int              `json:"mtaRootSize"`
	PraBmvAddress   chain.BtpAddress `json:"iconBmvAddress"`
	DstEndpoint     string
}

type Receiver struct {
	c                       *Client
	relayReceiver           relayReceiver
	src                     chain.BtpAddress
	dst                     chain.BtpAddress
	l                       log.Logger
	opt                     receiverOptions
	parachainId             substrate.SubstrateParachainId
	rxSeq                   uint64
	foundNextEventFromRxSeq bool
}

func NewReceiver(src, dst chain.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger, cfgAbsBaseDir string, dstEndpoint string) chain.Receiver {
	r := &Receiver{
		src: src,
		dst: dst,
		l:   l,
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &r.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	r.c = NewClient(endpoint, src.Account(), l)
	paraId, err := r.c.subClient.GetParachainId()
	if err != nil {
		l.Panicf("fail to get parachainId %v", err)
	}
	r.parachainId = *paraId

	if len(r.opt.RelayEndpoint) > 0 {
		r.opt.BaseDir = cfgAbsBaseDir
		r.opt.DstEndpoint = dstEndpoint
		r.relayReceiver = NewRelayReceiver(r.opt, l)
		if err != nil {
			l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
		}
	}
	return r
}

func (r *Receiver) newParaBlockUpdate(v *BlockNotification) (*chain.BlockUpdate, error) {
	var err error
	bu := &chain.BlockUpdate{
		Height:    int64(v.Height),
		BlockHash: v.Hash[:],
	}

	r.l.Debugf("newParaBlockUpdate: %d", v.Height)
	var update chain.ParaChainBlockUpdateExtra
	if update.ScaleEncodedBlockHeader, err = substrate.NewEncodedSubstrateHeader(*v.Header); err != nil {
		return nil, err
	}

	if len(r.opt.RelayEndpoint) > 0 {
		var err error
		vd, err := r.c.subClient.GetValidationData(v.Hash)
		if err != nil {
			return nil, err
		}

		update.FinalityProofs, err = r.relayReceiver.newParaFinalityProof(vd, r.parachainId, v.Hash, v.Height)
		if err != nil {
			r.l.Tracef("r.relayReceiver.newParaFinalityProof error: %+v", err)
			return nil, err
		}
	} else {
		// For local testing without relay chain
		update.FinalityProofs = [][]byte{nil}
	}

	r.l.Tracef("block proof rlp encode")
	bu.Proof, err = codec.RLP.MarshalToBytes(&update)
	if err != nil {
		r.l.Debugf("codec.RLP.MarshalToBytes(&update) error: %+v", err)
		return nil, err
	}

	bu.Header = update.ScaleEncodedBlockHeader
	return bu, nil
}

func (r *Receiver) getEvmLogEvents(hash substrate.SubstrateHash) ([]substrate.EventEVMLog, error) {
	r.l.Tracef("start getEvmLogEvents")
	events, err := r.c.subClient.GetSystemEvents(hash, "EVM", "Log")
	if err != nil {
		r.l.Debugf("getEvmLogEvents: fails to get EVM_Log event")
	}
	evmLogEvents := make([]substrate.EventEVMLog, 0)
	r.l.Tracef("decode substrate.NewEventEVMLog(event)")
	for _, event := range events {
		evmLogEvents = append(evmLogEvents, substrate.NewEventEVMLog(event))
	}

	return evmLogEvents, err
}

func (r *Receiver) newReceiptProofs(v *BlockNotification) ([]*chain.ReceiptProof, error) {
	r.l.Tracef("new receipt proof start get evm log events")
	rps := make([]*chain.ReceiptProof, 0)
	els, err := r.getEvmLogEvents(v.Hash)
	r.l.Tracef("got evm log events")
	if err != nil {
		r.l.Debugf("r.getEvmLogEvents(v.Hash) error: %+v", err)
		return nil, err
	}

	if len(els) > 0 {
		rp := &chain.ReceiptProof{
			Height: int64(v.Height),
		}

		r.l.Tracef("filter evm log event")
		for _, e := range els {
			if !e.CompareAddressCaseInsensitive(r.src.Account()) {
				continue
			}

			if bmcMsg, err := r.c.bmc.ParseMessage(NewEvmLog(e)); err == nil {
				rp.Events = append(rp.Events, &chain.Event{
					Message:  bmcMsg.Msg,
					Next:     chain.BtpAddress(bmcMsg.Next),
					Sequence: bmcMsg.Seq.Int64(),
				})

				r.l.Debugf("newReceiptProofs: newEvent Seq %d", rp.Events[len(rp.Events)-1].Sequence)
				if bmcMsg.Seq.Int64() == int64(r.rxSeq+1) {
					r.foundNextEventFromRxSeq = true
				}
			}
		}

		// only get ReceiptProof that has right Events
		if len(rp.Events) > 0 {
			r.l.Debugf("newReceiptProofs: build StateProof %d", v.Height)
			key, err := r.c.subClient.GetSystemEventStorageKey(v.Hash)
			if err != nil {
				r.l.Debugf("r.c.subClient.GetSystemEventStorageKey(v.Hash) error: %+v", err)
				return nil, err
			}

			readProof, err := r.c.SubstrateClient().GetReadProof(key, v.Hash)
			if err != nil {
				r.l.Debugf("r.c.SubstrateClient().GetReadProof(key, v.Hash) error: %+v", err)
				return nil, err
			}

			if rp.Proof, err = codec.RLP.MarshalToBytes(NewStateProof(key, &readProof)); err != nil {
				r.l.Debugf("codec.RLP.MarshalToBytes(NewStateProof(key, &readProof) error: %+v", err)
				return nil, err
			}

			rps = append(rps, rp)
		}
	}
	return rps, nil
}

func (r *Receiver) ReceiveLoop(height int64, seq int64, cb chain.ReceiveCallback, scb func()) error {
	if seq < 1 {
		r.foundNextEventFromRxSeq = true
	}

	r.rxSeq = uint64(seq)

	if err := r.c.MonitorBlock(uint64(height), true, func(v *BlockNotification) error {
		var err error
		var bu *chain.BlockUpdate
		var sp []*chain.ReceiptProof
		if bu, err = r.newParaBlockUpdate(v); err != nil {
			r.l.Debugf("r.newParaBlockUpdate(v) error: %+v", err)
			return errors.Wrap(err, "ReceiveLoop: newParaBlockUpdate")
		}

		if sp, err = r.newReceiptProofs(v); err != nil {
			r.l.Debugf("r.newReceiptProofs(v) error: %+v", err)
			return errors.Wrap(err, "ReceiveLoop: newReceiptProofs")
		} else if r.foundNextEventFromRxSeq {
			cb(bu, sp)
		} else {
			cb(bu, nil)
		}
		return nil
	}); err != nil {
		return errors.Wrap(err, "ReceiveLoop: parachain, got err")
	}

	return nil
}

func (r *Receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}
