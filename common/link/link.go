package link

import (
	"fmt"
	"math/rand"
	"sync"

	"github.com/icon-project/btp/chain"
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
	id       int
	bls      *types.BMCLinkStatus
	bpHeight int64
	message  []byte
	rmis     []RelayMessageItem
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

func (r *relayMessage) BMCLinkStatus() *types.BMCLinkStatus {
	return r.bls
}

func (r *relayMessage) BpHeight() int64 {
	return r.bpHeight
}

func (r *relayMessage) RelayMessageItems() []RelayMessageItem {
	return r.rmis
}

type relayMessageItem struct {
	rmis [][]RelayMessageItem
	size int64
}
type Link struct {
	r         Receiver
	s         types.Sender
	l         log.Logger
	mtx       sync.RWMutex
	src       types.BtpAddress
	dst       types.BtpAddress
	rmsMtx    sync.RWMutex
	rms       []*relayMessage
	rss       []ReceiveStatus
	rmi       *relayMessageItem
	limitSize int64
	cfg       *chain.Config //TODO config refactoring

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
		rmi: &relayMessageItem{
			rmis: make([][]RelayMessageItem, 0),
			size: 0,
		},
	}
	link.rmi.rmis = append(link.rmi.rmis, make([]RelayMessageItem, 0))
	return link
}

func (l *Link) Start(sender types.Sender) error {
	l.s = sender
	errCh := make(chan error)
	l.senderChannel(errCh)

	bls, err := l.s.GetStatus()
	if err != nil {
		return err
	}

	l.receiverChannel(bls, errCh)

	for {
		select {
		case err := <-errCh:
			if err != nil {
				return err
			}
		}
	}
	return nil
}

func (l *Link) Stop() {
	l.s.Stop()
	l.r.Stop()
}

func (l *Link) receiverChannel(bls *types.BMCLinkStatus, errCh chan error) {

	go func() {
		rsc, err := l.r.Start(bls)
		err = l.checkStatus(bls)

		for {
			select {
			case rs := <-rsc:
				l.rss = append(l.rss, rs)
				l.BuildRelayMessage(bls)
			}
		}

		select {
		case errCh <- err:
		default:
		}
	}()
}

func (l *Link) senderChannel(errCh chan error) {
	go func() {
		l.limitSize = int64(l.s.TxSizeLimit()) - l.s.GetMarginForLimit()
		scc, err := l.s.Start()

		for {
			select {
			case sc := <-scc:
				switch t := sc.(type) {
				case *types.RelayResult:
					err := l.result(t)
					errCh <- err
				case *types.BMCLinkStatus:
					l.clearRelayMessage(t)
					l.clearReceiveStatus(t)
				default:
					//TODO exception
				}
			}
		}

		select {
		case errCh <- err:
		default:
		}
	}()

}

func (l *Link) clearRelayMessage(bls *types.BMCLinkStatus) {
	for i, rm := range l.rms {
		if rm.bls.Verifier.Height <= bls.Verifier.Height && rm.bls.RxSeq <= bls.RxSeq {
			l.rms = l.rms[i+1:]
			break
		}
	}
}

func (l *Link) clearReceiveStatus(bls *types.BMCLinkStatus) {
	for i, rs := range l.rss {
		if rs.Height() <= bls.Verifier.Height && rs.Seq() <= bls.RxSeq {
			l.rss = l.rss[i+1:]
			break
		}
	}
}

func (l *Link) BuildRelayMessage(bls *types.BMCLinkStatus) error {
	if len(l.rmi.rmis) == 0 {
		l.createRelayMessageItem()
	}

	//Get Block
	bus, err := l.getHeader(bls)
	if err != nil {
		return err
	}

	if len(bus) != 0 {
		for _, bu := range bus {
			l.rmi.rmis[len(l.rmi.rmis)-1] = append(l.rmi.rmis[len(l.rmi.rmis)-1], bu)
			l.rmi.size += bu.Len()
			err := bu.UpdateBMCLinkStatus(bls)
			if err != nil {
				return err
			}

			if err = l.buildProof(bls, bu); err != nil {
				return err
			}

			if err = l.sendRelayMessage(bls); err != nil {
				return err
			}
		}
	}

	if l.isOverLimit(l.rmi.size) {
		l.sendRelayMessage(bls)
	}

	return nil
}

func (l *Link) sendRelayMessage(bls *types.BMCLinkStatus) error {
	rms, err := l.appendRelayMessage(bls)
	if err != nil {
		return err
	}

	for _, rm := range rms {
		l.rms = append(l.rms, rm)
		l.s.Relay(rm)
	}
	return nil
}

func (l *Link) getHeader(bs *types.BMCLinkStatus) ([]BlockUpdate, error) {
	for {
		bus, err := l.r.BuildBlockUpdate(bs, l.limitSize-l.rmi.size)
		if err != nil {
			return nil, err
		}
		if len(bus) != 0 {
			return bus, nil
		}
	}
}

