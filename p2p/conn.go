package p2p

import (
	"fmt"
	"net"
	"sync"
	"sync/atomic"
	"time"

	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/log"
)

const (
	DefaultSendTimeout = 5 * time.Second
)

type Conn struct {
	channel string
	pubKey  *crypto.PublicKey

	incoming  bool
	secureKey *secureKey

	conn    net.Conn
	reader  *PacketReader
	writer  *PacketWriter
	handler Handler
	hMtx    sync.RWMutex
	once    sync.Once
	//
	close        chan error
	closed       int32
	closeReason  []string
	closeErr     []error
	closeInfoMtx sync.RWMutex

	//
	sn    map[ProtocolInfo]int16
	snMtx sync.Mutex
	waits map[uint32]chan *Packet
	wMtx  sync.Mutex

	l log.Logger
}

func (c *Conn) Channel() string {
	return c.channel
}

func (c *Conn) PublicKey() *crypto.PublicKey {
	return c.pubKey
}

func (c *Conn) Incoming() bool {
	return c.incoming
}

func (c *Conn) _recover() interface{} {
	if err := recover(); err != nil {
		c.CloseByError(fmt.Errorf("_recover from %+v", err))
		return err
	}
	return nil
}

func (c *Conn) _close() {
	if cerr := c.conn.Close(); cerr == nil {
		atomic.StoreInt32(&c.closed, 1)
		close(c.close)
		if h := c.getHandler(); h != nil {
			h.OnClose(c)
		}
	}
}

func (c *Conn) Close(reason string) {
	c.closeInfoMtx.Lock()
	c.closeReason = append(c.closeReason, reason)
	c.closeInfoMtx.Unlock()

	c._close()
}

func (c *Conn) CloseByError(err error) {
	c.closeInfoMtx.Lock()
	c.closeErr = append(c.closeErr, err)
	c.closeInfoMtx.Unlock()

	c._close()
}

func (c *Conn) receiveRoutine() {
	defer func() {
		if err := c._recover(); err == nil {
			c.Close("receiveRoutine finish")
		}
		for _, wc := range c.waits {
			close(wc)
		}
	}()
	for {
		h := c.getHandler()
		p, err := c.reader.ReadPacket()
		c.l.Traceln("ReadPacket", p, err)
		if err != nil {
			isTmpError := IsTemporaryError(err)
			c.l.Debugf("Conn.receiveRoutine Error isTemporary:{%v} error:{%+v} Conn:%s",
				isTmpError, err, c.String())
			if h != nil && isTmpError {
				h.OnError(err, c, p)
				continue
			} else {
				c.CloseByError(err)
				return
			}
		}
		if wc, ok := c._removeWait(p); ok {
			wc <- p
			close(wc)
		} else if h != nil {
			h.OnPacket(p, c)
		} else {
			c.l.Infof("Conn[%s].OnPacket in nil, Drop %s", c.ConnString(), p.String())
		}
	}
}

func (c *Conn) _addWait(p *Packet) chan *Packet {
	c.wMtx.Lock()
	defer c.wMtx.Unlock()

	f := uint32(uint16(p.protocol))<<16 | uint32(uint16(p.sn*-1))
	wc, ok := c.waits[f]
	if ok {
		c.l.Debugln("_addWait close prev chan", f, p)
		close(wc)
	}
	wc = make(chan *Packet)
	c.waits[f] = wc
	return wc
}

func (c *Conn) _removeWait(p *Packet) (chan *Packet, bool) {
	c.wMtx.Lock()
	defer c.wMtx.Unlock()

	f := uint32(uint16(p.protocol))<<16 | uint32(uint16(p.sn))
	wc, ok := c.waits[f]
	if ok {
		delete(c.waits, f)
	}
	return wc, ok
}

func (c *Conn) _fillSn(p *Packet) {
	if p.sn == 0 {
		c.snMtx.Lock()
		sn := c.sn[p.protocol]
		sn += 1
		if sn < 1 {
			sn = 1
		}
		c.sn[p.protocol] = sn
		c.snMtx.Unlock()
		p.sn = sn
	}
}

func (c *Conn) _send(p *Packet) error {
	if c == nil || atomic.LoadInt32(&c.closed) == 1 {
		return ErrNotAvailable
	}
	c.l.Traceln("_send", p, c)
	if err := c.conn.SetWriteDeadline(time.Now().Add(DefaultSendTimeout)); err != nil {
		return err
	} else if err := c.writer.WritePacket(p); err != nil {
		return err
	} else if err := c.writer.Flush(); err != nil {
		return err
	}
	return nil
}

func (c *Conn) Send(p *Packet) error {
	c._fillSn(p)
	return c._send(p)
}

func (c *Conn) SendAndReceive(p *Packet) (*Packet, error) {
	c._fillSn(p)
	wc := c._addWait(p)
	err := c._send(p)
	if err != nil {
		c._removeWait(p)
		close(wc)
		return nil, err
	}

	r := <-wc
	return r, nil
}

func (c *Conn) String() string {
	if c == nil {
		return "nil"
	}
	return fmt.Sprintf("{conn:%s, in:%v, channel:%v}",
		c.ConnString(), c.incoming, c.channel)
}
func (c *Conn) ConnString() string {
	if c == nil {
		return ""
	}
	if c.incoming {
		return fmt.Sprint(c.conn.LocalAddr(), "<-", c.conn.RemoteAddr())
	} else {
		return fmt.Sprint(c.conn.LocalAddr(), "->", c.conn.RemoteAddr())
	}
}
func (c *Conn) CloseInfo() string {
	c.closeInfoMtx.RLock()
	defer c.closeInfoMtx.RUnlock()

	reason := "reason:["
	for i, s := range c.closeReason {
		if i != 0 {
			reason += ","
		}
		reason += "\"" + s + "\""
	}
	reason += "],"
	closeErr := "closeErr:["
	for i, e := range c.closeErr {
		if i != 0 {
			closeErr += ","
		}
		if IsCloseError(e) {
			closeErr += "CLOSED_ERR"
		}
		closeErr += fmt.Sprintf("{%T %v}", e, e)
	}
	closeErr += "]"
	return reason + closeErr
}
func (c *Conn) FirstCloseError() error {
	c.closeInfoMtx.RLock()
	defer c.closeInfoMtx.RUnlock()

	if l := len(c.closeErr); l > 0 {
		return c.closeErr[0]
	}
	return nil
}
func (c *Conn) ResetConn(conn net.Conn) {
	c.conn = conn
	c.reader.Reset(conn)
	c.writer.Reset(conn)
}

func (c *Conn) setHandler(h Handler) {
	c.hMtx.Lock()
	c.handler = h
	if h != nil {
		c.once.Do(func() {
			go c.receiveRoutine()
		})
	}
	c.hMtx.Unlock()

	c.handler.OnConn(c)
}

func (c *Conn) getHandler() Handler {
	c.hMtx.RLock()
	defer c.hMtx.RUnlock()

	return c.handler
}

func newConn(conn net.Conn, incoming bool, l log.Logger) *Conn {
	c := &Conn{
		incoming:    incoming,
		conn:        conn,
		reader:      NewPacketReader(conn),
		writer:      NewPacketWriter(conn),
		hMtx:        sync.RWMutex{},
		once:        sync.Once{},
		close:       make(chan error),
		closeReason: make([]string, 0),
		closeErr:    make([]error, 0),
		l:           l,
		sn:          make(map[ProtocolInfo]int16),
		waits:       make(map[uint32]chan *Packet),
	}
	c.l = l.WithFields(log.Fields{"conn": c.channel})
	return c
}
