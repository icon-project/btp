package p2p

import (
	"io"
	"net"
	"strings"

	"github.com/icon-project/btp/common/errors"
)

const (
	AlreadyListenedError = errors.CodeGeneral + iota
	AlreadyClosedError
	NotRegisteredProtocolError
	NotAvailableError
)

var (
	ErrAlreadyListened           = errors.NewBase(AlreadyListenedError, "AlreadyListened")
	ErrAlreadyClosed             = errors.NewBase(AlreadyClosedError, "AlreadyClosed")
	ErrNotRegisteredProtocol     = errors.NewBase(NotRegisteredProtocolError, "NotRegisteredProtocol")
	ErrNotAvailable              = errors.NewBase(NotAvailableError, "NotAvailable")
)


func IsTemporaryError(err error) bool {
	if oe, ok := err.(*net.OpError); ok { //after p.conn.Close()
		// log.Printf("IsTemporaryError OpError %+v %#v %#v %s", oe, oe, oe.Err, p.String())
		// if se, ok := oe.Err.(*os.SyscallError); ok {
		// 	log.Printf("IsTemporaryError *os.SyscallError %+v %#v %#v %s", se, se.Err, se.Err, p.String())
		// }
		return oe.Temporary()
	}
	return false
}

func IsCloseError(err error) bool {
	if oe, ok := err.(*net.OpError); ok {
		// if se, ok := oe.Err.(syscall.Errno); ok {
		// 	return se == syscall.ECONNRESET || se == syscall.ECONNABORTED
		// }
		//referenced from golang.org/x/net/http2/server.go isClosedConnError
		if strings.Contains(oe.Err.Error(), "use of closed network connection") ||
			strings.Contains(oe.Err.Error(), "connection reset by peer") {
			return true
		}
	} else if err == io.EOF || err == io.ErrUnexpectedEOF { //half Close (recieved tcp close)
		return true
	}
	return false
}