func (l *Link) checkStatus(bls *types.BMCLinkStatus) error {
	lastSeq := bls.RxSeq
	for {
		h := l.r.GetHeightForSeq(lastSeq)
		if h == 0 {
			break
		}
		if h == bls.Verifier.Height {
			mp, err := l.r.BuildMessageProof(bls, l.limitSize-l.rmi.size)
			if err != nil {
				return err
			}

			if mp.Len() != 0 || bls.RxSeq < mp.LastSeqNum() {
				l.rmi.rmis[len(l.rmi.rmis)-1] = append(l.rmi.rmis[len(l.rmi.rmis)-1], mp)
				l.rmi.size += mp.Len()
			}
			break
		} else if h < bls.Verifier.Height {
			err := l.buildProof(bls, nil)
			if err != nil {
				return err
			}
		} else {
			break
		}
	}
	if l.rmi.size > 0 {
		l.sendRelayMessage(bls)
	}
	return nil
}

func (l *Link) buildProof(bls *types.BMCLinkStatus, bu BlockUpdate) error {
	rs := l.getReceiveStatusForHeight(bls)
	if rs == nil {
		return nil
	}
	for {
		if rs.Seq() <= bls.RxSeq {
			break
		}
		if l.isOverLimit(l.rmi.size) {
			l.sendRelayMessage(bls)
			if bu.ProofHeight() != 0 {
				mh := l.r.GetHeightForSeq(bls.RxSeq)
				if err := l.buildBlockProof(bls, mh); err != nil {
					return err
				}
			}
			l.buildMessageProof(bls)

		} else {
			if bu.ProofHeight() == -1 {
				mh := l.r.GetHeightForSeq(bls.RxSeq)
				if err := l.buildBlockProof(bls, mh); err != nil {
					return err
				}
			}
			l.buildMessageProof(bls)

		}
	}
	return nil
}

func (l *Link) getReceiveStatusForHeight(bls *types.BMCLinkStatus) ReceiveStatus {
	for _, rs := range l.rss {
		if rs.Height() == bls.Verifier.Height {
			return rs
		}
	}
	return nil
}

func (l *Link) buildMessageProof(bls *types.BMCLinkStatus) error {
	mp, err := l.r.BuildMessageProof(bls, l.limitSize-l.rmi.size)
	if err != nil {
		return err
	}
	if mp != nil {
		l.rmi.rmis[len(l.rmi.rmis)-1] = append(l.rmi.rmis[len(l.rmi.rmis)-1], mp)
		l.rmi.size += mp.Len()
		if err := mp.UpdateBMCLinkStatus(bls); err != nil {
			return err
		}
	}
	return nil
}

func (l *Link) buildBlockProof(bls *types.BMCLinkStatus, height int64) error {
	bf, err := l.r.BuildBlockProof(bls, height)
	if err != nil {
		return err
	}

	if bf != nil {
		l.rmi.rmis[len(l.rmi.rmis)-1] = append(l.rmi.rmis[len(l.rmi.rmis)-1], bf)
		l.rmi.size += bf.Len()
		if err := bf.UpdateBMCLinkStatus(bls); err != nil {
			return err
		}

	}
	return nil
}

func (l *Link) appendRelayMessage(bls *types.BMCLinkStatus) ([]*relayMessage, error) {
	rms := make([]*relayMessage, 0)
	for _, rmi := range l.rmi.rmis {
		m, err := l.r.BuildRelayMessage(rmi)
		if err != nil {
			return nil, err
		}

		rm := &relayMessage{
			id:       rand.Int(),
			bls:      bls,
			bpHeight: l.r.GetHeightForSeq(bls.RxSeq),
			message:  m,
			rmis:     rmi,
		}
		rms = append(rms, rm)
	}
	l.rmi.rmis = l.rmi.rmis[:0]
	l.rmi.size = 0

	return rms, nil
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

	l.l.Debugf("remove relay message h:%d seq:%d", l.rms[index].BMCLinkStatus().Verifier.Height,
		l.rms[index].BMCLinkStatus().RxSeq)
	l.rms = l.rms[index:]

}

func (l *Link) updateBlockProof(id int) error {
	rm := l.searchRelayMessage(id)
	l.buildProof(rm.bls, nil)
	l.sendRelayMessage(rm.bls)
	return nil
}

func (l *Link) searchReceiveStatusForHeight(height int64) ReceiveStatus {
	for _, rs := range l.rss {
		if rs.Height() == height {
			return rs
		}
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

func (l *Link) isOverLimit(size int64) bool {
	if int64(l.s.TxSizeLimit()) < size {
		l.createRelayMessageItem()
		return true
	}
	return false
}

func (l *Link) createRelayMessageItem() {
	l.rmi.rmis = append(l.rmi.rmis, make([]RelayMessageItem, 0))
	l.rmi.size = 0
}

func (l *Link) result(rr *types.RelayResult) error {
	l.rmsMtx.Lock()
	defer l.rmsMtx.Lock()

	switch rr.Err {
	case BMVUnknown:
		l.l.Panicf("BMVUnknown Revert : ErrorCoder:%+v", rr.Err)
	case BMVNotVerifiable:
		bls, err := l.s.GetStatus()
		if err != nil {
			return err
		}
		l.BuildRelayMessage(bls)
	case BMVAlreadyVerified:
		l.removeRelayMessage(rr.Id)
	case BMVRevertInvalidBlockWitnessOld:
		l.updateBlockProof(rr.Id)
	default:
		l.l.Panicf("fail to GetResult RelayMessage ID:%v ErrorCoder:%+v",
			rr.Id, rr.Err)
	}
	return nil
}
