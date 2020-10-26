package p2p

import (
	"crypto/elliptic"
	"io"
	"net"
	"sync"

	"github.com/icon-project/btp/common/log"
)

const (
	LoggerFieldKeySubModule = "sub"
	DefaultNetwork          = "tcp"
)

var (
	DefaultSecureEllipticCurve = elliptic.P256()
	DefaultSecureSuites        = []SecureSuite{
		SecureSuiteNone,
		SecureSuiteEcdhe,
		SecureSuiteTls,
	}
	DefaultSecureAeadSuites = []SecureAeadSuite{
		SecureAeadSuiteChaCha20Poly1305,
		SecureAeadSuiteAes128Gcm,
		SecureAeadSuiteAes256Gcm,
	}
	DefaultSecureKeyLogWriter io.Writer
)

type Server struct {
	Address       string
	Listener      net.Listener
	mtx           sync.Mutex
	closeCh       chan bool
	Logger        log.Logger
	Authenticator *Authenticator
	Handler       Handler
}

func (s *Server) onAuthentication(c *Conn, err error) {
	if err == nil {
		c.setHandler(s.Handler)
	}
}

func (s *Server) Listen() error {
	defer s.mtx.Unlock()
	s.mtx.Lock()

	if s.Listener != nil {
		return ErrAlreadyListened
	}
	listener, err := net.Listen(DefaultNetwork, s.Address)
	if err != nil {
		return err
	}
	s.Logger.Infof("Starting P2PServer with %s", listener.Addr())
	s.Listener = listener
	s.closeCh = make(chan bool)
	return nil
}

func (s *Server) Start() error {
	if err := s.Listen(); err != nil {
		return err
	}
	go s.Serve()
	return nil
}

func (s *Server) ListenAndServe() error {
	if err := s.Listen(); err != nil {
		return err
	}
	s.Serve()
	return nil
}

func (s *Server) Stop() error {
	defer s.mtx.Unlock()
	s.mtx.Lock()

	if s.Listener == nil {
		return ErrAlreadyClosed
	}
	err := s.Listener.Close()
	if err != nil {
		return err
	}
	<-s.closeCh

	s.Listener = nil
	return nil
}

func (s *Server) Serve() {
	defer close(s.closeCh)

	for {
		conn, err := s.Listener.Accept()
		if err != nil {
			s.Logger.Infoln("Serve", err)
			return
		}
		s.onAccept(conn)
	}
}

func (s *Server) onAccept(conn net.Conn) {
	c := newConn(conn, true, s.Logger)
	s.Authenticator.Authenticate(c, s.onAuthentication)
}

func NewServer(address string, a *Authenticator, h Handler, l log.Logger) *Server {
	s := &Server{
		Address:       address,
		Logger:        l,
		Handler:       h,
		Authenticator: a,
	}
	return s
}
