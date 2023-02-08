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
	l   log.Logger
	src types.BtpAddress
	dst types.BtpAddress
	c   *client.Client
	nid int64
	rsc chan link.ReceiveStatus
	rss []*receiveStatus
}

func newReceiveStatus(height, seq int64, offset int64, msgs []string, next types.BtpAddress) (*receiveStatus, error) {
	evts := make([]*Event, 0)
	sn := offset + seq>>1
	for _, msg := range msgs {
		if sn > seq {
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
	err := b.Monitoring(bs)
	if err != nil {
		return nil, err
	}
	return b.rsc, nil
}

func (b *bridge) Stop() {
	close(b.rsc)
}

func (b *bridge) GetStatus() (link.ReceiveStatus, error) {
	return b.rss[len(b.rss)-1], nil
}

func (b *bridge) GetHeightForSeq(seq int64) int64 {
	return b.GetReceiveHeightForSequence(seq).height
}

func (b *bridge) BuildBlockUpdate(bs *types.BMCLinkStatus, limit int64) ([]link.BlockUpdate, error) {
	return nil, nil
}

func (b *bridge) BuildBlockProof(bs *types.BMCLinkStatus, height int64) (link.BlockProof, error) {
	return nil, nil
}

func (b *bridge) BuildMessageProof(bs *types.BMCLinkStatus, limit int64) (link.MessageProof, error) {
	var offset int
	var rmSize int
	rs := b.GetReceiveHeightForSequence(bs.RxSeq)
	if rs == nil {
		return nil, nil
	}
	offset = int(rs.seq - bs.RxSeq)
	trp := &ReceiptProof{
		Index:  rs.ReceiptProof().Index,
		Events: make([]*Event, 0),
		Height: rs.Height(),
	}
	eventLen := len(rs.ReceiptProof().Events)
	for i := offset; i < eventLen; i++ {
		size := sizeOfEvent(rs.ReceiptProof().Events[i])
		if limit < int64(rmSize+size) {
			return NewMessageProof(bs.RxSeq, bs.RxSeq+int64(i), i, trp), nil
		}
		trp.Events = append(trp.Events, rs.ReceiptProof().Events[i])
		rmSize += size
	}

	//last event
	return NewMessageProof(bs.RxSeq, bs.RxSeq+int64(eventLen), eventLen, trp), nil

}

func (b *bridge) BuildRelayMessage(rmis []link.RelayMessageItem) ([]byte, error) {
	rm := &BridgeRelayMessage{
		Receipts: make([][]byte, 0),
	}
	var (
		rb  []byte
		err error
	)

	for _, rmi := range rmis {
		r := &Receipt{
			Index:  rmi.(*relayMessageItem).ReceiptProof().Index,
			Events: rmi.(*relayMessageItem).Bytes(),
			Height: rmi.(*relayMessageItem).ReceiptProof().Height,
		}
		if rb, err = codec.RLP.MarshalToBytes(r); err != nil {
			return nil, err
		}
		rm.Receipts = append(rm.Receipts, rb)
	}
	if rb, err = codec.RLP.MarshalToBytes(rm); err != nil {
		return nil, err
	}
	return rb, nil
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

	err := b.monitorBTP2Block(req, onConn, onErr)
	if err != nil {
		return err
	}
	return nil
}

func (b *bridge) monitorBTP2Block(req *client.BTPRequest, scb func(conn *websocket.Conn), errCb func(*websocket.Conn, error)) error {
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
		msgs, err := b.getBtpMessage(bh.MainHeight)
		if err != nil {
			return err
		}

		rs, err := newReceiveStatus(bh.MainHeight, bh.UpdateNumber, offset, msgs, b.dst)
		if err != nil {
			return err
		}

		b.rss = append(b.rss, rs)
		b.rsc <- rs
		return nil
	}, scb, errCb)
}

func (b *bridge) updateReceiveStatus(bs *types.BMCLinkStatus) {
	for i, rs := range b.rss {
		//TODO refactoring
		if rs.Height() == bs.Verifier.Height && rs.Seq() == bs.RxSeq {
			b.rss = b.rss[:i]
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
