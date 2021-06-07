package pra

import (
	"encoding/hex"
	"encoding/json"

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

	if bu.Header, err = codec.RLP.MarshalToBytes(v.Header); err != nil {
		return nil, err
	}

	// https://github.com/w3f/consensus
	// Nominated Proof of Staking
	// if bu.Proof, err = codec.RLP.MarshalToBytes(v.Header); err != nil {
	// 	return nil, err
	// }

	return bu, nil
}

func (r *Receiver) newReceiptProofs(v *BlockNotification) ([]*chain.ReceiptProof, error) {
	rps := make([]*chain.ReceiptProof, 0)
	events := SubstateWithFrontierEventRecord{}

	if len(events.EVM_Log) > 0 {
		for _, e := range events.EVM_Log {
			address := hex.EncodeToString(e.Log.Address[:])
			// TODO filter with topics and data
			if address == r.bmcAddress {
				key, err := r.c.getSystemEventReadProofKey()
				if err != nil {
					return nil, err
				}

				proof, err := r.c.GetReadProof(key, v.Hash)
				if err != nil {
					return nil, err
				}

				rp := &chain.ReceiptProof{}
				if rp.Proof, err = codec.RLP.MarshalToBytes(proof); err != nil {
					return nil, err
				}
				rps = append(rps, rp)
			}
			continue
		}
	}

	return rps, nil
}

func (r *Receiver) ReceiveLoop(height int64, seq int64, cb chain.ReceiveCallback, scb func()) error {
	if seq < 1 {
		r.isFoundOffsetBySeq = true
	}

	return r.c.MonitorSubstrateBlock(uint64(height), func(v *BlockNotification) error {
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
	})
}

func (r *Receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}
