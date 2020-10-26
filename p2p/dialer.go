package p2p

import (
	"net"
	"time"

	"github.com/icon-project/btp/common/log"
)

const (
	DefaultDialTimeout = 5 * time.Second
)

type Dialer struct {
	Chain         string
	Address       string
	Logger        log.Logger
	Authenticator *Authenticator
}

func (d *Dialer) Dial(h Handler) (*Conn, error) {
	conn, err := net.DialTimeout(DefaultNetwork, d.Address, DefaultDialTimeout)
	if err != nil {
		return nil, err
	}
	c := newConn(conn, false, d.Logger)
	c.channel = d.Chain
	ch := make(chan error)
	d.Authenticator.Authenticate(c, func(c *Conn, err error) {
		if err == nil {
			c.setHandler(h)
		}
		ch <- err
	})
	if err = <-ch; err != nil {
		return nil, err
	}
	return c, nil
}

func NewDialer(chain string, address string, a *Authenticator, l log.Logger) (*Dialer, error) {
	d := &Dialer{
		Chain:         chain,
		Address:       address,
		Logger:        l,
		Authenticator: a,
	}
	return d, nil
}
