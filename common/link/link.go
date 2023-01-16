package link

import (
	"fmt"
	"math/rand"
	"sync"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/icon/client"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/types"
)

const (
	CodeBTP      errors.Code = 0
	CodeBMC      errors.Code = 10
	CodeBMV      errors.Code = 25
	CodeBSH      errors.Code = 40
	CodeReserved errors.Code = 55
)

const (
	BMVUnknown = CodeBMV + iota
	BMVNotVerifiable
	BMVAlreadyVerified
	BMVRevertInvalidBlockWitnessOld
)

type relayMessage struct {
	id      int
	height  int64
	seq     int64
	message []byte
	rmis    []RelayMessageItem
}

func (r *relayMessage) Id() int {
	return r.id
}

func (r *relayMessage) Bytes() []byte {
	return r.message
}

func (r *relayMessage) Size() int64 {
	return int64(len(r.message))
}

func (r *relayMessage) Height() int64 {
	return r.height
}

func (r *relayMessage) Sequence() int64 {
	return r.seq
}

func (r *relayMessage) RelayMessageItems() []RelayMessageItem {
	return r.rmis
}

type Link struct {
	r      Receiver
	s      types.Sender
	l      log.Logger
	mtx    sync.RWMutex
	src    types.BtpAddress
	dst    types.BtpAddress
	rmsMtx sync.RWMutex
	rms    []*relayMessage
	rss    []ReceiveStatus
	bs     *types.BMCLinkStatus
	ls     int64
	cfg    *chain.Config //TODO config refactoring

}

func NewLink(cfg *chain.Config, r Receiver, l log.Logger) types.Link {
	link := &Link{
		src: cfg.Src.Address,
		dst: cfg.Dst.Address,
		l:   l.WithFields(log.Fields{log.FieldKeyChain: fmt.Sprintf("%s", cfg.Dst.Address.NetworkID())}),
		cfg: cfg,
		r:   r,
		rms: make([]*relayMessage, 0),
		rss: make([]ReceiveStatus, 0),
	}
	return link
}

func (l *Link) Start(sender types.Sender) error {
	l.s = sender
	l.ls = int64(l.s.TxSizeLimit()) - l.r.GetMarginForLimit()
	scc, err := l.s.Start()
	if err != nil {
		return err
	}

	var once sync.Once
	for {
		select {
		case sc := <-scc:
			switch t := sc.(type) {
			case *types.RelayResult:
				l.result(t)
			case *types.BMCLinkStatus:
				go once.Do(func() {
					l.l.Debugf("Destination %s, height:%d",
						l.dst, t.Verifier.Height)
					l.receiverChannel(t)
				})
				l.bs = t
				l.sendRelayMessage(t)
			default:
				//TODO exception
			}
		}
	}

	return nil
}

func (l *Link) Stop() {
	l.s.Stop()
	l.r.Stop()
}

func (l *Link) receiverChannel(bs *types.BMCLinkStatus) error {
	rsc, err := l.r.Start(bs)

	if err != nil {
		return err
	}
	for {
		select {
		case rs := <-rsc:
			l.rss = append(l.rss, rs)
		}
	}
}

func (l *Link) sendRelayMessage(bs *types.BMCLinkStatus) error {
	rmis := make([]RelayMessageItem, 0)

	var limitOffset int64
	seq := bs.RxSeq
	var mp MessageProof

	//check block proof
	cs, err := l.checkSequence(bs, l.ls)
	if err != nil {
		return err
	}

	rmis = append(rmis, cs...)
	for _, s := range cs {
		limitOffset += s.Len()
	}

	mp, err = l.messageCheck(bs, l.ls-limitOffset)
	if err != nil {
		return err
	}
	limitOffset += mp.Len()

	bus, err := l.getHeader(bs, l.ls-limitOffset)
	if err != nil {
		return err
	}

	if len(bus) != 0 {
		for _, bu := range bus {
			rmis = append(rmis, bu)
			limitOffset += bu.Len()

			if int64(l.s.TxSizeLimit()) > limitOffset {
				bf, err := l.r.BuildBlockProof(bs, bu.TargetHeight())
				if err != nil {
					return err
				}
				if bf != nil {
					rmis = append(rmis, bf)
					limitOffset += bf.Len()
				}
			}
			if int64(l.s.TxSizeLimit()) > limitOffset {
				mp, err = l.r.BuildMessageProof(seq, l.ls-limitOffset)
				if err != nil {
					return err
				}
				if mp != nil {
					rmis = append(rmis, mp)
					limitOffset += mp.Len()
					seq = mp.LastSeqNum()
				}
			}
		}
	}
	if len(rmis) != 0 {
		var s int64
		if mp != nil {
			s = mp.LastSeqNum()
		} else {
			s = bs.RxSeq
		}
		rm, err := l.appendRelayMessage(bus[len(bus)-1].TargetHeight(), s, rmis)

		if err != nil {
			return err
		}
		l.rms = append(l.rms, rm)

		l.l.Debugf("rm info id:%d, height:%d, mpLen:%d", rm.Id(), bus[len(bus)-1].TargetHeight(), seq)
		l.s.Relay(rm)
	}

	return nil
}

