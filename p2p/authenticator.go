package p2p

import (
	"bytes"
	"crypto/tls"
	"fmt"
	"sync"

	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/wallet"
)

var (
	protoAuthKey  = ProtocolInfo(0x0100)
	protoAuthSign = ProtocolInfo(0x0200)
)

type Authenticator struct {
	*handler
	w            wallet.Wallet
	secureSuites map[string][]SecureSuite
	secureAeads  map[string][]SecureAeadSuite
	secureKeyNum int
	secureMtx    sync.RWMutex
	mtx          sync.Mutex

	cb    map[*Conn]AuthCallback
	cbMtx sync.Mutex
}

type AuthCallback func(c *Conn, err error)

func NewAuthenticator(w wallet.Wallet, l log.Logger) *Authenticator {
	_, err := crypto.ParsePublicKey(w.PublicKey())
	if err != nil {
		panic(err)
	}
	a := &Authenticator{
		w:            w,
		secureSuites: make(map[string][]SecureSuite),
		secureAeads:  make(map[string][]SecureAeadSuite),
		secureKeyNum: 2,
		cb:           make(map[*Conn]AuthCallback),
		handler:      newHandler(l.WithFields(log.Fields{LoggerFieldKeySubModule: "authenticator"})),
	}
	return a
}

func (a *Authenticator) Wallet() wallet.Wallet {
	return a.w
}

func (a *Authenticator) addCallback(c *Conn, cb AuthCallback) {
	a.cbMtx.Lock()
	defer a.cbMtx.Unlock()

	a.cb[c] = cb
}

func (a *Authenticator) removeCallback(c *Conn) (AuthCallback, bool) {
	a.cbMtx.Lock()
	defer a.cbMtx.Unlock()

	cb, ok := a.cb[c]
	if ok {
		delete(a.cb, c)
	}
	return cb, ok
}

func (a *Authenticator) call(c *Conn, err error) {
	if cb, ok := a.removeCallback(c); ok {
		cb(c, err)
	} else if err == nil {
		c.Close("not exists AuthenticateCallback when finish")
	}
}

func (a *Authenticator) Authenticate(c *Conn, cb AuthCallback) {
	if cb != nil {
		a.addCallback(c, cb)
	}
	c.setHandler(a)
}

func (a *Authenticator) OnConn(c *Conn) {
	a.l.Debugln("OnConn", c)
	if !c.Incoming() {
		a.sendSecureRequest(c)
	}
}

func (a *Authenticator) OnPacket(pkt *Packet, c *Conn) {
	a.l.Debugln("OnPacket", pkt, c)
	switch pkt.protocol {
	case protoAuthKey:
		if pkt.Sn() > 0 {
			a.handleSecureRequest(pkt, c)
		} else {
			a.handleSecureResponse(pkt, c)
		}
	case protoAuthSign:
		if pkt.Sn() > 0 {
			a.handleSignatureRequest(pkt, c)
		} else {
			a.handleSignatureResponse(pkt, c)
		}
	default:
		a.l.Infoln("not registered protocol", pkt, c)
		c.CloseByError(ErrNotRegisteredProtocol)
	}
}

func (a *Authenticator) OnError(err error, p *Conn, pkt *Packet) {
	a.l.Debugln("OnError", err, p, pkt)
}

func (a *Authenticator) OnClose(c *Conn) {
	a.l.Debugln("OnClose", c)
	err := c.FirstCloseError()
	if err == nil {
		err = fmt.Errorf(c.CloseInfo())
	}
	a.call(c, err)
}

func (a *Authenticator) Signature(content []byte) []byte {
	defer a.mtx.Unlock()
	a.mtx.Lock()
	h := crypto.SHA3Sum256(content)
	sb, _ := a.w.Sign(h)
	return sb
}

func (a *Authenticator) VerifySignature(publicKey []byte, signature []byte, content []byte) (*crypto.PublicKey, error) {
	pubKey, err := crypto.ParsePublicKey(publicKey)
	if err != nil {
		return nil, fmt.Errorf("fail to parse public key : %s", err.Error())
	}
	s, err := crypto.ParseSignature(signature)
	if err != nil {
		return nil, fmt.Errorf("fail to parse signature : %s", err.Error())
	}
	h := crypto.SHA3Sum256(content)
	if !s.Verify(h, pubKey) {
		err = fmt.Errorf("fail to verify signature")
	}
	return pubKey, err
}

