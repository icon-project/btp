package btp2

import (
	"encoding/base64"
	"fmt"

	"github.com/gorilla/websocket"

	"github.com/icon-project/btp/chain/icon/client"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/intconv"
	"github.com/icon-project/btp/common/link"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mbt"
	"github.com/icon-project/btp/common/types"
)

type receiveStatus struct {
	height int64
	seq    int64
	mt     *mbt.MerkleBinaryTree
}

func (r *receiveStatus) Height() int64 {
	return r.height
}

func (r *receiveStatus) Seq() int64 {
	return r.seq
}

func (r *receiveStatus) MerkleBinaryTree() *mbt.MerkleBinaryTree {
	return r.mt
}

func newReceiveStatus(height, seq int64, msgs []string) (*receiveStatus, error) {
	result := make([][]byte, 0)
	for _, mg := range msgs {
		m, err := base64.StdEncoding.DecodeString(mg)
		if err != nil {
			return nil, err
		}
		result = append(result, m)
	}

	mt, err := mbt.NewMerkleBinaryTree(mbt.HashFuncByUID("eth"), result)
	if err != nil {
		return nil, err
	}

	return &receiveStatus{
		height: height,
		seq:    seq,
		mt:     mt,
	}, nil

}

type btp2 struct {
	l           log.Logger
	src         types.BtpAddress
	dst         types.BtpAddress
	c           *client.Client
	nid         int64
	rsc         chan link.ReceiveStatus
	rss         []*receiveStatus
	seq         int64
	startHeight int64
}

func NewBTP2(src, dst types.BtpAddress, endpoint string, l log.Logger) *btp2 {
	c := &btp2{
		src: src,
		dst: dst,
		l:   l,
		rsc: make(chan link.ReceiveStatus),
		rss: make([]*receiveStatus, 0),
	}
	c.c = client.NewClient(endpoint, l)
	return c
}

func (b *btp2) getNetworkId() error {
	if b.nid == 0 {
		nid, err := b.c.GetBTPLinkNetworkId(b.src, b.dst)
		if err != nil {
			return err
		}
		b.nid = nid
	}

	return nil
}
func (b *btp2) getBtpMessage(height int64) ([]string, error) {
	pr := &client.BTPBlockParam{Height: client.HexInt(intconv.FormatInt(height)), NetworkId: client.HexInt(intconv.FormatInt(b.nid))}
	mgs, err := b.c.GetBTPMessage(pr)
	if err != nil {
		return nil, err
	}

	return mgs, nil
}

func (b *btp2) getBtpHeader(height int64) ([]byte, []byte, error) {
	pr := &client.BTPBlockParam{Height: client.HexInt(intconv.FormatInt(height)), NetworkId: client.HexInt(intconv.FormatInt(b.nid))}
	hB64, err := b.c.GetBTPHeader(pr)
	if err != nil {
		return nil, nil, err
	}

	h, err := base64.StdEncoding.DecodeString(hB64)
	if err != nil {
		return nil, nil, err
	}

	pB64, err := b.c.GetBTPProof(pr)
	if err != nil {
		return nil, nil, err
	}
	p, err := base64.StdEncoding.DecodeString(pB64)
	if err != nil {
		return nil, nil, err
	}

	return h, p, nil
}

func (b *btp2) Start(bs *types.BMCLinkStatus) (<-chan link.ReceiveStatus, error) {
	if err := b.getNetworkId(); err != nil {
		return nil, err
	}

	if err := b.setStartHeight(); err != nil {
		return nil, err
	}

	go func() {
		b.Monitoring(bs)
	}()

	//b.Monitoring(bs)

	return b.rsc, nil
}

func (b *btp2) Stop() {
	close(b.rsc)
}

func (b *btp2) GetStatus() (link.ReceiveStatus, error) {
	return b.rss[len(b.rss)-1], nil
}

func (b *btp2) GetHeightForSeq(seq int64) int64 {
	rs := b.GetReceiveHeightForSequence(seq)
	if rs != nil {
		return b.GetReceiveHeightForSequence(seq).height
	} else {
		return 0
	}

}

