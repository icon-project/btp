package common

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"io/ioutil"
	"mime"
	"mime/multipart"
	"net"
	"net/http"
	"net/textproto"
	"net/url"
	"os"
	"path"
	"path/filepath"
	"strings"
	"syscall"
	"text/template"
	"time"

	"github.com/labstack/echo/v4"
)

const (
	DefaultHttpNetwork = "tcp"
)

type HttpServer struct {
	srv     http.Server
	l       net.Listener
	e       *echo.Echo
	network string
	address string
}

func NewHttpServer(address string, e *echo.Echo) *HttpServer {
	if e == nil {
		e = echo.New()
	}
	s := &HttpServer{
		e: e,
	}
	s.srv.Handler = s.e
	s.srv.ErrorLog = s.e.StdLogger
	s.network, s.address = parseAddress(address)
	return s
}

func parseAddress(address string) (string, string) {
	switch t := strings.Split(address, "://"); {
	case len(t) == 2:
		return t[0], t[1]
	default:
		return DefaultHttpNetwork, address
	}
}

//socket path platform-specific length Mac&BSD:104, Linux:108
//when net.Dial return error as
//  (*net.OpError).Err.(*os.SyscallError).Err.(syscall.Errno) == syscall.EINVAL
//[TBD] symbolic link cannot resolved
func resolveUnixPath(sockPath string) (string, error) {
	abs, err := filepath.Abs(sockPath)
	if err != nil {
		return "", err
	}
	if abs == "/" {
		return "", fmt.Errorf("cannot be '/'")
	}

	wd, _ := filepath.Abs(".")
	rel, _ := filepath.Rel(wd, abs)
	if len(abs) > len(rel) {
		return rel, nil
	} else {
		return abs, nil
	}
}

func (s *HttpServer) Start() error {
	address := s.address
	if strings.HasPrefix(s.network, "unix") {
		sockPath, err := resolveUnixPath(s.address)
		if err != nil {
			return err
		}
		if err := os.RemoveAll(sockPath); err != nil {
			return err
		}
		address = sockPath
	}
	l, err := net.Listen(s.network, address)
	if err != nil {
		return err
	}
	s.l = l
	if err := s.srv.Serve(s.l); err != nil {
		return err
	}
	return nil
}

func (s *HttpServer) Stop() error {
	ctx, cf := context.WithTimeout(context.Background(), 5*time.Second)
	defer cf()
	return s.srv.Shutdown(ctx)
}

func (s *HttpServer) Address() string {
	return fmt.Sprintf("%s://%s", s.network, s.address)
}

func (s *HttpServer) ListenAddress() string {
	return s.l.Addr().String()
}

func (s *HttpServer) Echo() *echo.Echo {
	return s.e
}

type HttpClient struct {
	hc      http.Client
	network string
	address string
	baseUrl string
}

func NewHttpClient(address string, contextPath string) *HttpClient {
	c := &HttpClient{}
	c.network, c.address = parseAddress(address)
	if strings.HasPrefix(c.network, "unix") {
		c.hc.Transport = &http.Transport{
			DialContext: func(_ context.Context, _, _ string) (conn net.Conn, e error) {
				sockPath, err := resolveUnixPath(c.address)
				if err != nil {
					return nil, err
				}
				return net.Dial(c.network, sockPath)
			},
		}
		c.baseUrl = "http://localhost"
	} else {
		if strings.HasPrefix(c.network, "http") {
			c.baseUrl = address
		} else if c.network == "" || c.network == "tcp" {
			c.baseUrl = "http://" + c.address
		}
	}
	c.baseUrl += contextPath
	return c
}

func (c *HttpClient) _do(req *http.Request) (resp *http.Response, err error) {
	resp, err = c.hc.Do(req)
	if err != nil {
		return
	}
	if resp.StatusCode != http.StatusOK {
		err = NewHttpError(resp)
		return
	}
	return
}

func (c *HttpClient) Do(method, reqUrl string, reqPtr, respPtr interface{}) (resp *http.Response, err error) {
	var reqB io.Reader
	if reqPtr != nil {
		b, mErr := json.Marshal(reqPtr)
		if mErr != nil {
			err = mErr
			return
		}
		reqB = bytes.NewBuffer(b)
	}
	req, err := http.NewRequest(method, c.baseUrl+reqUrl, reqB)
	if err != nil {
		return
	}

	//if reqB != nil {
	//	log.Println("Using json header")
	req.Header.Set("Content-Type", "application/json")
	//} else {
	//	log.Println("Using text header")
	//	req.Header.Set("Accept","*/*")
	//}

	resp, err = c._do(req)
	if err != nil {
		return
	}
	err = readResponseBody(resp, respPtr)
	return
}

