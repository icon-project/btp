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
	"github.com/mitchellh/mapstructure"
	"github.com/reactivex/rxgo/v2"

	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/crypto"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
	"github.com/icon-project/btp/common/mpt"
)

/*-------------------constanst----------------*/

const (
	transactionMaxDataSize                     = 524288 //512 * 1024 // 512kB
	transactionOverheadScale                   = 0.37   //base64 encoding overhead 0.36, rlp and other fields 0.01
	transactionSizeLimit                       = transactionMaxDataSize / (1 + transactionOverheadScale)
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

/*----------------type-------------------------*/

type Client struct {
	api ApiInterface
	*jsonrpc.Client
	connections map[string]*websocket.Conn
	logger      log.Logger
}

var transactionSerializeExcludes = map[string]bool{"signature": true}

/*--------------- init function------------*/
func init() {
	client := Client{}
	base.RegisterClients([]string{"icon"}, &client)
}

/*----------------public functions--------------*/
func (c *Client) Initialize(uri string, logger log.Logger) {
	transport := &http.Transport{MaxIdleConnsPerHost: 1000}
	httpClient := jsonrpc.NewJsonRpcClient(&http.Client{Transport: transport}, uri)
	options := IconOptions{}
	options.SetBool(IconOptionsDebug, true)

	c.api = &api{
		Client: httpClient,
		conns:  make(map[string]*websocket.Conn),
		logger: logger,
	}

	c.api.(*api).CustomHeader[HeaderKeyIconOptions] = options.ToHeaderValue()
	c.connections = c.api.(*api).conns
	c.Client = httpClient
	c.logger = logger
}

func (c *Client) SignTransaction(wallet base.Wallet, param *base.TransactionParam) error {
	if transactionParam, ok := (*param).(*TransactionParam); ok {
		transactionParam.Timestamp = NewHexInt(time.Now().UnixNano() / int64(time.Microsecond))
		js, err := json.Marshal(transactionParam)
		fmt.Println(string(js))

		if err != nil {
			return err
		}

		bs, err := SerializeJSON(js, nil, transactionSerializeExcludes)
		if err != nil {
			return err
		}

		bs = append([]byte("icx_sendTransaction."), bs...)
		transactionHash := crypto.SHA3Sum256(bs)
		transactionParam.TxHash = NewHexBytes(transactionHash)

		signature, err := wallet.Sign(transactionHash)
		if err != nil {
			return err
		}

		transactionParam.Signature = base64.StdEncoding.EncodeToString(signature)
		return nil

	} else {
		return fmt.Errorf("fail to casting TransactionParam %T", transactionParam)
	}
}

func (c *Client) GetBlockByHeight(blockHeightParam *BlockHeightParam) (*Block, error) {
	return c.api.getBlockByHeight(blockHeightParam)
}

func (c *Client) GetBlockHeaderByHeight(height int64, blockHeader *base.BlockHeader) ([]byte, error) {
	params := &BlockHeightParam{
		Height: NewHexInt(height),
	}

	header, err := c.api.getBlockHeaderByHeight(params)
	if err != nil {
		return nil, err
	}

	_, err = codec.RLP.UnmarshalFromBytes(header, blockHeader)
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

func (c *Client) GetBlockProof(blockHeader *base.BlockHeader) ([]byte, error) {
	var proof struct {
		BlockHeader []byte
		Votes       []byte
		Validators  []byte
	}

	votesParams := &BlockHeightParam{Height: NewHexInt(blockHeader.Height)}
	votes, err := c.api.getVotesByHeight(votesParams)
	if err != nil {
		return nil, err
	}

	nextValidatorsParams := &DataHashParam{Hash: NewHexBytes(blockHeader.NextValidatorsHash)}
	nextValidators, err := c.api.getDataByHash(nextValidatorsParams)
	if err != nil {
		return nil, err
	}

	proof.BlockHeader = blockHeader.Serialized
	proof.Votes = votes
	proof.Validators = nextValidators

	return codec.RLP.MarshalToBytes(&proof)
}

func (c *Client) GetVotesByHeight(param *BlockHeightParam) ([]byte, error) {
	return c.api.getVotesByHeight(param)
}

func (c *Client) GetDataByHash(param *DataHashParam) ([]byte, error) {
	return c.api.getDataByHash(param)
}

func (c *Client) GetProofForResult(param *ProofResultParam) ([][]byte, error) {
	return c.api.getProofForResult(param)
}

func (c *Client) GetProofForEvents(param *ProofEventsParam) ([][][]byte, error) {
	return c.api.getProofForEvents(param)
}

func (c *Client) GetEventRequest(source base.BtpAddress, destination string, height int64) *base.BlockRequest {
	eventFilter := &EventFilter{
		Addr:      Address(source.ContractAddress()),
		Signature: EventSignature,
		Indexed:   []*string{&destination},
	}

	eventRequest := BlockRequest{
		Height:       NewHexInt(height),
		EventFilters: []*EventFilter{eventFilter},
	}

	//TODO : need to verify this part
	var v base.BlockRequest
	mapstructure.Decode(eventRequest, &v) //TODO : Do we need this part ?

	return &v
}

func (c *Client) GetTransactionResult(param *base.GetResultParam) (*base.TransactionResult, error) {
	if transactionHashParam, ok := (*param).(*TransactionHashParam); ok {
		transactionResult, err := c.api.getTransactionResult(transactionHashParam)

		if err != nil {
			return nil, err
		}

		var _transactionResult base.TransactionResult
		mapstructure.Decode(transactionResult, &_transactionResult)

		return &_transactionResult, nil
	} else {
		return nil, fmt.Errorf("fail to casting TransactionHashParam %T", param)
	}
}

func (c *Client) GetTransactionParams(segment *base.Segment) (base.TransactionParam, error) {
	if transactionParam, ok := segment.TransactionParam.(*TransactionParam); ok {
		return transactionParam, nil
	} else {
		return nil, fmt.Errorf("fail to casting TransactionParam %T", segment.TransactionParam)
	}
}

func (c *Client) GetRelayMethodParams(param *base.TransactionParam) (string, string, error) {
	if transactionParam, ok := (*param).(*TransactionParam); ok {
		callData := transactionParam.Data.(CallData)
		relayMethodParams := callData.Params.(BMCRelayMethodParams)
		return relayMethodParams.Messages, relayMethodParams.Prev, nil
	} else {
		return "", "", fmt.Errorf("fail to casting TransactionParam %T", param)
	}
}

func (c *Client) UnmarshalFromSegment(message string, relayMessage *base.RelayMessageClient) error {
	b, err := base64.URLEncoding.DecodeString(message)
	if _, err = codec.RLP.UnmarshalFromBytes(b, relayMessage); err != nil {
		return err
	}

	return nil
}

func (c *Client) AssignHash(param *base.TransactionHashParam, hashData []byte) error {
	if thp, ok := (*param).(*TransactionHashParam); ok {
		thp.Hash = HexBytes(hashData)
		// mapstructure.Decode(thp, &p) //TODO : Need to verify the this part code
	} else {
		return fmt.Errorf("fail to casting TransactionHashParam %T", thp)
	}

	return nil
}

func (c *Client) SendTransaction(param *base.TransactionParam) ([]byte, error) {
	if transactionParam, ok := (*param).(*TransactionParam); ok {
		transactionResult, err := c.api.sendTransaction(transactionParam)

		if err != nil {
			return nil, c.MapError(err)
		}
		return transactionResult, nil
	} else {
		return nil, fmt.Errorf("fail to casting TransactionParam %T", param)
	}
}

func (c *Client) SendTransactionAndWait(param *TransactionParam) (*HexBytes, error) {
	var result HexBytes
	if _, err := c.Do("icx_sendTransactionAndWait", param, &result); err != nil {
		return nil, err
	}

	return &result, nil
}

func (c *Client) CloseAllMonitor() {
	c.api.closeAllMonitor()
}

func (c *Client) GetBlockRequest(height int64) *base.BlockRequest {
	blockRequest := BlockRequest{
		Height: NewHexInt(height),
	}

	var v base.BlockRequest
	mapstructure.Decode(blockRequest, &v) //TODO : Need to investigate the code

	return &v
}

func (c *Client) GetBlockNotificationHeight(bn *base.BlockNotification) (int64, error) {
	height, err := (*bn).(*BlockNotification).Height.Value()
	if err != nil {
		return 0, c.MapError(err)
	}
	return height, nil
}

func (c *Client) GetBMCLinkStatus(wallet base.Wallet, destination, source base.BtpAddress) (*base.BMCLinkStatus, error) {
	callParam := &CallParam{
		FromAddress: Address(wallet.Address()),
		ToAddress:   Address(destination.ContractAddress()),
		DataType:    "call",

		Data: CallData{
			Method: BMCGetStatusMethod,
			Params: BMCStatusParams{
				Target: source.String(),
			},
		},
	}

	bmcStatus := &BMCStatusResponse{}
	if err := c.api.call(callParam, bmcStatus); err != nil {
		return nil, err
	}

	linkStatus := &base.BMCLinkStatus{}
	linkStatus.TxSeq, _ = bmcStatus.TxSeq.Value()
	linkStatus.RxSeq, _ = bmcStatus.RxSeq.Value()
	linkStatus.Verifier.Height, _ = bmcStatus.Verifier.Height.Value()
	linkStatus.Verifier.Offset, _ = bmcStatus.Verifier.Offset.Value()
	linkStatus.Verifier.LastHeight, _ = bmcStatus.Verifier.LastHeight.Value()
	linkStatus.BMRs = make([]struct {
		Address      string
		BlockCount   int64
		MessageCount int64
	}, len(bmcStatus.BMRs))

	for i, bmr := range bmcStatus.BMRs {
		linkStatus.BMRs[i].Address = string(bmr.Address)
		linkStatus.BMRs[i].BlockCount, _ = bmr.BlockCount.Value()
		linkStatus.BMRs[i].MessageCount, _ = bmr.MessageCount.Value()
	}

	linkStatus.BMRIndex, _ = bmcStatus.BMRIndex.Int()
	linkStatus.RotateHeight, _ = bmcStatus.RotateHeight.Value()
	linkStatus.RotateTerm, _ = bmcStatus.RotateTerm.Int()
	linkStatus.DelayLimit, _ = bmcStatus.DelayLimit.Int()
	linkStatus.MaxAggregation, _ = bmcStatus.MaxAggregation.Int()
	linkStatus.CurrentHeight, _ = bmcStatus.CurrentHeight.Value()
	linkStatus.RxHeight, _ = bmcStatus.RxHeight.Value()
	linkStatus.RxHeightSrc, _ = bmcStatus.RxHeightSrc.Value()

	return linkStatus, nil
}

func (c *Client) BMCRelayMethodTransactionParam(wallet base.Wallet, destination, source base.BtpAddress, previous string, relayMessage *base.RelayMessageClient, stepLimit int64) (base.TransactionParam, error) {
	b, err := codec.RLP.MarshalToBytes(relayMessage)
	if err != nil {
		return nil, err
	}

	relayMethodParams := BMCRelayMethodParams{
		Prev:     previous,
		Messages: base64.URLEncoding.EncodeToString(b),
	}

	transactionParam := &TransactionParam{
		Version:     NewHexInt(JsonrpcApiVersion),
		FromAddress: Address(wallet.Address()),
		ToAddress:   Address(destination.ContractAddress()),
		NetworkID:   HexInt(destination.NetworkID()),
		StepLimit:   NewHexInt(stepLimit),
		DataType:    "call",

		Data: CallData{
			Method: BMCRelayMethod,
			Params: relayMethodParams,
		},
	}

	return transactionParam, nil
}

func (c *Client) IsTransactionOverLimit(size int) bool {
	return transactionSizeLimit < float64(size)
}

func (c *Client) MonitorBlock(blockRequestPointer *base.BlockRequest, scb func()) rxgo.Observable {
	channel := make(chan rxgo.Item)
	response := &BlockNotification{}
	blockRequest := (*blockRequestPointer).(BlockRequest)

	go func() error {
		defer close(channel)
		return c.api.monitor("/block", blockRequest, response, func(conn *websocket.Conn, v interface{}) {
			switch t := v.(type) {
			case *BlockNotification:
				var bn base.BlockNotification = t
				channel <- rxgo.Of(&bn)

			case WSEvent:
				c.logger.Debugf("MonitorBlock WSEvent %s %+v", conn.LocalAddr().String(), t)
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

	observable := rxgo.FromChannel(channel)
	return observable
}

func (c *Client) GetReceiptProofs(bn *base.BlockNotification, isFoundOffsetBySequence bool, eventLogFil base.EventLogFilter) ([]*base.ReceiptProof, bool, error) {
	receiptProofs := make([]*base.ReceiptProof, 0)
	blockNotification := (*bn).(*BlockNotification)
	nextEventProof := 0
	eventLogFilter := (eventLogFil).(EventLogFilter)

	if len(blockNotification.Indexes) > 0 {
		l := blockNotification.Indexes[0]

	RpLoop:
		for i, index := range l {
			proofEventsParam := &ProofEventsParam{BlockHash: blockNotification.Hash, Index: index, Events: blockNotification.Events[0][i]}
			proofs, err := c.api.getProofForEvents(proofEventsParam)
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
						c.logger.Debugln("onCatchUp found offset sequence", j, blockNotification)
						if (j + 1) < len(proofEventsParam.Events) {
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

			for k := nextEventProof; k < len(proofEventsParam.Events); k++ {
				eventIdx, _ := proofEventsParam.Events[k].Value()
				eventProof := &base.EventProof{
					Index: int(eventIdx),
				}

				if eventProof.Proof, err = codec.RLP.MarshalToBytes(proofs[k+1]); err != nil {
					return nil, isFoundOffsetBySequence, err
				}

				var event *base.Event
				if event, err = toEvent(proofs[k+1], eventLogFil); err != nil {
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

func toEvent(proof [][]byte, eventLogFil base.EventLogFilter) (*base.Event, error) {
	eventLogFilter := (eventLogFil).(EventLogFilter)
	eventLog, err := toEventLog(proof)
	if err != nil {
		return nil, err
	}

	if bytes.Equal(eventLog.Addr, eventLogFilter.addr) &&
		bytes.Equal(eventLog.Indexed[EventIndexSignature], eventLogFilter.signature) &&
		bytes.Equal(eventLog.Indexed[EventIndexNext], eventLogFilter.next) {
		var i common.HexInt
		i.SetBytes(eventLog.Indexed[EventIndexSequence])

		event := &base.Event{
			Next:     base.BtpAddress(eventLog.Indexed[EventIndexNext]),
			Sequence: i.Int64(),
			Message:  eventLog.Data[0],
		}

		return event, nil
	}

	return nil, fmt.Errorf("invalid event")
}

func (c *Client) MonitorReceiverBlock(blockRequest *base.BlockRequest, callback func(rxgo.Observable) error, scb func()) error {
	return callback(c.MonitorBlock(blockRequest, scb))
}

func (c *Client) MonitorSenderBlock(br *base.BlockRequest, callback func(rxgo.Observable) error, scb func()) error {
	return callback(c.MonitorBlock(br, scb))
}