func (b *btp2) BuildBlockUpdate(bs *types.BMCLinkStatus, limit int64) ([]link.BlockUpdate, error) {
	b.updateReceiveStatus(bs)
	bus := make([]link.BlockUpdate, 0)
	for _, rs := range b.rss {

		h, p, err := b.getBtpHeader(rs.Height())
		if err != nil {
			return nil, err
		}
		bh := &client.BTPBlockHeader{}
		if _, err := codec.RLP.UnmarshalFromBytes(h, bh); err != nil {
			return nil, err
		}
		bbu := &client.BTPBlockUpdate{BTPBlockHeader: h, BTPBlockProof: p}

		if limit < int64(len(codec.RLP.MustMarshalToBytes(bbu))) {
			return bus, nil
		}

		bu := NewBlockUpdate(bs, bh.MainHeight, bbu)
		bus = append(bus, bu)

	}
	return bus, nil
}

func (b *btp2) BuildBlockProof(bs *types.BMCLinkStatus, height int64) (link.BlockProof, error) {
	return nil, nil
}

func (b *btp2) BuildMessageProof(bs *types.BMCLinkStatus, limit int64) (link.MessageProof, error) {
	rs := b.GetReceiveHeightForHeight(bs.Verifier.Height)

	if rs == nil {
		return nil, nil
	}
	messageCnt := int64(rs.MerkleBinaryTree().Len())
	offset := bs.RxSeq - (rs.Seq() - messageCnt)
	if (bs.RxSeq - rs.seq) == 0 {
		return nil, nil
	}
	if messageCnt > 0 {
		for i := offset + 1; i < messageCnt; i++ {
			p, err := rs.MerkleBinaryTree().Proof(int(offset+1), int(i))
			if err != nil {
				return nil, err
			}

			if limit < int64(len(codec.RLP.MustMarshalToBytes(p))) {
				mp := NewMessageProof(bs, bs.RxSeq+i, *p)
				return mp, nil
			}
		}
	}

	p, err := rs.MerkleBinaryTree().Proof(int(offset+1), int(messageCnt))
	if err != nil {
		return nil, err
	}
	mp := NewMessageProof(bs, bs.RxSeq+messageCnt, *p)
	return mp, nil
}

func (b *btp2) BuildRelayMessage(rmis []link.RelayMessageItem) ([]byte, error) {
	bm := &BTPRelayMessage{
		Messages: make([]*TypePrefixedMessage, 0),
	}

	for _, rmi := range rmis {
		tpm, err := NewTypePrefixedMessage(rmi)
		if err != nil {
			return nil, err
		}
		bm.Messages = append(bm.Messages, tpm)
	}

	rb, err := codec.RLP.MarshalToBytes(bm)
	if err != nil {
		return nil, err
	}

	return rb, nil
}

func (b *btp2) updateReceiveStatus(bs *types.BMCLinkStatus) {
	for i, rs := range b.rss {
		if rs.Height() <= bs.Verifier.Height && rs.Seq() <= bs.RxSeq {
			b.rss = b.rss[i+1:]
			return
		}
	}
}

func (b *btp2) Monitoring(bs *types.BMCLinkStatus) error {
	if bs.Verifier.Height < 1 {
		return fmt.Errorf("cannot catchup from zero height")
	}

	req := &client.BTPRequest{
		Height:    client.HexInt(intconv.FormatInt(bs.Verifier.Height + 1)),
		NetworkID: client.HexInt(intconv.FormatInt(b.nid)),
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

func (b *btp2) monitorBTP2Block(req *client.BTPRequest, bs *types.BMCLinkStatus, scb func(conn *websocket.Conn), errCb func(*websocket.Conn, error)) error {
	if bs.RxSeq != 0 {
		b.seq = bs.RxSeq
	}

	return b.c.MonitorBTP(req, func(conn *websocket.Conn, v *client.BTPNotification) error {
		h, err := v.Header.Value()
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

			b.seq += int64(len(msgs))
			rs, err := newReceiveStatus(bh.MainHeight, b.seq, msgs)
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

func (b *btp2) GetReceiveHeightForSequence(seq int64) *receiveStatus {
	for _, rs := range b.rss {
		if seq <= rs.Seq() && seq >= rs.Seq() {
			return rs
		}
	}
	return nil
}

func (b *btp2) GetReceiveHeightForHeight(height int64) *receiveStatus {
	for _, rs := range b.rss {
		if rs.Height() == height {
			return rs
		}
	}
	return nil
}

func (b *btp2) setStartHeight() error {
	p := &client.BTPNetworkInfoParam{Id: client.HexInt(intconv.FormatInt(b.nid))}
	ni, err := b.c.GetBTPNetworkInfo(p)
	if err != nil {
		return err
	}
	sh, err := ni.StartHeight.Value()
	b.startHeight = sh + 1
	return nil
}