func readResponseBody(resp *http.Response, respPtr interface{}) error {
	if respPtr != nil {
		defer resp.Body.Close()
		switch ptr := respPtr.(type) {
		case *string:
			var b []byte
			b, err := ioutil.ReadAll(resp.Body)
			if err != nil {
				return fmt.Errorf("failed read err=%+v", err)
			}
			*ptr = string(b)
		default:
			if err := json.NewDecoder(resp.Body).Decode(ptr); err != nil {
				return fmt.Errorf("failed json decode err=%+v", err)
			}
		}
	}
	return nil
}

type HttpStreamFunc func(respPtr interface{}) error

func (c *HttpClient) Stream(reqUrl string, reqPtr, respPtr interface{},
	respFunc HttpStreamFunc, cancelCh <-chan bool, reqParams ...*url.Values) (resp *http.Response, err error) {
	var reqB io.Reader
	if reqPtr != nil {
		b, mErr := json.Marshal(reqPtr)
		if mErr != nil {
			err = mErr
			return
		}
		reqB = bytes.NewBuffer(b)

	}
	req, err := http.NewRequest(http.MethodGet, c.baseUrl+UrlWithParams(reqUrl, reqParams...), reqB)
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err = c._do(req)
	if err != nil {
		return
	}
	if respFunc != nil {
		ch := make(chan interface{})
		dec := json.NewDecoder(resp.Body)
		defer resp.Body.Close()

		go func() {
			for {
				if err := dec.Decode(respPtr); err != nil {
					ch <- err
					return
				}
				ch <- respPtr
			}
		}()

		for {
			select {
			case <-cancelCh:
				return
			case v := <-ch:
				if de, ok := v.(error); ok {
					err = de
					return
				}
				if err = respFunc(v); err != nil {
					return
				}
			}
		}
	}
	return
}

func (c *HttpClient) Get(reqUrl string, respPtr interface{}, reqParams ...*url.Values) (resp *http.Response, err error) {
	return c.Do(http.MethodGet, UrlWithParams(reqUrl, reqParams...), nil, respPtr)
}
func (c *HttpClient) Post(reqUrl string, respPtr interface{}) (resp *http.Response, err error) {
	return c.Do(http.MethodPost, reqUrl, nil, respPtr)
}
func (c *HttpClient) PostWithJson(reqUrl string, reqPtr interface{}, respPtr interface{}) (resp *http.Response, err error) {
	return c.Do(http.MethodPost, reqUrl, reqPtr, respPtr)
}

func (c *HttpClient) PostWithReader(reqUrl string, reqPtr interface{}, fieldName string, r io.Reader, respPtr interface{}) (resp *http.Response, err error) {
	buf := &bytes.Buffer{}
	mw := multipart.NewWriter(buf)
	if err = MultipartCopy(mw, fieldName, r); err != nil {
		return
	}
	if err = MultipartJson(mw, "json", reqPtr); err != nil {
		return
	}
	if err = mw.Close(); err != nil {
		return
	}
	req, err := http.NewRequest(http.MethodPost, c.baseUrl+reqUrl, buf)
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", mw.FormDataContentType())
	resp, err = c._do(req)
	if err != nil {
		return
	}
	err = readResponseBody(resp, respPtr)
	return
}

func (c *HttpClient) PostWithFile(reqUrl string, reqPtr interface{}, fieldName, fileName string, respPtr interface{}) (resp *http.Response, err error) {
	buf := &bytes.Buffer{}
	mw := multipart.NewWriter(buf)
	if err = MultipartFile(mw, fieldName, fileName); err != nil {
		return
	}
	if err = MultipartJson(mw, "json", reqPtr); err != nil {
		return
	}
	if err = mw.Close(); err != nil {
		return
	}
	req, err := http.NewRequest(http.MethodPost, c.baseUrl+reqUrl, buf)
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", mw.FormDataContentType())
	resp, err = c._do(req)
	if err != nil {
		return
	}
	err = readResponseBody(resp, respPtr)
	return
}
func (c *HttpClient) Delete(reqUrl string, respPtr interface{}) (resp *http.Response, err error) {
	return c.Do(http.MethodDelete, reqUrl, nil, respPtr)
}

func UrlWithParams(reqUrl string, reqParams ...*url.Values) string {
	reqUrlWithParams := reqUrl
	if len(reqParams) > 0 {
		reqUrlWithParams += "?"
		for i, p := range reqParams {
			if i != 0 {
				reqUrlWithParams += "&"
			}
			reqUrlWithParams += p.Encode()
		}
	}
	return reqUrlWithParams
}

