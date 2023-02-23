package bridge

import (
	"encoding/base64"
	"fmt"
	"unsafe"

	"github.com/gorilla/websocket"

	"github.com/icon-project/btp/chain/icon/client"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/intconv"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/types"
)

type receiveStatus struct {
	height int64
	seq    int64
	rp     *ReceiptProof
}

func (r *receiveStatus) Height() int64 {
	return r.height
}

func (r *receiveStatus) Seq() int64 {
	return r.seq
}

func (r *receiveStatus) ReceiptProof() *ReceiptProof {
	return r.rp
}

type bridge struct {
	l           log.Logger
	src         types.BtpAddress
	dst         types.BtpAddress
	c           *client.Client
	nid         int64
	rsc         chan link.ReceiveStatus
	rss         []*receiveStatus
	startHeight int64
}

func newReceiveStatus(height, rxSeq int64, sn int64, msgs []string, next types.BtpAddress) (*receiveStatus, error) {
	evts := make([]*Event, 0)
	seq := sn
	for _, msg := range msgs {
		if sn > rxSeq {
			evt, err := messageToEvent(next, msg, sn)
			if err != nil {
				return nil, err
			}
			evts = append(evts, evt)
		}
		sn++
	}

	rp := &ReceiptProof{
		Index:  0,
		Events: evts,
		Height: height,
	}

	return &receiveStatus{
		height: height,
		seq:    seq,
		rp:     rp,
	}, nil
}

func NewBridge(src, dst types.BtpAddress, endpoint string, l log.Logger) *bridge {
	c := &bridge{
		src: src,
		dst: dst,
		l:   l,
		rsc: make(chan link.ReceiveStatus),
		rss: make([]*receiveStatus, 0),
	}
	c.c = client.NewClient(endpoint, l)
	return c
}

func (b *bridge) getNetworkId() error {
	if b.nid == 0 {
		nid, err := b.c.GetBTPLinkNetworkId(b.src, b.dst)
		if err != nil {
			return err
		}
		b.nid = nid
	}

	return nil
}

func (b *bridge) Start(bs *types.BMCLinkStatus) (<-chan link.ReceiveStatus, error) {
	if err := b.getNetworkId(); err != nil {
		return nil, err
	}

	if err := b.setStartHeight(); err != nil {
		return nil, err
	}

	go func() {
		b.Monitoring(bs)
	}()

	return b.rsc, nil
}

func (b *bridge) Stop() {
	close(b.rsc)
}

func (b *bridge) GetStatus() (link.ReceiveStatus, error) {
	return b.rss[len(b.rss)-1], nil
}

func (b *bridge) GetHeightForSeq(seq int64) int64 {
	rs := b.GetReceiveHeightForSequence(seq)
	if rs != nil {
		return b.GetReceiveHeightForSequence(seq).height
	} else {
		return 0
	}
}

func (b *bridge) BuildBlockUpdate(bls *types.BMCLinkStatus, limit int64) ([]link.BlockUpdate, error) {
	b.updateReceiveStatus(bls)
	bus := make([]link.BlockUpdate, 0)
	for _, rs := range b.rss {
		bu := NewBlockUpdate(bls, rs.Height())
		bus = append(bus, bu)
	}
	return bus, nil
}

func (b *bridge) BuildBlockProof(bls *types.BMCLinkStatus, height int64) (link.BlockProof, error) {
	return nil, nil
}

func (b *bridge) BuildMessageProof(bls *types.BMCLinkStatus, limit int64) (link.MessageProof, error) {
	var rmSize int
	rs := b.GetReceiveHeightForSequence(bls.RxSeq + 1)
	if rs == nil {
		return nil, nil
	}
	//offset := int64(rs.seq - bls.RxSeq)
	messageCnt := len(rs.ReceiptProof().Events)
	offset := bls.RxSeq - (rs.Seq() - int64(messageCnt))
	trp := &ReceiptProof{
		Index:  rs.ReceiptProof().Index,
		Events: make([]*Event, 0),
		Height: rs.Height(),
	}

	for i := offset; i < int64(messageCnt); i++ {
		size := sizeOfEvent(rs.ReceiptProof().Events[i])
		if limit < int64(rmSize+size) {
			return NewMessageProof(bls, bls.RxSeq+i, trp)
		}
		trp.Events = append(trp.Events, rs.ReceiptProof().Events[i])
		rmSize += size
	}

	//last event
	return NewMessageProof(bls, bls.RxSeq+int64(messageCnt), trp)

}

