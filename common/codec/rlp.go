package codec

import (
	"bytes"
	"io"
	"io/ioutil"
	"reflect"

	cerrors "github.com/icon-project/btp/common/errors"
	"github.com/icon-project/btp/common/intconv"
)

var rlpCodecObject rlpCodec
var RLP = bytesWrapper{&rlpCodecObject}

type rlpCodec struct {
}

type rlpReader struct {
	reader io.Reader
}

func sizeToBytes(s int) []byte {
	return intconv.SizeToBytes(uint64(s))
}

func bytesToSize(bs []byte) int {
	return int(intconv.BytesToSize(bs))
}

func (r *rlpReader) skipN(sz int) error {
	if _, err := io.CopyN(ioutil.Discard, r.reader, int64(sz)); err != nil {
		if err == io.EOF {
			return cerrors.Wrapf(ErrInvalidFormat, "InvalidFormat(expect=%d)", sz)
		}
		return cerrors.WithStack(err)
	}
	return nil
}

func (r *rlpReader) readSize(buffer []byte) (int, error) {
	if err := r.readAll(buffer); err != nil {
		return 0, err
	}
	return bytesToSize(buffer), nil
}

func (r *rlpReader) readAll(buffer []byte) error {
	if _, err := io.ReadFull(r.reader, buffer); err != nil {
		if err == io.EOF {
			return cerrors.Wrapf(ErrInvalidFormat, "InvalidFormat(sz=%d,err=%s)", len(buffer), err)
		}
		return cerrors.WithStack(err)
	}
	return nil
}

func (r *rlpReader) skipOne() error {
	var header [9]byte
	if _, err := io.ReadFull(r.reader, header[0:1]); err != nil {
		return err
	}
	tag := int(header[0])
	switch {
	case tag < 0x80:
		return nil
	case tag <= 0xB7:
		size := tag - 0x80
		return r.skipN(size)
	case tag < 0xC0:
		sz := tag - 0xB7
		sz2, err := r.readSize(header[1 : 1+sz])
		if err != nil {
			return err
		}
		return r.skipN(sz2)
	case tag <= 0xF7:
		sz := tag - 0xC0
		return r.skipN(sz)
	default:
		sz := tag - 0xF7
		sz2, err := r.readSize(header[1 : 1+sz])
		if err != nil {
			return err
		}
		return r.skipN(sz2)
	}
}

func (r *rlpReader) Skip(cnt int) error {
	for i := 0; i < cnt; i++ {
		if err := r.skipOne(); err != nil {
			return err
		}
	}
	return nil
}

func (r *rlpReader) readList() (Reader, error) {
	var header [9]byte
	if _, err := io.ReadFull(r.reader, header[0:1]); err != nil {
		return nil, err
	}
	tag := int(header[0])
	switch {
	case tag < 0xC0:
		return nil, cerrors.Wrap(ErrInvalidFormat, "InvalidFormat(RLPBytes)")
	case tag <= 0xF7:
		size := tag - 0xC0
		return &rlpReader{
			reader: io.LimitReader(r.reader, int64(size)),
		}, nil
	default:
		sz := tag - 0xF7
		sz2, err := r.readSize(header[1 : 1+sz])
		if err != nil {
			return nil, err
		}
		if sz == 1 && sz2 == 0 {
			return nil, ErrNilValue
		}
		return &rlpReader{
			reader: io.LimitReader(r.reader, int64(sz2)),
		}, nil
	}
}

func (r *rlpReader) ReadList() (Reader, error) {
	return r.readList()
}

func (r *rlpReader) ReadMap() (Reader, error) {
	return r.readList()
}

