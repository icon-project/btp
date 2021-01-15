package jsonrpc

import (
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/labstack/echo/v4"

	"github.com/icon-project/btp/common"
)

type Client struct {
	hc           *http.Client
	Endpoint     string
	CustomHeader map[string]string
	Pre func(req *http.Request) error
}

func NewJsonRpcClient(hc *http.Client, endpoint string) *Client {
	return &Client{hc: hc, Endpoint: endpoint, CustomHeader: make(map[string]string)}
}

func (c *Client) _do(req *http.Request) (resp *http.Response, err error) {
	if c.Pre != nil {
		if err = c.Pre(req); err != nil {
			return nil, err
		}
	}
	resp, err = c.hc.Do(req)
	if err != nil {
		return
	}
	if resp.StatusCode != http.StatusOK {
		err = common.NewHttpError(resp)
		//err = fmt.Errorf("http-status(%s) is not StatusOK", resp.Status)
	}
	return
}

//Supported Parameter Structures only 'by-name through an Object'
//refer https://www.jsonrpc.org/specification#parameter_structures
func (c *Client) Do(method string, reqPtr, respPtr interface{}) (jrResp *Response, err error) {
	jrReq := &Request{
		ID:      time.Now().UnixNano() / int64(time.Millisecond),
		Version: Version,
		Method:  method,
	}
	if reqPtr != nil {
		b, mErr := json.Marshal(reqPtr)
		if mErr != nil {
			err = mErr
			return
		}
		jrReq.Params = json.RawMessage(b)
	}
	reqB, err := json.Marshal(jrReq)
	if err != nil {
		return nil, err
	}
	req, err := http.NewRequest("POST", c.Endpoint, bytes.NewReader(reqB))
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	for k, v := range c.CustomHeader {
		req.Header.Set(k, v)
	}

	var resp *http.Response
	resp, err = c._do(req)
	if err != nil {
		if hErr, ok := err.(*common.HttpError); ok && len(hErr.Response()) > 0{
			if resp != nil && common.HasContentType(resp.Header, echo.MIMEApplicationJSON) {
				if dErr := json.Unmarshal(hErr.Response(), &jrResp); dErr != nil {
					err = fmt.Errorf("fail to decode response body err:%+v, httpErr:%+v, httpResp:%+v, responseBody:%s",
						dErr, err, resp, string(hErr.Response()))
				} else {
					err = jrResp.Error
					//fmt.Printf("jrResp.Error:%+v in HttpError", err)
				}
			}
			return
		}
		return
	}

	if jrResp, err = decodeResponseBody(resp); err != nil {
		err = fmt.Errorf("fail to decode response body err:%+v, jsonrpcResp:%+v",
			err, resp)
		return
	}
	if jrResp.Error != nil {
		err = jrResp.Error
		return
	}
	if respPtr != nil {
		rb, mErr := json.Marshal(jrResp.Result)
		if mErr != nil {
			err = mErr
			return
		}
		err = json.Unmarshal(rb, respPtr)
		if err != nil {
			return
		}
	}
	return
}

func (c *Client) Raw(reqB []byte) (resp *http.Response, err error) {
	req, err := http.NewRequest("POST", c.Endpoint, bytes.NewReader(reqB))
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Accept", "application/json")
	for k, v := range c.CustomHeader {
		req.Header.Set(k, v)
	}

	return c._do(req)
}

func decodeResponseBody(resp *http.Response) (jrResp *Response, err error) {
	defer resp.Body.Close()
	err = json.NewDecoder(resp.Body).Decode(&jrResp)
	return
}