func (a *Authenticator) SetSecureSuites(channel string, ss []SecureSuite) error {
	a.secureMtx.Lock()
	defer a.secureMtx.Unlock()

	for i, s := range ss {
		for j := i + 1; j < len(ss); j++ {
			if s == ss[j] {
				return fmt.Errorf("duplicate set %s index:%d and %d", s, i, j)
			}
		}
	}
	a.secureSuites[channel] = ss
	return nil
}

func (a *Authenticator) GetSecureSuites(channel string) []SecureSuite {
	a.secureMtx.RLock()
	defer a.secureMtx.RUnlock()

	suites, ok := a.secureSuites[channel]
	if !ok || len(suites) == 0 {
		return DefaultSecureSuites
	}
	return suites
}

func (a *Authenticator) SetSecureAeads(channel string, sas []SecureAeadSuite) error {
	a.secureMtx.Lock()
	defer a.secureMtx.Unlock()

	for i, sa := range sas {
		for j := i + 1; j < len(sas); j++ {
			if sa == sas[j] {
				return fmt.Errorf("duplicate set %s index:%d and %d", sa, i, j)
			}
		}
	}
	a.secureAeads[channel] = sas
	return nil
}

func (a *Authenticator) GetSecureAeads(channel string) []SecureAeadSuite {
	a.secureMtx.RLock()
	defer a.secureMtx.RUnlock()

	aeads, ok := a.secureAeads[channel]
	if !ok || len(aeads) == 0 {
		return DefaultSecureAeadSuites
	}
	return aeads
}

type SecureRequest struct {
	Channel          string
	SecureSuites     []SecureSuite
	SecureAeadSuites []SecureAeadSuite
	SecureParam      []byte
}
type SecureResponse struct {
	Channel         string
	SecureSuite     SecureSuite
	SecureAeadSuite SecureAeadSuite
	SecureParam     []byte
	SecureError     SecureError
}
type SignatureRequest struct {
	PublicKey []byte
	Signature []byte
}
type SignatureResponse struct {
	PublicKey []byte
	Signature []byte
	Error     string
}

func (a *Authenticator) sendSecureRequest(c *Conn) {
	c.secureKey = newSecureKey(DefaultSecureEllipticCurve, DefaultSecureKeyLogWriter)
	sss := a.secureSuites[c.channel]
	if len(sss) == 0 {
		sss = DefaultSecureSuites
	}
	sas := a.secureAeads[c.channel]
	if len(sas) == 0 {
		sas = DefaultSecureAeadSuites
	}
	m := &SecureRequest{
		Channel:          c.channel,
		SecureSuites:     sss,
		SecureAeadSuites: sas,
		SecureParam:      c.secureKey.marshalPublicKey(),
	}
	a.send(protoAuthKey, m, c)
	a.l.Traceln("sendSecureRequest", m, c)
}