func (r *rlpReader) readBytes() ([]byte, error) {
	var header [9]byte
	if _, err := io.ReadFull(r.reader, header[0:1]); err != nil {
		return nil, err
	}
	tag := int(header[0])
	switch {
	case tag < 0x80:
		return []byte{header[0]}, nil
	case tag <= 0xB7:
		buffer := make([]byte, int(header[0])-0x80)
		if err := r.readAll(buffer); err != nil {
			return nil, err
		} else {
			return buffer, nil
		}
	case tag < 0xC0:
		sz := int(header[0]) - 0xB7
		sz2, err := r.readSize(header[1 : 1+sz])
		if err != nil {
			return nil, err
		}
		buffer := make([]byte, sz2)
		if err := r.readAll(buffer); err != nil {
			return nil, err
		} else {
			return buffer, nil
		}
	case tag == 0xF8:
		if sz2, err := r.readSize(header[1:2]); err != nil {
			return nil, err
		} else {
			if sz2 == 0 {
				return nil, ErrNilValue
			}
		}
		fallthrough
	default:
		return nil, cerrors.Wrap(ErrInvalidFormat, "InvalidFormat(RLPList)")
	}
}

func (r *rlpReader) ReadValue(v reflect.Value) error {
	switch v.Kind() {
	case reflect.Bool:
		bs, err := r.readBytes()
		if err != nil {
			return err
		}
		v.SetBool(intconv.BytesToUint64(bs) != 0)
		return nil
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		bs, err := r.readBytes()
		if err != nil {
			return err
		}
		v.SetUint(intconv.BytesToUint64(bs))
		return nil
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		bs, err := r.readBytes()
		if err != nil {
			return err
		}
		v.SetInt(intconv.BytesToInt64(bs))
		return nil
	case reflect.String:
		bs, err := r.readBytes()
		if err != nil {
			return err
		}
		v.SetString(string(bs))
		return nil
	}
	return cerrors.Wrapf(ErrIllegalType, "IllegalType(%s)", v.Type())
}

func (r *rlpReader) Close() error {
	_, err := io.Copy(ioutil.Discard, r.reader)
	return err
}

func (r *rlpReader) ReadBytes() ([]byte, error) {
	return r.readBytes()
}

func (r *rlpReader) readMore(org []byte, size int) ([]byte, error) {
	buffer := make([]byte, len(org)+size)
	copy(buffer, org)
	if err := r.readAll(buffer[len(org):]); err != nil {
		return nil, err
	}
	return buffer, nil
}

func (r *rlpReader) ReadRaw() ([]byte, error) {
	var header [9]byte
	if _, err := io.ReadFull(r.reader, header[0:1]); err != nil {
		return nil, err
	}
	tag := int(header[0])
	switch {
	case tag < 0x80:
		return header[0:1], nil
	case tag <= 0xB7:
		size := tag - 0x80
		return r.readMore(header[0:1], size)
	case tag < 0xC0:
		sz := tag - 0xB7
		sz2, err := r.readSize(header[1 : 1+sz])
		if err != nil {
			return nil, err
		}
		return r.readMore(header[0:1+sz], sz2)
	case tag <= 0xF7:
		sz := tag - 0xC0
		return r.readMore(header[0:1], sz)
	default:
		sz := tag - 0xF7
		sz2, err := r.readSize(header[1 : 1+sz])
		if err != nil {
			return nil, err
		}
		return r.readMore(header[0:1+sz], sz2)
	}
}

type rlpParent struct {
	buffer *bytes.Buffer
	writer *rlpWriter
	isMap  bool
	cnt    int
}

type rlpWriter struct {
	parent *rlpParent
	writer io.Writer
}

func (w *rlpWriter) countN(cnt int) {
	if w.parent != nil {
		w.parent.cnt += cnt
	}
}

func (w *rlpWriter) WriteList() (Writer, error) {
	w.countN(1)
	p := &rlpParent{
		buffer: bytes.NewBuffer(nil),
		writer: w,
	}
	return &rlpWriter{
		parent: p,
		writer: p.buffer,
	}, nil
}

func (w *rlpWriter) WriteMap() (Writer, error) {
	w.countN(1)
	p := &rlpParent{
		buffer: bytes.NewBuffer(nil),
		isMap:  true,
		writer: w,
	}
	return &rlpWriter{
		parent: p,
		writer: p.buffer,
	}, nil
}