func MultipartCopy(mw *multipart.Writer, fieldName string, r io.Reader) error {
	h := make(textproto.MIMEHeader)
	h.Set("Content-Disposition",
		fmt.Sprintf(`form-data; name="%s"; filename="blob"`, fieldName))
	h.Set("Content-Type", "application/zip")
	pw, err := mw.CreatePart(h)
	if err != nil {
		return err
	}
	if _, err = io.Copy(pw, r); err != nil {
		return err
	}
	return nil
}

func MultipartFile(mw *multipart.Writer, fieldName, fileName string) error {
	f, err := os.Open(fileName)
	if err != nil {
		return err
	}
	defer f.Close()

	pw, err := mw.CreateFormFile(fieldName, path.Base(fileName))
	if err != nil {
		return err
	}
	if _, err = io.Copy(pw, f); err != nil {
		return err
	}
	return nil
}
func MultipartJson(mw *multipart.Writer, fieldName string, v interface{}) error {
	h := make(textproto.MIMEHeader)
	h.Set("Content-Disposition",
		fmt.Sprintf(`form-data; name="%s"`, fieldName))
	h.Set("Content-Type", "application/json")
	pw, err := mw.CreatePart(h)
	if err != nil {
		return err
	}
	if err := json.NewEncoder(pw).Encode(v); err != nil {
		return err
	}
	return nil
}

type HttpError struct {
	status   int
	response []byte
	message  string
}

func (e *HttpError) Error() string {
	return e.message
}

func (e *HttpError) StatusCode() int {
	return e.status
}

func (e *HttpError) Response() []byte {
	return e.response
}

func NewHttpError(r *http.Response) *HttpError {
	hErr := &HttpError{
		status:   r.StatusCode,
		message:  "HTTP " + r.Status,
	}
	if rb, err := ioutil.ReadAll(r.Body); err == nil {
		hErr.response = rb
	} else {
		hErr.message += fmt.Sprintf("\nfail to read response body err:%+v", err)
	}
	return hErr
}

func EqualsSyscallErrno(err error, sen syscall.Errno) bool {
	if oe, ok := err.(*net.OpError); ok {
		if se, ok := oe.Err.(*os.SyscallError); ok {
			if en, ok := se.Err.(syscall.Errno); ok && en == sen {
				return true
			}
		}
	}
	return false
}

func HasContentType(h http.Header, mimetype string) bool {
	l := strings.Split(h.Get(echo.HeaderContentType), ",")
	for _, v := range l {
		if t, _, _ := mime.ParseMediaType(v); t == mimetype {
			return true
		}
	}
	return false
}

// is 'close by client' error
func IsBrokenPipeError(err error) bool {
	return EqualsSyscallErrno(err, syscall.EPIPE)
}

func IsConnectRefusedError(err error) bool {
	return EqualsSyscallErrno(err, syscall.ECONNREFUSED)
}

var (
	DefaultJsonTemplate = NewJsonTemplate("default")
)

type JsonTemplate struct {
	*template.Template
}

func NewJsonTemplate(name string) *JsonTemplate {
	tmpl := &JsonTemplate{template.New(name)}
	tmpl.Option("missingkey=error")
	tmpl.Funcs(template.FuncMap{
		"json": func(v interface{}) string {
			a, _ := json.Marshal(v)
			return string(a)
		},
	})
	return tmpl
}

func (t *JsonTemplate) Response(format string, v interface{}, resp *echo.Response) error {
	nt, err := t.Clone()
	if err != nil {
		return err
	}
	nt, err = nt.Parse(format)
	if err != nil {
		return err
	}
	err = nt.Execute(resp, v)
	if err != nil {
		return err
	}

	// resp.Header().Set(echo.HeaderContentType, echo.MIMEApplicationJSONCharsetUTF8)
	resp.Header().Set(echo.HeaderContentType, echo.MIMETextPlain)
	resp.WriteHeader(http.StatusOK)
	return nil
}

func NoneMiddlewareFunc(next echo.HandlerFunc) echo.HandlerFunc {
	return func(c echo.Context) error {
		return next(c)
	}
}

func Unauthorized(readOnly bool) echo.MiddlewareFunc {
	if readOnly {
		return func(next echo.HandlerFunc) echo.HandlerFunc {
			return func(ctx echo.Context) error {
				return ctx.String(http.StatusUnauthorized, "unauthorized")
			}
		}
	} else {
		return NoneMiddlewareFunc
	}
}

func WrapFunc(fs ...func()) echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(ctx echo.Context) error {
			for _, f := range fs {
				f()
			}
			return next(ctx)
		}
	}
}

func WrapHandler(hs ...http.Handler) echo.HandlerFunc {
	return func(ctx echo.Context) error {
		for _, h := range hs {
			h.ServeHTTP(ctx.Response(), ctx.Request())
		}
		return nil
	}
}
