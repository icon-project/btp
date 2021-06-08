/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package iconee

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mpt"
	"github.com/mitchellh/mapstructure"
	"github.com/reactivex/rxgo/v2"
)

const (
	txMaxDataSize                              = 524288 //512 * 1024 // 512kB
	txOverheadScale                            = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	txSizeLimit                                = txMaxDataSize / (1 + txOverheadScale)
	DefaultGetRelayResultInterval              = time.Second
	DefaultSendTransactionRetryInterval        = 3 * time.Second         //3sec
	DefaultGetTransactionResultPollingInterval = 1500 * time.Millisecond //1.5sec
)

const (
	EventSignature      = "Message(str,int,bytes)"
	EventIndexSignature = 0
	EventIndexNext      = 1
	EventIndexSequence  = 2
)

type Client struct {
	api ApiInterface
	*jsonrpc.Client
	conns map[string]*websocket.Conn
	l     log.Logger
}

var txSerializeExcludes = map[string]bool{"signature": true}

func (c *Client) SignTransaction(w base.Wallet, p *base.TransactionParam) error {
	if tp, ok := (*p).(*TransactionParam); ok {
		tp.Timestamp = NewHexInt(time.Now().UnixNano() / int64(time.Microsecond))
		js, err := json.Marshal(tp)
		fmt.Println(string(js))
		if err != nil {
			return err
		}

		bs, err := SerializeJSON(js, nil, txSerializeExcludes)
		if err != nil {
			return err
		}

		bs = append([]byte("icx_sendTransaction."), bs...)
		txHash := crypto.SHA3Sum256(bs)
		tp.TxHash = NewHexBytes(txHash)

		sig, err := w.Sign(txHash)
		if err != nil {
			return err
		}

		tp.Signature = base64.StdEncoding.EncodeToString(sig)
		return nil

	} else {
		return fmt.Errorf("fail to casting TransactionParam %T", p)
	}
}

func (c *Client) GetBlockByHeight(p *BlockHeightParam) (*Block, error) {
	return c.api.getBlockByHeight(p)
}

func (c *Client) GetBlockHeaderByHeight(h int64, b *base.BlockHeader) ([]byte, error) {
	params := &BlockHeightParam{
		Height: NewHexInt(h),
	}

	header, err := c.api.getBlockHeaderByHeight(params)
	if err != nil {
		return nil, err
	}

	_, err = codec.RLP.UnmarshalFromBytes(header, b)
	if err != nil {
		return nil, err
	}

	return header, nil
}

func (c *Client) GetBlockNotificationHash(bn *base.BlockNotification) ([]byte, error) {
	if bn, ok := (*bn).(*BlockNotification); ok {
		hash, err := bn.Hash.Value()
		return hash, err
	} else {
		return nil, fmt.Errorf("fail to casting BlockNotification %T", bn)
	}
}

func (c *Client) GetBlockProof(bh *base.BlockHeader) ([]byte, error) {
	var proof struct {
		BlockHeader []byte
		Votes       []byte
		Validators  []byte
	}

	votesParams := &BlockHeightParam{Height: NewHexInt(bh.Height)}
	votes, err := c.api.getVotesByHeight(votesParams)
	if err != nil {
		return nil, err
	}

	nextValidatorsParams := &DataHashParam{Hash: NewHexBytes(bh.NextValidatorsHash)}
	nextValidators, err := c.api.getDataByHash(nextValidatorsParams)
	if err != nil {
		return nil, err
	}

	proof.BlockHeader = bh.Serialized
	proof.Votes = votes
	proof.Validators = nextValidators

	return codec.RLP.MarshalToBytes(&proof)
}

func (c *Client) GetVotesByHeight(p *BlockHeightParam) ([]byte, error) {
	return c.api.getVotesByHeight(p)
}

func (c *Client) GetDataByHash(p *DataHashParam) ([]byte, error) {
	return c.api.getDataByHash(p)
}

func (c *Client) GetProofForResult(p *ProofResultParam) ([][]byte, error) {
	return c.api.getProofForResult(p)
}

func (c *Client) GetProofForEvents(p *ProofEventsParam) ([][][]byte, error) {
	return c.api.getProofForEvents(p)
}

