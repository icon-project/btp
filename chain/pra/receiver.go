package pra

import (
	"encoding/hex"
	"encoding/json"
	"fmt"

	"github.com/icon-project/btp/chain"
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

func (r *Receiver) newBlockUpdate(v *BlockNotification) (*BlockUpdate, error) {
	return nil, nil
}

func (r *Receiver) newReceiptProofs(v *BlockNotification) (*StateProof, error) {
	events := SubstateWithFrontierEventRecord{}

	metadata := r.c.getMetadata()
	// ignore this error because system event of each substrate-based is different, we only care module_events EVM_Log
	_ = v.Events.DecodeEventRecords(metadata, events)

	if len(events.EVM_Log) > 0 {
		for _, e := range events.EVM_Log {
			address := hex.EncodeToString(e.Log.Address[:])
			// TODO filter with topics and data
			if address == r.bmcAddress {
				key, err := r.c.getSystemEventReadProofKey()
				if err != nil {
					return nil, err
				}

				proofs, err := r.c.getReadProof(key, &v.Hash)
				if err != nil {
					return nil, err
				}

				sp := &StateProof{
					Key:    key,
					Proofs: proofs,
				}
				return sp, nil
			}
			continue
		}

	}

	return nil, nil
}

func (r *Receiver) ReceiveLoop(height int64, seq int64, cb chain.ReceiveCallback, scb func()) error {
	if seq < 1 {
		r.isFoundOffsetBySeq = true
	}

	return r.c.MonitorSubstrateBlock(uint64(height), func(v *BlockNotification) error {
		var err error
		var bu *BlockUpdate
		var sp *StateProof
		if bu, err = r.newBlockUpdate(v); err != nil {
			return err
		}

		if sp, err = r.newReceiptProofs(v); err != nil {
			return err
		} else if r.isFoundOffsetBySeq {
			fmt.Printf("Call BlockUpdate %+v\n", bu)
			fmt.Printf("Call StateProof %+v\n", sp)
			// TODO this can't work because BlockUpdate is incompatible with chain.BlockUpdate
			// TODO this can't work because StateProof is incompatible with []chain.ReceiptProof
			// cb(bu, sp)
		} else {
			fmt.Printf("Call BlockUpdate %+v\n", bu)
			// TODO this can't work because BlockUpdate is incompatible with chain.BlockUpdate
			// cb(bu, nil)
		}
		return nil
	})
}

func (r *Receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}