func (a *Authenticator) handleSecureRequest(pkt *Packet, c *Conn) {
	rm := &SecureRequest{}
	a.decode(pkt.payload, rm)
	a.l.Traceln("handleSecureRequest", rm, c)

	m := &SecureResponse{
		Channel:         c.channel,
		SecureSuite:     SecureSuiteUnknown,
		SecureAeadSuite: SecureAeadSuiteUnknown,
	}

	sss := a.secureSuites[rm.Channel]
	if len(sss) == 0 {
		sss = DefaultSecureSuites
	}
SecureSuiteLoop:
	for _, ss := range sss {
		for _, rss := range rm.SecureSuites {
			if rss == ss {
				m.SecureSuite = ss
				a.l.Traceln("handleSecureRequest", c.ConnString(), "SecureSuite", ss)
				break SecureSuiteLoop
			}
		}
	}
	if m.SecureSuite == SecureSuiteUnknown {
		m.SecureError = SecureErrorInvalid
	}

	sas := a.secureAeads[rm.Channel]
	if len(sas) == 0 {
		sas = DefaultSecureAeadSuites
	}
SecureAeadLoop:
	for _, sa := range sas {
		for _, rsa := range rm.SecureAeadSuites {
			if rsa == sa {
				m.SecureAeadSuite = sa
				a.l.Traceln("handleSecureRequest", c.ConnString(), "SecureAeadSuite", sa)
				break SecureAeadLoop
			}
		}
	}
	if m.SecureAeadSuite == SecureAeadSuiteUnknown && (m.SecureSuite == SecureSuiteEcdhe || m.SecureSuite == SecureSuiteTls) {
		m.SecureError = SecureErrorInvalid
	}

	switch c.conn.(type) {
	case *SecureConn:
		m.SecureSuite = SecureSuiteEcdhe
		m.SecureError = SecureErrorEstablished
	case *tls.Conn:
		m.SecureSuite = SecureSuiteTls
		m.SecureError = SecureErrorEstablished
	default:
		c.secureKey = newSecureKey(DefaultSecureEllipticCurve, DefaultSecureKeyLogWriter)
		m.SecureParam = c.secureKey.marshalPublicKey()
	}

	a.response(pkt, m, c)
	if m.SecureError != SecureErrorNone {
		err := fmt.Errorf("handleSecureRequest error[%v]", m.SecureError)
		a.l.Infoln("handleSecureRequest", c.ConnString(), "SecureError", err)
		c.CloseByError(err)
		return
	}

	c.channel = rm.Channel

	err := c.secureKey.setup(m.SecureAeadSuite, rm.SecureParam, c.incoming, a.secureKeyNum)
	if err != nil {
		a.l.Infoln("handleSecureRequest", c.ConnString(), "failed secureKey.setup", err)
		c.CloseByError(err)
		return
	}
	switch m.SecureSuite {
	case SecureSuiteEcdhe:
		secureConn, err := NewSecureConn(c.conn, m.SecureAeadSuite, c.secureKey)
		if err != nil {
			a.l.Infoln("handleSecureRequest", c.ConnString(), "failed NewSecureConn", err)
			c.CloseByError(err)
			return
		}
		c.ResetConn(secureConn)
	case SecureSuiteTls:
		config, err := c.secureKey.tlsConfig()
		if err != nil {
			a.l.Infoln("handleSecureRequest", c.ConnString(), "failed tlsConfig", err)
			c.CloseByError(err)
			return
		}
		tlsConn := tls.Server(c.conn, config)
		c.ResetConn(tlsConn)
	}
}

