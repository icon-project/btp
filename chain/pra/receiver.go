package pra

import (
	"encoding/json"
	"fmt"

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

func (r *Receiver) newBlockUpdate(v *BlockNotification) (*chain.BlockUpdate, error) {
	var err error
	bu := &chain.BlockUpdate{
		Height:    int64(v.Height),
		BlockHash: v.Hash[:],
	}

	// Justification required, when update validators list
	if len(v.Events.Grandpa_NewAuthorities) > 0 {
		signedBlock, err := r.c.subAPI.RPC.Chain.GetBlock(v.Hash)
		if err != nil {
			return nil, err
		}

		if bu.Header, err = codec.RLP.MarshalToBytes(SignedHeader{
			Header:        *v.Header,
			Justification: signedBlock.Justification,
		}); err != nil {
			return nil, err
		}
	} else {
		if bu.Header, err = codec.RLP.MarshalToBytes(v.Header); err != nil {
			return nil, err
		}
	}

	// TODO subscribe to GrandpaJustification with a RWLock
	// if bu.Proof, err = codec.RLP.MarshalToBytes(); err != nil {
	// 	return nil, err
	// }

	return bu, nil
}

func (r *Receiver) newReceiptProofs(v *BlockNotification) ([]*chain.ReceiptProof, error) {
	rps := make([]*chain.ReceiptProof, 0)

	if len(v.Events.EVM_Log) > 0 {
		for _, e := range v.Events.EVM_Log {
			if r.c.IsSendMessageEvent(e) {
				key, err := r.c.getSystemEventReadProofKey(v.Hash)
				if err != nil {
					return nil, err
				}

				proof, err := r.c.getReadProof(key, v.Hash)
				if err != nil {
					return nil, err
				}

				rp := &chain.ReceiptProof{}
				if rp.Proof, err = codec.RLP.MarshalToBytes(proof); err != nil {
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
