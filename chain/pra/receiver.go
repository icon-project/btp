package pra

import (
	"encoding/json"
	"fmt"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/pra/frontier"
	"github.com/icon-project/btp/chain/pra/moonriver"
	"github.com/icon-project/btp/chain/pra/substrate"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

type receiverOptions struct {
	RelayEndpoint string `json:"relay_endpoint"`
	IconEndpoint  string `json:"icon_endpoint"`
	PraBmvAddress string `json:"icon_bmv_address"`
}

type Receiver struct {
	c                           *Client
	relayReceiver               relayReceiver
	src                         chain.BtpAddress
	dst                         chain.BtpAddress
	l                           log.Logger
	opt                         receiverOptions
	rxSeq                       uint64
	isFoundMessageEventByOffset bool
}

func NewReceiver(src, dst chain.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) chain.Receiver {
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
	r.c = NewClient(endpoint, src.ContractAddress(), l)
	if len(r.opt.RelayEndpoint) > 0 && len(r.opt.IconEndpoint) > 0 {
		r.relayReceiver = NewRelayReceiver(r.opt.RelayEndpoint, r.opt.IconEndpoint, r.opt.PraBmvAddress, l)
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

	var update BlockUpdate
	if update.ScaleEncodedBlockHeader, err = substrate.NewEncodedSubstrateHeader(*v.Header); err != nil {
		return nil, err
	}

	if len(r.opt.RelayEndpoint) > 0 {
		var err error
		vd, err := r.c.subClient.GetValidationData(v.Hash)
		if err != nil {
			return nil, err
		}

		update.FinalityProof, err = r.relayReceiver.newParaFinalityProof(vd)
		if err != nil {
			return nil, err
		}
	} else {
		// For local testing without relay chain
		update.FinalityProof = nil
	}

	bu.Proof, err = codec.RLP.MarshalToBytes(&update)
	if err != nil {
		return nil, err
	}

	bu.Header = update.ScaleEncodedBlockHeader
	return bu, nil
}

func (r *Receiver) getEvmLogEvents(v *BlockNotification) ([]frontier.EventEVMLog, error) {
	meta, err := r.c.subClient.GetMetadata(v.Hash)
	if err != nil {
		return nil, err
	}

	key, err := r.c.subClient.CreateStorageKey(meta, "System", "Events", nil, nil)
	if err != nil {
		return nil, err
	}

	sdr, err := r.c.subClient.GetStorageRaw(key, v.Hash)
	if err != nil {
		return nil, err
	}

	// Build parachain adapter here
	records := &moonriver.MoonriverEventRecord{}
	if err = substrate.SubstrateEventRecordsRaw(*sdr).DecodeEventRecords(meta, records); err != nil {
		return nil, err
	}

	return records.EVM_Log, nil
}

func (r *Receiver) newReceiptProofs(v *BlockNotification) ([]*chain.ReceiptProof, error) {
	rps := make([]*chain.ReceiptProof, 0)
	els, err := r.getEvmLogEvents(v)
	if err != nil {
		return nil, err
	}

	if len(els) > 0 {
		rp := &chain.ReceiptProof{
			Height: int64(v.Height),
		}

		for _, e := range els {
			if !e.CompareAddressCaseInsensitive(r.src.ContractAddress()) {
				continue
			}

			if bmcMsg, err := r.c.bmc.ParseMessage(NewEvmLog(e)); err == nil {
				rp.Events = append(rp.Events, &chain.Event{
					Message:  bmcMsg.Msg,
					Next:     chain.BtpAddress(bmcMsg.Next),
					Sequence: bmcMsg.Seq.Int64(),
				})

				if bmcMsg.Seq.Int64() == int64(r.rxSeq) {
					r.isFoundMessageEventByOffset = true
				}
			}
		}

		// only get ReceiptProof that has right Events
		if len(rp.Events) > 0 {
			key, err := r.c.CreateSystemEventsStorageKey(v.Hash)
			if err != nil {
				return nil, err
			}

			readProof, err := r.c.SubstrateClient().GetReadProof(key, v.Hash)
			if err != nil {
				return nil, err
			}

			if rp.Proof, err = codec.RLP.MarshalToBytes(NewStateProof(key, &readProof)); err != nil {
				return nil, err
			}

			rps = append(rps, rp)
		}
	}
	return rps, nil
}

func (r *Receiver) ReceiveLoop(height int64, seq int64, cb chain.ReceiveCallback, scb func()) error {
	if seq < 1 {
		r.isFoundMessageEventByOffset = true
	}

	r.rxSeq = uint64(seq)

	if err := r.c.MonitorBlock(uint64(height), true, func(v *BlockNotification) error {
		var err error
		var bu *chain.BlockUpdate
		var sp []*chain.ReceiptProof
		if bu, err = r.newParaBlockUpdate(v); err != nil {
			return err
		}

		if sp, err = r.newReceiptProofs(v); err != nil {
			return err
		} else if r.isFoundMessageEventByOffset {
			cb(bu, sp)
		} else {
			cb(bu, nil)
		}
		return nil
	}); err != nil {
		return fmt.Errorf("ReceiveLoop parachain, got err: %v", err)
	}

	return nil
}

func (r *Receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}