func (a *Authenticator) handleSecureResponse(pkt *Packet, c *Conn) {
	rm := &SecureResponse{}
	a.decode(pkt.payload, rm)
	a.l.Traceln("handleSecureResponse", rm, c)

	if rm.SecureError != SecureErrorNone {
		err := fmt.Errorf("handleSecureResponse error[%v]", rm.SecureError)
		a.l.Infoln("handleSecureResponse", c.ConnString(), "SecureError", err)
		c.CloseByError(err)
		return
	}

	var rss SecureSuite = SecureSuiteUnknown
	sss := a.secureSuites[c.channel]
	if len(sss) == 0 {
		sss = DefaultSecureSuites
	}
SecureSuiteLoop:
	for _, ss := range sss {
		if ss == rm.SecureSuite {
			rss = ss
			break SecureSuiteLoop
		}
	}
	if rss == SecureSuiteUnknown {
		err := fmt.Errorf("handleSecureResponse invalid SecureSuite %d", rm.SecureSuite)
		a.l.Infoln("handleSecureResponse", c.ConnString(), "SecureError", err)
		c.CloseByError(err)
		return
	}

	var rsa SecureAeadSuite = SecureAeadSuiteUnknown
	sas := a.secureAeads[rm.Channel]
	if len(sas) == 0 {
		sas = DefaultSecureAeadSuites
	}
SecureAeadLoop:
	for _, sa := range sas {
		if sa == rm.SecureAeadSuite {
			rsa = sa
			break SecureAeadLoop
		}
	}
	if rsa == SecureAeadSuiteUnknown && (rss == SecureSuiteEcdhe || rss == SecureSuiteTls) {
		err := fmt.Errorf("handleSecureResponse invalid SecureSuite %d SecureAeadSuite %d", rm.SecureSuite, rm.SecureAeadSuite)
		a.l.Infoln("handleSecureResponse", c.ConnString(), "SecureError", err)
		c.CloseByError(err)
		return
	}

	var secured bool
	switch c.conn.(type) {
	case *SecureConn:
		secured = true
	case *tls.Conn:
		secured = true
	}
	if secured {
		err := fmt.Errorf("handleSecureResponse already established secure connection %T", c.conn)
		a.l.Infoln("handleSecureResponse", c.ConnString(), "SecureError", err)
		c.CloseByError(err)
		return
	}

	err := c.secureKey.setup(rm.SecureAeadSuite, rm.SecureParam, c.incoming, a.secureKeyNum)
	if err != nil {
		a.l.Infoln("handleSecureRequest", c.ConnString(), "failed secureKey.setup", err)
		c.CloseByError(err)
		return
	}
	switch rm.SecureSuite {
	case SecureSuiteEcdhe:
		secureConn, err := NewSecureConn(c.conn, rm.SecureAeadSuite, c.secureKey)
		if err != nil {
			a.l.Infoln("handleSecureResponse", c.ConnString(), "failed NewSecureConn", err)
			c.CloseByError(err)
			return
		}
		c.ResetConn(secureConn)
	case SecureSuiteTls:
		config, err := c.secureKey.tlsConfig()
		if err != nil {
			a.l.Infoln("handleSecureResponse", c.ConnString(), "failed tlsConfig", err)
			c.CloseByError(err)
			return
		}
		tlsConn := tls.Client(c.conn, config)
		if err := tlsConn.Handshake(); err != nil {
			a.l.Infoln("handleSecureResponse", c.ConnString(), "failed tls handshake", err)
			c.CloseByError(err)
			return
		}
		c.ResetConn(tlsConn)
	}

	m := &SignatureRequest{
		PublicKey: a.w.PublicKey(),
		Signature: a.Signature(c.secureKey.extra),
	}
	a.send(protoAuthSign, m, c)
}

func (a *Authenticator) handleSignatureRequest(pkt *Packet, c *Conn) {
	rm := &SignatureRequest{}
	a.decode(pkt.payload, rm)
	a.l.Traceln("handleSignatureRequest", rm, c)

	m := &SignatureResponse{
		PublicKey: a.w.PublicKey(),
		Signature: a.Signature(c.secureKey.extra),
	}

	pubKey, err := a.VerifySignature(rm.PublicKey, rm.Signature, c.secureKey.extra)
	if err != nil {
		m = &SignatureResponse{Error: err.Error()}
	} else if bytes.Equal(pubKey.SerializeCompressed(), a.w.PublicKey()) {
		m = &SignatureResponse{Error: "selfAddress"}
	}
	c.pubKey = pubKey
	a.response(pkt, m, c)

	if m.Error != "" {
		err := fmt.Errorf("handleSignatureRequest error[%v]", m.Error)
		a.l.Infoln("handleSignatureRequest", c.ConnString(), "Error", err)
		c.CloseByError(err)
		return
	}

	a.call(c, nil)
}

func (a *Authenticator) handleSignatureResponse(pkt *Packet, c *Conn) {
	rm := &SignatureResponse{}
	a.decode(pkt.payload, rm)
	a.l.Traceln("handleSignatureResponse", rm, c)

	if rm.Error != "" {
		err := fmt.Errorf("handleSignatureResponse error[%v]", rm.Error)
		a.l.Infoln("handleSignatureResponse", c.ConnString(), "Error", err)
		c.CloseByError(err)
		return
	}

	pubKey, err := a.VerifySignature(rm.PublicKey, rm.Signature, c.secureKey.extra)
	if err != nil {
		err := fmt.Errorf("handleSignatureResponse error[%v]", err)
		a.l.Infoln("handleSignatureResponse", c.ConnString(), "Error", err)
		c.CloseByError(err)
		return
	}
	c.pubKey = pubKey
	a.call(c, nil)
}
