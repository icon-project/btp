package p2p

import (
	"bufio"
	"bytes"
	"encoding/binary"
	"fmt"
	"hash"
	"hash/fnv"
	"io"
	"sync"
	"time"
)

const (
	packetHeaderSize = 8
	DefaultPacketBufferSize     = 4096 //bufio.defaultBufSize=4096
	DefaultPacketPayloadMax     = 1024 * 1024
	DefaultPacketRewriteLimit   = 10
	DefaultPacketRewriteDelay   = 100 * time.Millisecond

)

type ProtocolInfo uint16

func newProtocolInfo(id byte, version byte) ProtocolInfo {
	return ProtocolInfo(int(id)<<8 | int(version))
}
func (pi ProtocolInfo) ID() byte {
	return byte(pi >> 8)
}
func (pi ProtocolInfo) Version() byte {
	return byte(pi)
}
func (pi ProtocolInfo) String() string {
	return fmt.Sprintf("{%#04x}", pi.Uint16())
}
func (pi ProtocolInfo) Uint16() uint16 {
	return uint16(pi)
}

type Packet struct {
	//header
	protocol        ProtocolInfo //2byte
	sn              int16       //2byte
	lengthOfPayload uint32       //4byte

	//bytes
	header  []byte
	payload []byte
}

func NewPacket(pi ProtocolInfo, payload []byte) *Packet {
	lengthOfPayload := len(payload)
	if lengthOfPayload > DefaultPacketPayloadMax {
		lengthOfPayload = DefaultPacketPayloadMax
	}
	return &Packet{
		protocol:        pi,
		lengthOfPayload: uint32(lengthOfPayload),
		payload:         payload[:lengthOfPayload],
	}
}

func (p *Packet) String() string {
	return fmt.Sprintf("{pi:%#04x,sn:%d,len:%v}",
		p.protocol.Uint16(),
		p.sn,
		p.lengthOfPayload)
}

func (p *Packet) Protocol() ProtocolInfo {
	return p.protocol
}

func (p *Packet) Sn() int16 {
	return p.sn
}

func (p *Packet) Len() int {
	return packetHeaderSize + len(p.payload)
}

func (p *Packet) Payload() []byte {
	return p.payload
}

func (p *Packet) Response(payload []byte) *Packet {
	r := NewPacket(p.protocol, payload)
	r.sn = p.sn * -1
	return r
}

func (p *Packet) _read(r io.Reader, n int) ([]byte, int, error) {
	if n < 0 {
		return nil, 0, fmt.Errorf("invalid n:%d", n)
	}
	b := make([]byte, n)
	if n == 0 {
		return b, 0, nil
	}
	rn := 0
	for {
		tn, err := r.Read(b[rn:])
		if rn += tn; err != nil {
			return nil, rn, err
		}
		if rn >= n {
			break
		}
	}
	return b, rn, nil
}

func (p *Packet) WriteTo(w io.Writer) (n int64, err error) {
	var tn int
	tn, err = w.Write(p.headerToBytes(false))
	if n += int64(tn); err != nil {
		return
	}
	tn, err = w.Write(p.payload[:p.lengthOfPayload])
	if n += int64(tn); err != nil {
		return
	}
	return
}

func (p *Packet) _hash(force bool) (hash.Hash64, error) {
	h := fnv.New64a()
	if _, err := h.Write(p.headerToBytes(force)); err != nil {
		return nil, err
	}
	if _, err := h.Write(p.payload[:p.lengthOfPayload]); err != nil {
		return nil, err
	}
	return h, nil
}

func (p *Packet) headerToBytes(force bool) []byte {
	if force || p.header == nil {
		p.header = make([]byte, packetHeaderSize)
		tb := p.header[:]
		binary.BigEndian.PutUint16(tb[:2], p.protocol.Uint16())
		tb = tb[2:]
		binary.BigEndian.PutUint16(tb[:2], uint16(p.sn))
		tb = tb[2:]
		binary.BigEndian.PutUint32(tb[:4], p.lengthOfPayload)
		tb = tb[4:]
	}
	return p.header[:]
}

func (p *Packet) ReadFrom(r io.Reader) (n int64, err error) {
	var b []byte
	var tn int
	b, tn, err = p._read(r, packetHeaderSize)
	if n += int64(tn); err != nil {
		return
	}
	if _, err = p.setHeader(b); err != nil {
		return
	}

	p.payload, tn, err = p._read(r, int(p.lengthOfPayload))
	if n += int64(tn); err != nil {
		return
	}
	return
}

