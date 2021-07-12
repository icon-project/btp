package pra

import (
	"encoding/json"
	"fmt"

	"github.com/centrifuge/go-substrate-rpc-client/v3/types"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

type Receiver struct {
	c   *Client
	src chain.BtpAddress
	dst chain.BtpAddress
	l   log.Logger

	opt struct {
		RelayEndpoint string
	}
	isFoundOffsetBySeq bool
	bmcAddress         string
}

func NewReceiver(src, dst chain.BtpAddress, endpoint string, opt map[string]interface{}, l log.Logger) chain.Receiver {
	r := &Receiver{
		src:        src,
		dst:        dst,
		l:          l,
		bmcAddress: src.ContractAddress(),
	}
	b, err := json.Marshal(opt)
	if err != nil {
		l.Panicf("fail to marshal opt:%#v err:%+v", opt, err)
	}
	if err = json.Unmarshal(b, &r.opt); err != nil {
		l.Panicf("fail to unmarshal opt:%#v err:%+v", opt, err)
	}
	r.c = NewClient(endpoint, src.ContractAddress(), l)
	return r
}

func (r *Receiver) newFinalityProof(v *BlockNotification) ([]byte, error) {
	// For edgeware only
	// // Justification required, when update validators list
	// if len(v.Events.Grandpa_NewAuthorities) > 0 {
	// 	signedBlock, err := r.c.subAPI.RPC.Chain.GetBlock(v.Hash.Hash())
	// 	if err != nil {
	// 		return nil, err
	// 	}
	// }

	// TODO verify this with kusama relay chain
	return nil, nil
}

func (r *Receiver) newBlockUpdate(v *BlockNotification) (*chain.BlockUpdate, error) {
	var err error
	bu := &chain.BlockUpdate{
		Height:    int64(v.Height),
		BlockHash: v.Hash[:],
	}

	var update BlockUpdate
	if update.ScaleEncodedBlockHeader, err = types.EncodeToBytes(v.Header); err != nil {
		return nil, err
	}

	if update.FinalityProof, err = r.newFinalityProof(v); err != nil {
		return nil, err
	}

	bu.Proof, err = codec.RLP.MarshalToBytes(&update)
	if err != nil {
		return nil, err
	}

	bu.Header = update.ScaleEncodedBlockHeader
	return bu, nil
}

func (r *Receiver) newReceiptProofs(v *BlockNotification) ([]*chain.ReceiptProof, error) {
	rps := make([]*chain.ReceiptProof, 0)

	if len(v.Events.EVM_Log) > 0 {
		for _, e := range v.Events.EVM_Log {
			if r.c.IsSendMessageEvent(e) {
				key, err := r.c.CreateSystemEventsStorageKey(v.Hash)
				if err != nil {
					return nil, err
				}

				proof, err := r.c.SubstrateClient().GetReadProof(key, v.Hash)
				if err != nil {
					return nil, err
				}

				rp := &chain.ReceiptProof{}
				if rp.Proof, err = codec.RLP.MarshalToBytes(&StateProof{
					Key:   key,
					Value: proof.Proof,
				}); err != nil {
					return nil, err
				}

				rps = append(rps, rp)
				continue
			}
		}
	}

	return rps, nil
}

func (r *Receiver) ReceiveLoop(height int64, seq int64, cb chain.ReceiveCallback, scb func()) error {
	if seq < 1 {
		r.isFoundOffsetBySeq = true
	}

	if err := r.c.MonitorBlock(uint64(height), true, func(v *BlockNotification) error {
		var err error
		var bu *chain.BlockUpdate
		var sp []*chain.ReceiptProof
		if bu, err = r.newBlockUpdate(v); err != nil {
			return err
		}

		if sp, err = r.newReceiptProofs(v); err != nil {
			return err
		} else if r.isFoundOffsetBySeq {
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