func (l *Link) getHeader(bs *types.BMCLinkStatus, limit int64) ([]BlockUpdate, error) {
	for {
		bus, err := l.r.BuildBlockUpdate(bs, limit)
		if err != nil {
			return nil, err
		}
		if len(bus) != 0 {
			return bus, nil
		}
	}
}

func (l *Link) checkSequence(bs *types.BMCLinkStatus, limit int64) ([]RelayMessageItem, error) {
	rmis := make([]RelayMessageItem, 0)
	var limitOffset int64
	seq := bs.RxSeq

	for {
		h := l.r.GetHeightForSeq(seq)
		if h < bs.Verifier.Height {
			if int64(l.s.TxSizeLimit()) > limit {
				bf, err := l.r.BuildBlockProof(bs, h)
				if err != nil {
					return nil, err
				}
				if bf != nil {
					rmis = append(rmis, bf)
					limitOffset += bf.Len()
				}
			}
			if int64(l.s.TxSizeLimit()) > limitOffset {
				mp, err := l.r.BuildMessageProof(seq, l.ls-limitOffset)
				if err != nil {
					return nil, err
				}
				if mp != nil {
					rmis = append(rmis, mp)
					limitOffset += mp.Len()
					seq = mp.LastSeqNum()
				}
			}
		} else {
			break
		}

	}

	return rmis, nil
}

func (l *Link) messageCheck(bs *types.BMCLinkStatus, limit int64) (MessageProof, error) {
	rs := l.selectReceiveStatus(bs.Verifier.Height)
	if rs == nil {
		return nil, nil
	}
	vs, err := getVerifierStatus(bs.Verifier.Extra)
	if err != nil {
		return nil, err
	}

	index := (bs.RxSeq - vs.MessageCount) - vs.FirstMessageSn
	if index < rs.Seq() {
		mf, err := l.r.BuildMessageProof(bs.RxSeq, limit)
		if err != nil {
			return nil, err
		}
		return mf, nil
	}
	return nil, nil
}

func (l *Link) selectReceiveStatus(height int64) ReceiveStatus {
	for _, rs := range l.rss {
		if rs.Height() == height {
			return rs
		}
	}
	return nil
}

func (l *Link) appendRelayMessage(height, seq int64, rmis []RelayMessageItem) (*relayMessage, error) {
	m, err := l.r.BuildRelayMessage(rmis)
	if err != nil {
		return nil, err
	}

	rm := &relayMessage{
		id:      rand.Int(),
		height:  height,
		seq:     seq,
		message: m,
		rmis:    rmis,
	}

	return rm, nil
}

func (l *Link) removeRelayMessage(id int) {
	l.mtx.Lock()
	defer l.mtx.Unlock()

	index := 0
	for i, rm := range l.rms {
		if rm.id == id {
			index = i
		}
	}

	l.l.Debugf("remove relay message h:%d seq:%d", l.rms[index].Height(), l.rms[index].Sequence())
	l.rms = l.rms[index:]

}

func (l *Link) updateBlockProof(id int) error {
	rm := l.searchRelayMessage(id)
	bp, err := l.r.BuildBlockProof(l.bs, rm.Height())
	if err != nil {
		return err
	}

	limitSize := int64(l.s.TxSizeLimit()) - l.r.GetMarginForLimit()
	if rm.Size()+bp.Len() < limitSize {
		rmis := rm.RelayMessageItems()
		rmis = append(rmis, bp)
		m, err := l.appendRelayMessage(rm.Height(), rm.Sequence(), rmis)
		if err != nil {
			return err
		}
		rm = m
		l.s.Relay(rm)
	} else {
		rmis := make([]RelayMessageItem, 0)
		m, err := l.appendRelayMessage(rm.Height(), rm.Sequence(), rmis)
		if err != nil {
			return err
		}
		l.rms = append(l.rms, m)
		l.s.Relay(m)
	}
	return nil
}

func (l *Link) searchRelayMessage(id int) *relayMessage {
	for _, rm := range l.rms {
		if rm.Id() == id {
			return rm
		}
	}
	return nil
}

func (l *Link) result(rr *types.RelayResult) {
	switch rr.Err {
	case BMVUnknown:
		l.l.Panicf("BMVUnknown Revert : ErrorCoder:%+v", rr.Err)
	case BMVNotVerifiable:
		l.sendRelayMessage(l.bs)
	case BMVAlreadyVerified:
		l.removeRelayMessage(rr.Id)
	case BMVRevertInvalidBlockWitnessOld:
		l.updateBlockProof(rr.Id)
	default:
		l.l.Panicf("fail to GetResult RelayMessage ID:%v ErrorCoder:%+v",
			rr.Id, rr.Err)
	}

}

func getVerifierStatus(extra []byte) (*client.VerifierStatus, error) {
	vs := &client.VerifierStatus{}
	_, err := codec.RLP.UnmarshalFromBytes(extra, vs)
	if err != nil {
		return nil, err
	}
	return vs, nil
}