func (p *Packet) setHeader(b []byte) ([]byte, error) {
	if len(b) < packetHeaderSize {
		//io.ErrShortBuffer
		return b, fmt.Errorf("short buffer")
	}
	p.header = b[:packetHeaderSize]
	tb := p.header[:]
	p.protocol = ProtocolInfo(binary.BigEndian.Uint16(tb[:2]))
	tb = tb[2:]
	p.sn = int16(binary.BigEndian.Uint16(tb[:2]))
	tb = tb[2:]
	p.lengthOfPayload = binary.BigEndian.Uint32(tb[:4])
	tb = tb[4:]
	if p.lengthOfPayload > DefaultPacketPayloadMax {
		return b[packetHeaderSize:], fmt.Errorf("invalid lengthOfPayload")
	}
	return b[packetHeaderSize:], nil
}

type PacketReader struct {
	*bufio.Reader
	rd   io.Reader
	pkt  *Packet
	hash hash.Hash64
}

// NewReader returns a new Reader whose buffer has the default size.
func NewPacketReader(rd io.Reader) *PacketReader {
	return &PacketReader{Reader: bufio.NewReaderSize(rd, DefaultPacketBufferSize), rd: rd}
}

func (pr *PacketReader) _read(n int) ([]byte, error) {
	b := make([]byte, n)
	rn := 0
	for {
		tn, err := pr.Reader.Read(b[rn:])
		if err != nil {
			return nil, err
		}
		rn += tn
		if rn >= n {
			break
		}
	}
	return b, nil
}

func (pr *PacketReader) Reset(rd io.Reader) {
	pr.rd = rd
	pr.Reader.Reset(pr.rd)
}

func (pr *PacketReader) ReadPacket() (pkt *Packet, e error) {
	pkt = &Packet{}
	_, err := pkt.ReadFrom(pr)
	if err != nil {
		e = err
		return
	}
	return
}

type PacketWriter struct {
	*bufio.Writer
	wr io.Writer
}

func NewPacketWriter(w io.Writer) *PacketWriter {
	return &PacketWriter{Writer: bufio.NewWriterSize(w, DefaultPacketBufferSize), wr: w}
}

func (pw *PacketWriter) Reset(wr io.Writer) {
	pw.wr = wr
	pw.Writer.Reset(pw.wr)
}

func (pw *PacketWriter) WritePacket(pkt *Packet) error {
	_, err := pkt.WriteTo(pw)
	if err != nil {
		return err
	}
	return nil
}

func (pw *PacketWriter) Write(b []byte) (int, error) {
	wn := 0
	re := 0
	for {
		n, err := pw.Writer.Write(b[wn:])
		wn += n
		if err != nil && err == io.ErrShortWrite && re < DefaultPacketRewriteLimit {
			re++
			time.Sleep(DefaultPacketRewriteDelay)
			continue
		} else {
			return wn, err
		}
	}
}

func (pw *PacketWriter) Flush() error {
	re := 0
	for {
		err := pw.Writer.Flush()
		if err != nil && err == io.ErrShortWrite && re < DefaultPacketRewriteLimit {
			re++
			time.Sleep(DefaultPacketRewriteDelay)
			continue
		} else {
			return err
		}
	}
}

type PacketReadWriter struct {
	b    *bytes.Buffer
	rd   *PacketReader
	wr   *PacketWriter
	rpkt *Packet
	wpkt *Packet
	mtx  sync.RWMutex
}

func NewPacketReadWriter() *PacketReadWriter {
	b := bytes.NewBuffer(make([]byte, DefaultPacketBufferSize))
	b.Reset()
	return &PacketReadWriter{b: b, rd: NewPacketReader(b), wr: NewPacketWriter(b)}
}

func (prw *PacketReadWriter) WritePacket(pkt *Packet) error {
	defer prw.mtx.Unlock()
	prw.mtx.Lock()
	if err := prw.wr.WritePacket(pkt); err != nil {
		return err
	}
	if err := prw.wr.Flush(); err != nil {
		return err
	}
	prw.wpkt = pkt
	return nil
}

func (prw *PacketReadWriter) ReadPacket() (*Packet, error) {
	defer prw.mtx.RUnlock()
	prw.mtx.RLock()
	if prw.rpkt == nil {
		//(pkt *Packet, h footer.Hash64, e error)
		pkt, err := prw.rd.ReadPacket()
		if err != nil {
			return nil, err
		}
		prw.rpkt = pkt
	}
	return prw.rpkt, nil
}

func (prw *PacketReadWriter) Reset(rd io.Reader, wr io.Writer) {
	defer prw.mtx.Unlock()
	prw.mtx.Lock()
	prw.b.Reset()
	prw.rd.Reset(rd)
	prw.wr.Reset(wr)
	prw.rpkt = nil
	prw.wpkt = nil
}