func (c *Client) GetEventRequest(addr base.BtpAddress, s string, height int64) *base.BlockRequest {
	ef := &EventFilter{
		Addr:      Address(addr.ContractAddress()),
		Signature: EventSignature,
		Indexed:   []*string{&s},
	}

	eventRequest := BlockRequest{
		Height:       NewHexInt(height),
		EventFilters: []*EventFilter{ef},
	}
	var v base.BlockRequest
	mapstructure.Decode(eventRequest, &v)

	return &v
}

func (c *Client) GetTransactionResult(p *base.GetResultParam) (*base.TransactionResult, error) {
	if thp, ok := (*p).(*TransactionHashParam); ok {
		txr, err := c.api.getTransactionResult(thp)

		if err != nil {
			return nil, err
		}

		var _txr base.TransactionResult
		mapstructure.Decode(txr, &_txr)

		return &_txr, nil
	} else {
		return nil, fmt.Errorf("fail to casting TransactionHashParam %T", p)
	}
}

func (c *Client) GetTransactionParams(segment *base.Segment) (base.TransactionParam, error) {
	if thp, ok := segment.TransactionParam.(*TransactionParam); ok {
		return thp, nil
	} else {
		return nil, fmt.Errorf("fail to casting TransactionParam %T", segment.TransactionParam)
	}
}

func (c *Client) GetRelayMethodParams(p *base.TransactionParam) (string, string, error) {
	if txp, ok := (*p).(*TransactionParam); ok {
		cd := txp.Data.(CallData)
		rmp := cd.Params.(BMCRelayMethodParams)
		return rmp.Messages, rmp.Prev, nil
	} else {
		return "", "", fmt.Errorf("fail to casting TransactionParam %T", p)
	}
}

func (c *Client) UnmarshalFromSegment(msg string, rm *base.RelayMessageClient) error {
	b, err := base64.URLEncoding.DecodeString(msg)
	if _, err = codec.RLP.UnmarshalFromBytes(b, rm); err != nil {
		return err
	}
	return nil
}

func (c *Client) AttachHash(p *base.TransactionHashParam, b []byte) error {
	if thp, ok := (*p).(*TransactionHashParam); ok {
		thp.Hash = HexBytes(b)
		// mapstructure.Decode(thp, &p)
	} else {
		return fmt.Errorf("fail to casting TransactionHashParam %T", thp)
	}
	return nil
}

func (c *Client) SendTransaction(p *base.TransactionParam) ([]byte, error) {
	if txh, ok := (*p).(*TransactionParam); ok {
		txr, err := c.api.sendTransaction(txh)
		if err != nil {
			return nil, c.MapError(err)
		}
		return txr, nil
	} else {
		return nil, fmt.Errorf("fail to casting TransactionParam %T", p)
	}
}