func (b *bridge) BuildRelayMessage(rmis []link.RelayMessageItem) ([]byte, error) {
	//delete blockUpdate and only mp append
	for _, rmi := range rmis {
		if rmi.Type() == link.TypeMessageProof {
			mp := rmi.(*MessageProof)
			b.l.Debugf("BuildRelayMessage height:%d data:%s ", mp.nextBls.Verifier.Height,
				base64.URLEncoding.EncodeToString(mp.Bytes()))

			return mp.Bytes(), nil
		}
	}
	return nil, nil
}

//TODO Refactoring reduplication func
func (b *bridge) getBtpMessage(height int64) ([]string, error) {
	pr := &client.BTPBlockParam{Height: client.HexInt(intconv.FormatInt(height)), NetworkId: client.HexInt(intconv.FormatInt(b.nid))}
	mgs, err := b.c.GetBTPMessage(pr)
	if err != nil {
		return nil, err
	}

	return mgs, nil
}

func (b *bridge) Monitoring(bs *types.BMCLinkStatus) error {
	if bs.Verifier.Height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}

	req := &client.BTPRequest{
		Height:    client.NewHexInt(bs.Verifier.Height),
		NetworkID: client.NewHexInt(b.nid),
		ProofFlag: client.NewHexInt(0),
	}

	onErr := func(conn *websocket.Conn, err error) {
		b.l.Debugf("onError %s err:%+v", conn.LocalAddr().String(), err)
		_ = conn.Close()
	}
	onConn := func(conn *websocket.Conn) {
		b.l.Debugf("ReceiveLoop monitorBTP2Block height:%d seq:%d networkId:%d connected %s",
			bs.Verifier.Height, bs.TxSeq, b.nid, conn.LocalAddr().String())
	}

	err := b.monitorBTP2Block(req, bs, onConn, onErr)
	if err != nil {
		return err
	}
	return nil
}

func (b *bridge) monitorBTP2Block(req *client.BTPRequest, bs *types.BMCLinkStatus, scb func(conn *websocket.Conn), errCb func(*websocket.Conn, error)) error {
	offset, err := b.c.GetBTPLinkOffset(b.src, b.dst)
	if err != nil {
		return err
	}
	//BMC.seq starts with 1 and BTPBlock.FirstMessageSN starts with 0
	offset += 1

	return b.c.MonitorBTP(req, func(conn *websocket.Conn, v *client.BTPNotification) error {
		h, err := base64.StdEncoding.DecodeString(v.Header)
		if err != nil {
			return err
		}

		bh := &client.BTPBlockHeader{}
		if _, err = codec.RLP.UnmarshalFromBytes(h, bh); err != nil {
			return err
		}
		if bh.MainHeight != b.startHeight {
			msgs, err := b.getBtpMessage(bh.MainHeight)
			if err != nil {
				return err
			}
			sn := offset + bh.UpdateNumber>>1

			rs, err := newReceiveStatus(bh.MainHeight, bs.RxSeq, sn, msgs, b.dst)
			if err != nil {
				return err
			}
			b.rss = append(b.rss, rs)
			b.l.Debugf("monitor info : Height:%d  UpdateNumber:%d  MessageCnt:%d ", bh.MainHeight, bh.UpdateNumber, len(msgs))

			b.rsc <- rs
		}
		return nil
	}, scb, errCb)
}

func (b *bridge) updateReceiveStatus(bs *types.BMCLinkStatus) {
	for i, rs := range b.rss {
		if rs.Height() <= bs.Verifier.Height && rs.Seq() <= bs.RxSeq {
			b.rss = b.rss[i+1:]
			return
		}
	}
}

func (b *bridge) GetReceiveHeightForSequence(seq int64) *receiveStatus {
	for _, rs := range b.rss {
		if seq <= rs.Seq() && seq >= rs.Seq() {
			return rs
		}
	}
	return nil
}

func (b *bridge) setStartHeight() error {
	p := &client.BTPNetworkInfoParam{Id: client.HexInt(intconv.FormatInt(b.nid))}
	ni, err := b.c.GetBTPNetworkInfo(p)
	if err != nil {
		return err
	}
	sh, err := ni.StartHeight.Value()
	b.startHeight = sh + 1
	return nil
}

func sizeOfEvent(rp *Event) int {
	return int(unsafe.Sizeof(rp))
}

func messageToEvent(next types.BtpAddress, msg string, seq int64) (*Event, error) {
	b, err := base64.StdEncoding.DecodeString(msg)
	if err != nil {
		return nil, err
	}
	evt := &Event{
		Next:     next.String(),
		Sequence: seq,
		Message:  b,
	}
	return evt, nil
}