func (w *rlpWriter) writeAll(b []byte) error {
	for written := 0; written < len(b); {
		n, err := w.writer.Write(b[written:])
		if err != nil {
			return err
		}
		written += n
	}
	return nil
}

var nullSequence = []byte{0xf8, 0}

func (w *rlpWriter) writeNull() error {
	return w.writeAll(nullSequence)
}

func (w *rlpWriter) writeBytes(b []byte) error {
	if b == nil {
		return w.writeNull()
	}
	switch l := len(b); {
	case l == 0:
		return w.writeAll([]byte{0x80})
	case l == 1:
		if b[0] < 0x80 {
			return w.writeAll(b)
		}
		fallthrough
	case l <= 55:
		var header [1]byte
		header[0] = byte(0x80 + l)
		if err := w.writeAll(header[:]); err != nil {
			return err
		}
		return w.writeAll(b)
	default:
		sz := sizeToBytes(l)
		var header [1]byte
		header[0] = byte(0x80 + 55 + len(sz))
		if err := w.writeAll(header[:]); err != nil {
			return err
		}
		if err := w.writeAll(sz); err != nil {
			return err
		}
		return w.writeAll(b)
	}
}

func (w *rlpWriter) writeList(b []byte) error {
	switch l := len(b); {
	case l == 0:
		return w.writeAll([]byte{0xc0})
	case l <= 55:
		var header [1]byte
		header[0] = byte(0xC0 + l)
		if err := w.writeAll(header[:]); err != nil {
			return err
		}
		return w.writeAll(b)
	default:
		sz := sizeToBytes(l)
		var header [1]byte
		header[0] = byte(0xC0 + 55 + len(sz))
		if err := w.writeAll(header[:]); err != nil {
			return err
		}
		if err := w.writeAll(sz); err != nil {
			return err
		}
		return w.writeAll(b)
	}
}

func (w *rlpWriter) WriteBytes(b []byte) error {
	w.countN(1)
	return w.writeBytes(b)
}

func (w *rlpWriter) WriteRaw(b []byte) error {
	w.countN(1)
	return w.writeAll(b)
}

func (w *rlpWriter) WriteValue(v reflect.Value) error {
	w.countN(1)
	switch v.Kind() {
	case reflect.Bool:
		if v.Bool() {
			return w.writeBytes([]byte{0x1})
		} else {
			return w.writeBytes([]byte{})
		}

	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64:
		if v.Uint() == 0 {
			return w.writeBytes([]byte{})
		}
		return w.writeBytes(bytes.Trim(intconv.Uint64ToBytes(v.Uint()), "\x00"))

	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		if v.Int() == 0 {
			return w.writeBytes([]byte{})
		}
		return w.writeBytes(bytes.Trim(intconv.Int64ToBytes(v.Int()), "\x00"))

	case reflect.String:
		return w.writeBytes([]byte(v.String()))

	default:
		return cerrors.Wrapf(ErrIllegalType, "IllegalType(%s)", v.Kind())
	}
}

func (w *rlpWriter) WriteNull(v reflect.Value) error {
	w.countN(1)
	return w.writeNull()
}

func (w *rlpWriter) Close() error {
	if p := w.parent; p != nil {
		if p.isMap {
			if (p.cnt % 2) != 0 {
				return ErrInvalidFormat
			}
		}
		if err := p.writer.writeList(p.buffer.Bytes()); err != nil {
			return err
		}
		w.parent = nil
	}
	return nil
}

func (c *rlpCodec) NewDecoder(r io.Reader) SimpleDecoder {
	return NewDecoder(&rlpReader{
		reader: r,
	})
}

func (c *rlpCodec) NewEncoder(w io.Writer) SimpleEncoder {
	return NewEncoder(&rlpWriter{
		writer: w,
	})
}