func (c *Client) SendTransactionAndWait(p *TransactionParam) (*HexBytes, error) {
	var result HexBytes
	if _, err := c.Do("icx_sendTransactionAndWait", p, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (c *Client) CloseAllMonitor() {
	c.api.closeAllMonitor()
}

func (c *Client) Initialize(uri string, l log.Logger) {
	tr := &http.Transport{MaxIdleConnsPerHost: 1000}
	httpClient := jsonrpc.NewJsonRpcClient(&http.Client{Transport: tr}, uri)
	opts := IconOptions{}
	opts.SetBool(IconOptionsDebug, true)
	c.api = &api{
		Client: httpClient,
		conns:  make(map[string]*websocket.Conn),
		logger: l,
	}
	c.api.(*api).CustomHeader[HeaderKeyIconOptions] = opts.ToHeaderValue()
	c.conns = c.api.(*api).conns
	c.Client = httpClient
	c.l = l
}

func (c *Client) GetBlockRequest(height int64) *base.BlockRequest {
	b := BlockRequest{
		Height: NewHexInt(height),
	}

	var v base.BlockRequest
	mapstructure.Decode(b, &v)

	return &v
}

func (c *Client) GetBlockNotificationHeight(bn *base.BlockNotification) (int64, error) {
	height, err := (*bn).(*BlockNotification).Height.Value()
	if err != nil {
		return 0, c.MapError(err)
	}
	return height, nil
}

func (c *Client) GetBMCLinkStatus(w base.Wallet, dst, src base.BtpAddress) (*base.BMCLinkStatus, error) {
	p := &CallParam{
		FromAddress: Address(w.Address()),
		ToAddress:   Address(dst.ContractAddress()),
		DataType:    "call",
		Data: CallData{
			Method: BMCGetStatusMethod,
			Params: BMCStatusParams{
				Target: src.String(),
			},
		},
	}
	bs := &BMCStatusResponse{}
	if err := c.api.call(p, bs); err != nil {
		return nil, err
	}
	ls := &base.BMCLinkStatus{}
	ls.TxSeq, _ = bs.TxSeq.Value()
	ls.RxSeq, _ = bs.RxSeq.Value()
	ls.Verifier.Height, _ = bs.Verifier.Height.Value()
	ls.Verifier.Offset, _ = bs.Verifier.Offset.Value()
	ls.Verifier.LastHeight, _ = bs.Verifier.LastHeight.Value()
	ls.BMRs = make([]struct {
		Address      string
		BlockCount   int64
		MessageCount int64
	}, len(bs.BMRs))
	for i, bmr := range bs.BMRs {
		ls.BMRs[i].Address = string(bmr.Address)
		ls.BMRs[i].BlockCount, _ = bmr.BlockCount.Value()
		ls.BMRs[i].MessageCount, _ = bmr.MessageCount.Value()
	}
	ls.BMRIndex, _ = bs.BMRIndex.Int()
	ls.RotateHeight, _ = bs.RotateHeight.Value()
	ls.RotateTerm, _ = bs.RotateTerm.Int()
	ls.DelayLimit, _ = bs.DelayLimit.Int()
	ls.MaxAggregation, _ = bs.MaxAggregation.Int()
	ls.CurrentHeight, _ = bs.CurrentHeight.Value()
	ls.RxHeight, _ = bs.RxHeight.Value()
	ls.RxHeightSrc, _ = bs.RxHeightSrc.Value()
	return ls, nil
}

func (c *Client) BMCRelayMethodTransactionParam(w base.Wallet, dst, src base.BtpAddress, prev string, rm *base.RelayMessageClient, stepLimit int64) (base.TransactionParam, error) {
	b, err := codec.RLP.MarshalToBytes(rm)
	if err != nil {
		return nil, err
	}
	rmp := BMCRelayMethodParams{
		Prev:     prev,
		Messages: base64.URLEncoding.EncodeToString(b),
	}
	p := &TransactionParam{
		Version:     NewHexInt(JsonrpcApiVersion),
		FromAddress: Address(w.Address()),
		ToAddress:   Address(dst.ContractAddress()),
		NetworkID:   HexInt(dst.NetworkID()),
		StepLimit:   NewHexInt(stepLimit),
		DataType:    "call",
		Data: CallData{
			Method: BMCRelayMethod,
			Params: rmp,
		},
	}
	return p, nil
}

func (c *Client) IsTransactionOverLimit(size int) bool {
	return txSizeLimit < float64(size)
}

func init() {
	client := Client{}
	base.RegisterClients([]string{"icon"}, &client)
}

func (c *Client) MonitorBlock(b *base.BlockRequest, scb func()) rxgo.Observable {
	ch := make(chan rxgo.Item)
	resp := &BlockNotification{}
	br := (*b).(BlockRequest)
	go func() error {
		defer close(ch)
		return c.api.monitor("/block", br, resp, func(conn *websocket.Conn, v interface{}) {
			switch t := v.(type) {
			case *BlockNotification:
				var bn base.BlockNotification = t
				ch <- rxgo.Of(&bn)
			case WSEvent:
				c.l.Debugf("MonitorBlock WSEvent %s %+v", conn.LocalAddr().String(), t)
				switch t {
				case WSEventInit:
					if scb != nil {
						scb()
					}
				}
			case error:
				fmt.Errorf("not supported type %T", t)
			default:
				fmt.Errorf("not supported type %T", t)
			}
		})
	}()
	observable := rxgo.FromChannel(ch)
	return observable
}

func (c *Client) GetReceiptProofs(bn *base.BlockNotification, isFoundOffsetBySequence bool, ef base.EventLogFilter) ([]*base.ReceiptProof, bool, error) {
	receiptProofs := make([]*base.ReceiptProof, 0)
	blockNotification := (*bn).(*BlockNotification)
	nextEventProof := 0
	eventLogFilter := (ef).(EventLogFilter)
	if len(blockNotification.Indexes) > 0 {
		l := blockNotification.Indexes[0]
	RpLoop:
		for i, index := range l {
			p := &ProofEventsParam{BlockHash: blockNotification.Hash, Index: index, Events: blockNotification.Events[0][i]}
			proofs, err := c.api.getProofForEvents(p)
			if err != nil {
				return nil, isFoundOffsetBySequence, c.MapError(err)
			}

			if !isFoundOffsetBySequence {
			EpLoop:
				for j := 0; j < len(blockNotification.Events); j++ {
					if eventLog, err := toEventLog(proofs[j+1]); err != nil {
						return nil, isFoundOffsetBySequence, err
					} else if bytes.Equal(eventLog.Addr, eventLogFilter.addr) &&
						bytes.Equal(eventLog.Indexed[EventIndexSignature], eventLogFilter.signature) &&
						bytes.Equal(eventLog.Indexed[EventIndexNext], eventLogFilter.next) &&
						bytes.Equal(eventLog.Indexed[EventIndexSequence], eventLogFilter.seq) {

						isFoundOffsetBySequence = true
						c.l.Debugln("onCatchUp found offset sequence", j, blockNotification)
						if (j + 1) < len(p.Events) {
							nextEventProof = j + 1
							break EpLoop
						}
					}
				}

				if nextEventProof == 0 {
					continue RpLoop
				}
			}

			idx, _ := index.Value()
			receiptProof := &base.ReceiptProof{
				Index:       int(idx),
				EventProofs: make([]*base.EventProof, 0),
			}

			if receiptProof.Proof, err = codec.RLP.MarshalToBytes(proofs[0]); err != nil {
				return nil, isFoundOffsetBySequence, err
			}

			for k := nextEventProof; k < len(p.Events); k++ {
				eventIdx, _ := p.Events[k].Value()
				eventProof := &base.EventProof{
					Index: int(eventIdx),
				}

				if eventProof.Proof, err = codec.RLP.MarshalToBytes(proofs[k+1]); err != nil {
					return nil, isFoundOffsetBySequence, err
				}

				var event *base.Event
				if event, err = toEvent(proofs[k+1], ef); err != nil {
					return nil, isFoundOffsetBySequence, err
				}
				receiptProof.Events = append(receiptProof.Events, event)
				receiptProof.EventProofs = append(receiptProof.EventProofs, eventProof)
			}

			receiptProofs = append(receiptProofs, receiptProof)
			nextEventProof = 0
		}
	}

	return receiptProofs, isFoundOffsetBySequence, nil
}

func toEventLog(proof [][]byte) (*EventLog, error) {
	mptProof, err := mpt.NewMptProof(proof)
	if err != nil {
		return nil, err
	}

	eventLog := &EventLog{}
	if _, err := codec.RLP.UnmarshalFromBytes(mptProof.Leaf().Data, eventLog); err != nil {
		return nil, fmt.Errorf("fail to parse EventLog on leaf err:%+v", err)
	}

	return eventLog, nil
}

func toEvent(proof [][]byte, ef base.EventLogFilter) (*base.Event, error) {
	eventLogFilter := (ef).(EventLogFilter)
	el, err := toEventLog(proof)
	if err != nil {
		return nil, err
	}

	if bytes.Equal(el.Addr, eventLogFilter.addr) &&
		bytes.Equal(el.Indexed[EventIndexSignature], eventLogFilter.signature) &&
		bytes.Equal(el.Indexed[EventIndexNext], eventLogFilter.next) {
		var i common.HexInt
		i.SetBytes(el.Indexed[EventIndexSequence])
		event := &base.Event{
			Next:     base.BtpAddress(el.Indexed[EventIndexNext]),
			Sequence: i.Int64(),
			Message:  el.Data[0],
		}
		return event, nil
	}

	return nil, fmt.Errorf("invalid event")
}

func (c *Client) MonitorReceiverBlock(br *base.BlockRequest, cb func(rxgo.Observable) error, scb func()) error {
	return cb(c.MonitorBlock(br, scb))
}

func (c *Client) MonitorSenderBlock(br *base.BlockRequest, cb func(rxgo.Observable) error, scb func()) error {
	return cb(c.MonitorBlock(br, scb))
}
