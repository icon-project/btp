package p2p

import (
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/log"
)

type Handler interface {
	OnConn(c *Conn)
	OnPacket(pkt *Packet, c *Conn)
	OnError(err error, c *Conn, pkt *Packet)
	OnClose(c *Conn)
}

type handler struct {
	//log
	l log.Logger
}

func (h *handler) OnConn(c *Conn) {
	h.l.Traceln("onPeer", c)
}

func (h *handler) OnPacket(pkt *Packet, c *Conn) {
	h.l.Traceln("OnPacket", pkt, c)
}

func (h *handler) OnError(err error, c *Conn, pkt *Packet) {
	h.l.Traceln("OnError", err, c, pkt)
	c.CloseByError(err)
}

func (h *handler) OnClose(c *Conn) {
	h.l.Traceln("OnClose", c)
}

func (h *handler) encode(v interface{}) []byte {
	b := make([]byte, DefaultPacketBufferSize)
	enc := codec.MP.NewEncoderBytes(&b)
	if err := enc.Encode(v); err != nil {
		log.Panicf("fail to encode object v=%+v err=%+v", v, err)
	}
	return b
}

func (h *handler) decode(b []byte, v interface{}) {
	codec.MP.MustUnmarshalFromBytes(b, v)
}

func (h *handler) send(pi ProtocolInfo, m interface{}, c *Conn) {
	pkt := NewPacket(pi, h.encode(m))
	if err := c.Send(pkt); err != nil {
		h.l.Infoln("send", err)
		c.CloseByError(err)
	} else {
		h.l.Traceln("send", m, c)
	}
}

func (h *handler) response(pkt *Packet, m interface{}, c *Conn) {
	resp := pkt.Response(h.encode(m))
	if err := c.Send(resp); err != nil {
		h.l.Infoln("response", err)
		c.CloseByError(err)
	} else {
		h.l.Traceln("response", m, c)
	}
}

func newHandler(l log.Logger) *handler {
	return &handler{l: l}
}
