package pra

import (
	"encoding/json"

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
	return r
}

func (r *Receiver) newBlockUpdate(v *BlockNotification) (*chain.BlockUpdate, error) {
	return nil, nil
}

func (r *Receiver) newReceiptProofs(v *BlockNotification) ([]*chain.ReceiptProof, error) {
	return nil, nil
}

func (r *Receiver) ReceiveLoop(height int64, seq int64, cb chain.ReceiveCallback, scb func()) error {
	if seq < 1 {
		r.isFoundOffsetBySeq = true
	}

	return r.c.MonitorSubstrateBlock(uint64(height), func(v *BlockNotification) error {
		var err error
		var bu *chain.BlockUpdate
		var rps []*chain.ReceiptProof
		if bu, err = r.newBlockUpdate(v); err != nil {
			return err
		}
		if rps, err = r.newReceiptProofs(v); err != nil {
			return err
		} else if r.isFoundOffsetBySeq {
			cb(bu, rps)
		} else {
			cb(bu, nil)
		}
		return nil
	})
}

func (r *Receiver) StopReceiveLoop() {
	r.c.CloseAllMonitor()
}
