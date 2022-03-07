package near

import (
	"bytes"
	"context"
	"crypto"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"
	"math/big"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/btcsuite/btcutil/base58"
	"github.com/gorilla/websocket"
	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/chain/base"
	account "github.com/icon-project/btp/chain/near/account"
	"github.com/icon-project/btp/common/codec"
	"github.com/icon-project/btp/common/jsonrpc"
	"github.com/icon-project/btp/common/log"
	w "github.com/icon-project/btp/common/wallet"
	"github.com/near/borsh-go"
	"github.com/reactivex/rxgo/v2"

	"github.com/icon-project/btp/chain/near/types"
)

const (
	transactionMaxDataSize                           = 524288
	functionCallMethod                               = "handle_relay_message"
	gas                                              = uint64(300000000000000)
	DefaultSendTransactionInterval                   = time.Second //same as parent variable
	ErrorMessagecode               jsonrpc.ErrorCode = -32000
	BmcContractMessageStateKey                       = "bWVzc2FnZQ=="
)

var balance = *big.NewInt(0)

type Api interface {
	broadcastTransaction(string) (string, error)
	broadcastTransactionAsync(string) (string, error)
	getBlockByHash(string) (types.Block, error)
	getBlockByHeight(int64) (types.Block, error)
	getBmcLinkStatus(accountId string, link *chain.BtpAddress) (types.BmcStatus, error)
	getBmvStatus(accountId string) (types.BmvStatus, error)
	getContractStateChange(height int64, accountId string, keyPrefix string) (types.ContractStateChange, error)
	getLatestBlockHash() (string, error)
	getLatestBlockHeight() (int64, error)
	getNextBlockProducers(*types.CryptoHash) (types.NextBlockProducers, error)
	getNonce(string, string) (int64, error)
	getReceiptProof(blockHash, receiptId *types.CryptoHash, receiverId string) (types.ReceiptProof, error)
	getTransactionResult(string, string) (types.TransactionResult, error)
}

/*--------------- Types------------*/
type Event struct {
	Next     chain.BtpAddress
	Sequence string
	Message  string
}

type Wallet interface {
	Address() string
	Sign(data []byte) ([]byte, error)
}

type Client struct {
	api Api
	*jsonrpc.Client
	connections     map[string]*websocket.Conn
	logger          log.Logger
	isMonitorClosed bool
}

//For FunctionCall
type Data struct {
	Source  string `json:"source"`
	Message string `json:"message"`
}

/*--------------- init function------------*/
func init() {
	client := Client{}
	base.RegisterClients([]string{"near"}, &client)
}

/*----------------public functions--------------*/
func (c *Client) Initialize(uri string, logger log.Logger) {
	transport := &http.Transport{MaxIdleConnsPerHost: 1000}
	httpClient := jsonrpc.NewJsonRpcClient(&http.Client{Transport: transport}, uri)
	c.api = &api{
		Client: httpClient,
		logger: logger,
	}

	c.Client = httpClient
	c.logger = logger
	c.isMonitorClosed = false
}

/*------------To get size of data in Json -------*/
func countBytesOfCompactJSON(jsonData interface{}) int {
	data, err := json.Marshal(jsonData)
	if err != nil {
		return transactionMaxDataSize
	}

	if len(data) == 0 {
		return transactionMaxDataSize
	}

	newBuffer := bytes.NewBuffer(nil)
	if err := json.Compact(newBuffer, data); err != nil {
		return transactionMaxDataSize
	}

	return newBuffer.Len()
}

/* --------------Public Functions-----------------*/

/*
purpose : To Close Monitor
Input   : nil
Output  : nil
*/

func (c *Client) CloseAllMonitor() {
	c.isMonitorClosed = true
}

/*
purpose : To Get Height
Input   : BlockNotification
Output  : BlockHeight & Error
*/

//Function to get height out of blockNotifcation
func (c *Client) GetBlockNotificationHeight(blockNotification *base.BlockNotification) (int64, error) {
	if block, Ok := (*blockNotification).(*types.Block); Ok {
		height := block.Height()
		return height, nil
	}

	return -1, fmt.Errorf("failed to cast parameter")
}

/*
purpose : To get Block
Input   : Height
Output  : Block & Error
*/

func (c *Client) GetBlockHeaderByHeight(height int64) (*base.BlockHeader, error) {
	var blockheader base.BlockHeader
	block, err := c.api.getBlockByHeight(height)
	if err != nil {
		return nil, err //TODO: Add Meaning Full Errors
	}

	headerBytes, err := block.RlpSerialize()
	if err != nil {
		return nil, err //TODO: Add Meaning Full Errors
	}

	blockheader.Hash = block.Hash()
	blockheader.Height = block.Height()
	blockheader.Serilaized = append(blockheader.Serilaized, headerBytes...)

	return &blockheader, nil
}

/*
purpose : to get block based on the offset/height
Input   : height/offset
Output  : observable
*/

//Function to get Block based on the height/offset
func (c *Client) GetBlock(offset int64) rxgo.Observable {
	return c.MonitorBlockHeight(offset).FlatMap(func(i rxgo.Item) rxgo.Observable {

		if i.E != nil {
			return rxgo.Just(
				errors.New(i.E.Error()),
			)()
		}

		newOffset := i.V.(int64)
		blockResponse, err := c.api.getBlockByHeight(newOffset)
		if err != nil {
			return rxgo.Just(
				errors.New(err.Error()),
			)()
		}

		// if blockResponse == nil {
		// 	e := fmt.Sprintf("DATA FOR THE PROVIDED BLCOK HEIGHT IS NOT AVAILABLE%v", newOffset)
		// 	return rxgo.Just(

		// 		errors.New(e),
		// 	)()
		// }

		block := &MonitorBlockData{}
		// TODO: Fix
		block.BlockHash = string(blockResponse.Header.Hash.Base58Encode())
		block.BlockId = newOffset

		return rxgo.Just(block)()
	}, rxgo.WithCPUPool())
}

func (c *Client) ComputeBlockHash(serialized []byte) ([]byte, error) {
	var header struct {
		PreviousBlockHash []byte
		InnerLite         types.HeaderInnerLite
		InnerRest         types.HeaderInnerRest
	}
	_, err := codec.RLP.UnmarshalFromBytes(serialized, &header)
	if err != nil {
		return nil, err
	}

	var block types.Block

	return block.ComputeHash(header.PreviousBlockHash, header.InnerLite, header.InnerRest)
}

/*
purpose : Create Transaction
Input   : wallet & TranactionParam
Output  : error
*/

func (c *Client) CreateTransaction(wallet base.Wallet, p *chain.TransactionParam) error {
	if nearWallet, Ok := (wallet).(*w.NearWallet); Ok {
		publicKey := account.PublicKeyToString(*nearWallet.Pkey)
		accountId := nearWallet.Address()
		blockHash, err := c.api.getLatestBlockHash()
		if err != nil {
			return err
		}

		blockHashDecoded := base58.Decode(blockHash)
		nonce, err := c.GetNonce(publicKey, accountId)

		if nonce == -1 || err != nil {
			return err
		}

		if transactionParam, ok := (*p).(*types.TransactionParam); ok {
			var err error
			relayMessage := &Data{
				Source:  transactionParam.RelayMessage.Previous,
				Message: transactionParam.RelayMessage.Message,
			}

			data, err := json.Marshal(relayMessage)
			if err != nil {
				return err
			}

			actions := []Action{
				{
					Enum: 2,
					FunctionCall: FunctionCall{
						MethodName: functionCallMethod,
						Args:       data,
						Gas:        gas,
						Deposit:    balance,
					},
				},
			}

			publicKey := PublicKeyFromEd25519(*nearWallet.Pkey)
			transaction := createTransaction(transactionParam.From, transactionParam.To, blockHashDecoded, nonce, actions, publicKey)
			serializedTransaction, err := borsh.Serialize(*transaction)
			if err != nil {
				return fmt.Errorf("failed to serialize transaction")
			}

			transacparm := new(Transacparm)
			transacparm.PrivateKey = *nearWallet.Skey
			transacparm.TransacHex = serializedTransaction
			newTransactionParam := chain.TransactionParam(transacparm)
			if err := c.SignTransaction(&newTransactionParam); err != nil {
				return err
			}
			signedTransaction, err := c.CreateSignatureTransaction(&newTransactionParam, transaction)
			if err != nil {
				return err
			}

			buffer, err := borsh.Serialize(*signedTransaction)
			if err != nil {
				panic(err)
			}

			base64Data := base64.StdEncoding.EncodeToString(buffer)
			transactionParam.Base64encodedData = base64Data

			return nil
		}
		return fmt.Errorf("failed to create transaction")
	}
	return fmt.Errorf("failed to create transaction")
}

/*
purpose : Create Sign Transaction
Input   : Transaction Parameters
Output  : error
*/

func (c *Client) SignTransaction(param *chain.TransactionParam) error {
	if transactionParam, ok := (*param).(*Transacparm); ok {
		preSigndata := sha256.Sum256(transactionParam.TransacHex)
		privateKey := transactionParam.PrivateKey

		sign, err := privateKey.Sign(rand.Reader, preSigndata[:], crypto.Hash(0))
		if err != nil {
			return fmt.Errorf("failed to sign transaction")
		}

		if len(sign) != 64 {
			return fmt.Errorf("sign error,length is not equal 64,length=%d", len(sign))
		}

		transactionParam.Signature = sign
		return nil
	}
	return fmt.Errorf("fail to cast TransactionParam %T", param)
}

/*
purpose : Create Signature Transaction
Input   : Transaction-Parameter & Transaction
Output  : SignatureTransaction & Error
*/

func (c *Client) CreateSignatureTransaction(param *chain.TransactionParam, transaction *Transaction) (*SignedTransaction, error) {
	var signature Signature
	if transactionParam, ok := (*param).(*Transacparm); ok {

		signature.KeyType = ED25519
		copy(signature.Data[:], transactionParam.Signature)

		var signedTransaction SignedTransaction
		signedTransaction.Transaction = *transaction
		signedTransaction.Signature = signature

		return &signedTransaction, nil
	}
	return nil, fmt.Errorf("failed to cast Parameters")
}

/*
purpose : Create Transaction
Input   : from , to (address) , blockhash , nonce , actions , publicKey
Output  : Transaction
*/

func createTransaction(from, to string, blockhash []byte, nonce int64, actions []Action, publickey PublicKey) *Transaction {
	var transaction Transaction

	transaction.SignerID = from
	transaction.ReceiverID = to
	transaction.PublicKey = publickey
	copy(transaction.BlockHash[:], blockhash)
	transaction.Nonce = uint64(nonce + 1)
	transaction.Actions = actions

	return &transaction
}

/*
purpose : Create BlockRequest
Input   : height
Output  : BlockRequest
*/

func (c *Client) GetBlockRequest(height int64) *base.BlockRequest {
	blockRequest := &BlockHeightParam{
		BlockID: height,
	}

	newBlockRequest := base.BlockRequest(blockRequest)
	return &newBlockRequest
}

/*
purpose : to get Transaction Parameters from segment
Input   : segment
Output  : transaction parameters
*/

func (c *Client) GetTransactionParams(segment *chain.Segment) (chain.TransactionParam, error) {
	if transactionParam, ok := segment.TransactionParam.(*types.TransactionParam); ok {
		return transactionParam, nil
	}

	return nil, fmt.Errorf("fail to cast TransactionParam %T", segment.TransactionParam)
}

/*
purpose : to SendTransaction and Wait for response
Input   : transaction Parameter
Output  : transaction result(bytes) & error
*/

//async transaction
func (c *Client) SendTransactionAndWait(param *chain.TransactionParam) ([]byte, error) {
	var result []byte
	if transaction, ok := (*param).(*types.TransactionParam); ok {
		var response string
		var err error

		for {
			response, err = c.api.broadcastTransactionAsync(transaction.Base64encodedData)

			if err != nil {
				if jsonError, ok := err.(*jsonrpc.Error); ok {
					switch jsonError.Message {
					case "Timeout":
						<-time.After(DefaultSendTransactionInterval)
						continue
					}
				}
			}
			break
		}

		result = []byte(response)
		return result, nil
	}
	return nil, fmt.Errorf("failed to cast transaction")
}

/*
purpose : to SendTransaction
Input   : transaction Parameter
Output  : transaction result(bytes) & error
*/

//commit transaction
func (c *Client) SendTransaction(param *chain.TransactionParam) ([]byte, error) {
	var result []byte
	if transaction, ok := (*param).(*types.TransactionParam); ok {
		var response string
		var err error

		response, err = c.api.broadcastTransaction(transaction.Base64encodedData)
		if err != nil {
			return nil, err
		}

		result = []byte(response)
		return result, nil
	}
	return nil, fmt.Errorf("failed to cast paramteres")
}

/*
purpose : to GetTransactionResult
Input   : getresultparam
Output  : transaction result(struct) & error
*/

func (c *Client) GetTransactionResult(param *chain.GetResultParam) (chain.TransactionResult, error) {
	if transactionHashParam, Ok := (*param).(*base.TransactionHashParam); Ok {
		if newResultParam, Ok := (*transactionHashParam).(GetResultParam); Ok {
			response, err := c.api.getTransactionResult(newResultParam.Txid, newResultParam.SenderId)
			if err != nil {
				return nil, err
			}

			return response, nil
		}
		return nil, fmt.Errorf("failed to cast paramteres")
	}
	return nil, fmt.Errorf("failed to cast paramteres")
}

/*
purpose : to get transaction result with receipts
Input   : getresultparam
Output  : transaction result(struct) & error
*/

// func (c *Client) GetTransactionResultWithRecipts(param *chain.GetResultParam) (chain.TransactionResult, error) {
// 	if transactionHashParam, Ok := (*param).(*base.TransactionHashParam); Ok {
// 		if newResultParam, Ok := (*transactionHashParam).(GetResultParam); Ok {
// 			response, err := c.api.getTransactionResultWithRecipts(newResultParam.Txid, newResultParam.SenderId)
// 			if err != nil {
// 				return nil, err
// 			}

// 			return response, nil
// 		}
// 		return nil, fmt.Errorf("failed to cast paramteres")
// 	}
// 	return nil, fmt.Errorf("failed to cast paramteres")
// }

/*---------------Near Specific----------------*/

/*
purpose : to get nonce
Input   : publickey(string) & accountid(string)
Output  : nonce(number) & error
*/

func (c *Client) GetNonce(publicKey string, accountId string) (int64, error) {
	var err error
	var publicKeyString string

	if !strings.HasPrefix(publicKey, "ed25519:") {
		var publicKeyBytes []byte
		if len(publicKey) == 64 {

			publicKeyBytes, err = hex.DecodeString(publicKey)
			if err != nil {
				return -1, err
			}

			publicKeyString = account.PublicKeyToString(publicKeyBytes)
		} else {

			publicKeyBytes = base58.Decode(publicKey)
			if len(publicKeyBytes) == 0 {
				return -1, fmt.Errorf("b58 decode public key error, %s", publicKey)
			}

			publicKeyString = "ed25519:" + publicKey
		}
	} else {
		publicKeyString = publicKey
	}

	nonce, err := c.api.getNonce(accountId, publicKeyString)
	if err != nil {
		return -1, err
	}
	return nonce, nil
}

/*
purpose : to get relaymethod parameters
Input   : transaction parameters
Output  : message , previousmessage & error
*/

func (c *Client) GetRelayMethodParams(param *chain.TransactionParam) (string, string, error) {
	if transactionParam, Ok := (*param).(*types.TransactionParam); Ok {
		relaymessage := transactionParam.RelayMessage

		return relaymessage.Message, relaymessage.Previous, nil
	}
	return "", "", fmt.Errorf("fail to cast TransactionParam %T", param)
}

/*
purpose : get smartcontract link status (bmc)
Input   : wallet, source & destination btp address
Output  : bmc link status
*/

func (c *Client) GetBMCLinkStatus(destination, source chain.BtpAddress) (*chain.BMCLinkStatus, error) {
	bmcStatus, err := c.api.getBmcLinkStatus(destination.Account(), &source)
	if err != nil {
		return nil, err
	}

	bmvStatus, err := c.api.getBmvStatus(string(bmcStatus.Verifier))
	if err != nil {
		return nil, err
	}

	linkstatus := &chain.BMCLinkStatus{}
	linkstatus.TxSeq = bmcStatus.TxSeq
	linkstatus.RxSeq = bmcStatus.RxSeq
	linkstatus.Verifier.Height = bmvStatus.Height
	linkstatus.Verifier.Offset = bmvStatus.Offset
	linkstatus.Verifier.LastHeight = bmvStatus.LastHeight
	linkstatus.BMRs = make([]struct {
		Address      string
		BlockCount   int64
		MessageCount int64
	}, len(bmcStatus.BMRs))

	for i, bmr := range bmcStatus.BMRs {
		linkstatus.BMRs[i].Address = string(bmr.Address)
		linkstatus.BMRs[i].BlockCount = bmr.BlockCount
		linkstatus.BMRs[i].MessageCount = bmr.MessageCount
	}

	linkstatus.BMRIndex = bmcStatus.BMRIndex
	linkstatus.RotateHeight = bmcStatus.RotateHeight
	linkstatus.RotateTerm = bmcStatus.RotateTerm
	linkstatus.DelayLimit = bmcStatus.DelayLimit
	linkstatus.MaxAggregation = bmcStatus.MaxAggregation
	linkstatus.CurrentHeight = bmcStatus.CurrentHeight
	linkstatus.RxHeight = bmcStatus.RxHeight
	linkstatus.RxHeightSrc = bmcStatus.RxHeightSrc
	linkstatus.BlockIntervalSrc = bmcStatus.BlockIntervalSrc
	linkstatus.BlockIntervalDst = bmcStatus.BlockIntervalDst

	return linkstatus, nil
}

/*
purpose : to check the transaction limit
Input   : size
Output  : bool
*/

func (c *Client) IsTransactionOverLimit(size int) bool {
	return transactionMaxDataSize < float64(size)
}

/*
purpose : get events from smartcontract changes
Input   : accountId (string) & blockId (int64)
Output  : slice of events , receipthash & error
*/

func (c *Client) getEvents(accountId string, blockId int64) ([]*chain.Event, []types.CryptoHash, error) {
	var events = make([]*chain.Event, 0)

	var receiptHash = make([]types.CryptoHash, 0)
	response, err := c.api.getContractStateChange(blockId, accountId, BmcContractMessageStateKey)
	if err != nil {
		return nil, nil, err
	}

	for _, r := range response.Changes {
		var event Event
		var eventsData string
		// TODO: Fix change to actual receipthash r.Cause.ReceiptHash
		receiptHash = append(receiptHash, r.Cause.ReceiptHash)

		eventDataBytes, err := base64.URLEncoding.Strict().DecodeString(r.Data.ValueBase64)
		if err != nil {
			return nil, nil, err
		}

		err = borsh.Deserialize(&eventsData, eventDataBytes)
		if err != nil {
			return nil, nil, err
		}

		err = json.Unmarshal([]byte(eventsData), &event)
		if err != nil {
			return nil, nil, err
		}

		sequence, err := strconv.ParseInt(event.Sequence, 10, 64)
		if err != nil {
			return nil, nil, err
		}

		eventData := &chain.Event{
			Next:     event.Next,
			Sequence: sequence,
			Message:  []byte(event.Message),
		}
		events = append(events, eventData)
	}
	return events, receiptHash, nil
}

/*
purpose : create transation param for relay
Input   : wallet , destination btp address, previous , realymessage, steplimit
Output  : transaction param
*/

func (c *Client) BMCRelayMethodTransactionParam(wallet base.Wallet, destination chain.BtpAddress, previous string, relayMessage *base.RelayMessageClient, stepLimit int64) (chain.TransactionParam, error) {

	if nearWallet, Ok := (wallet).(*w.NearWallet); Ok {
		bytes, err := codec.RLP.MarshalToBytes(relayMessage)
		if err != nil {
			return nil, err
		}

		relayMessageParam := types.RelayMessageParam{
			Previous: previous,
			Message:  base64.URLEncoding.EncodeToString(bytes),
		}
		log.Tracef("newTransactionParam RLPEncodedRelayMessage: %x\n", bytes)

		transactionParam := &types.TransactionParam{
			From:         nearWallet.Address(),
			To:           destination.Account(),
			RelayMessage: relayMessageParam,
		}

		return transactionParam, nil
	}
	return nil, fmt.Errorf("falied cast the parameters")
}

/*
purpose : compute block hash from the block notification
Input   : block notification
Output  : hash(byte) & error
*/

// func (c *Client) GetBlockNotificationHash(param *base.BlockNotification) ([]byte, error) {
// 	if lightClientBlock, oK := (*param).(*LightClientResponse); oK {
// 		blockhash, err := computeBlockHash(lightClientBlock)
// 		if err != nil {
// 			return nil, err
// 		}

// 		return blockhash, err
// 	}
// 	return nil, fmt.Errorf("falied to cast the parameter")
// }

/*
purpose : get block proof for block
Input   : block
Output  : RLP marshalled proof & error
*/

func (c *Client) GetBlockProof(block *base.BlockNotification) ([]byte, error) {
	var proof struct {
		BlockHeader              types.SerializableHeader
		NextBlockInnerHash       []byte
		ApprovalsAfterNext       []types.Signature
		ApprovalMessage          types.ApprovalMessage
		ApprovalMessageAfterNext types.ApprovalMessage
		NextBlockProducers       types.NextBlockProducers
	}

	currentBlock, ok := (*block).(*types.Block)
	if !ok {
		return nil, fmt.Errorf("falied to cast")
	}

	proof.BlockHeader = currentBlock.SerializableHeader()

	previousBlock, err := c.api.getBlockByHash(currentBlock.Header.PreviousBlockHash.Base58Encode())
	if err != nil {
		return nil, err //TODO: Add Meaning Full Errors
	}

	if previousBlock.Height()+1 == currentBlock.Height() {
		proof.ApprovalMessage = types.ApprovalMessage{
			Type:              []byte{0},
			PreviousBlockHash: previousBlock.Hash(),
		}
	} else {
		proof.ApprovalMessage = types.ApprovalMessage{
			Type:                []byte{1},
			PreviousBlockHeight: previousBlock.Height(),
		}
	}

	nextBlock, err := c.getNextBlock(currentBlock)
	if err != nil {
		return nil, err //TODO: Add Meaning Full Errors
	}

	proof.NextBlockInnerHash, err = nextBlock.ComputeInnerHash(nextBlock.InnerLite(), nextBlock.InnerRest())
	if err != nil {
		return nil, err //TODO: Add Meaning Full Errors
	}

	afterNextBlock, err := c.getNextBlock(&nextBlock)
	if err != nil {
		return nil, err //TODO: Add Meaning Full Errors
	}

	if nextBlock.Height()+1 == afterNextBlock.Height() {
		proof.ApprovalMessageAfterNext = types.ApprovalMessage{
			Type:              []byte{0},
			PreviousBlockHash: nextBlock.Hash(),
		}
	} else {
		proof.ApprovalMessageAfterNext = types.ApprovalMessage{
			Type:                []byte{1},
			PreviousBlockHeight: nextBlock.Height(),
		}
	}

	proof.ApprovalMessageAfterNext.TargetHeight = afterNextBlock.Height()

	proof.ApprovalsAfterNext = afterNextBlock.Header.Approvals

	if bytes.Equal(currentBlock.Header.EpochId, previousBlock.Header.EpochId) {
		nextBlockProducers, err := c.api.getNextBlockProducers(&currentBlock.Header.Hash)
		if err != nil {
			return nil, err
		}
		proof.NextBlockProducers = nextBlockProducers
	} else {
		proof.NextBlockProducers = nil
	}

	blockProof, err := codec.RLP.MarshalToBytes(proof)

	if err != nil {
		return nil, fmt.Errorf("falied to serialise")
	}

	return blockProof, nil
}

func (c *Client) getNextBlock(block *types.Block) (types.Block, error) {
	return c.api.getBlockByHeight(block.Height() + 1)
}

/*
purpose : to create new block request
Input   : source , destination(btpaddress) & height
Output  : block request
*/

func (c *Client) GetEventRequest(source chain.BtpAddress, destination chain.BtpAddress, height int64) *base.BlockRequest {
	blockRequest := new(BlockRequest)
	blockRequest.Height = height

	eventFilter := EventFilter{
		AccountId:   source.Account(),
		Message:     "bWVzc2FnZQ==",
		Destination: destination,
	}

	blockRequest.EventFilters = eventFilter
	newBlockRequest := base.BlockRequest(blockRequest)

	return &newBlockRequest
}

/*
purpose : to get receipt proofs for the events
Input   : block request
Output  : slice of receiptsproof
*/

func (c *Client) GetReceiptProofs(blockRequest *base.BlockRequest) ([]*chain.ReceiptProof, error) {
	receiptProofs := make([]*chain.ReceiptProof, 0)

	if data, Ok := (*blockRequest).(*BlockRequest); Ok {
		events, receiptHash, err := c.getEvents(data.EventFilters.AccountId, data.Height)
		if err != nil {
			return nil, err
		}

		if len(events) > 0 {
			receiptProof := &chain.ReceiptProof{
				Height: data.Height,
			}

			for index, event := range events {
				receiptProof.Events = append(receiptProof.Events, event)
				if len(receiptProof.Events) > 0 {
					blockParam := data.Height + 1

					block, err := c.api.getBlockByHeight(blockParam)
					if err != nil {
						return nil, err
					}

					changeProofResponse, err := c.api.getReceiptProof(&block.Header.Hash, &receiptHash[index], data.EventFilters.AccountId)
					if err != nil {
						return nil, err
					}

					proof, err := codec.RLP.MarshalToBytes(changeProofResponse)
					if err != nil {
						return nil, err
					}

					receiptProof.EventProofs = append(receiptProof.EventProofs, &chain.EventProof{
						Proof: proof,
					})
				}
			}
			receiptProofs = append(receiptProofs, receiptProof)
		}
		return receiptProofs, nil
	}
	return nil, fmt.Errorf("falied to cast parameter")
}

/*
purpose : to create result param from transaction hash and senderid
Input   : wallet , transactionhashparam , transationhash
Output  : error
*/

func (c *Client) AssignHash(wallet base.Wallet, param *base.TransactionHashParam, transactionHash []byte) error {
	if nearWallet, Ok := (wallet).(*w.NearWallet); Ok {
		data := GetResultParam{}
		data.Txid = string(transactionHash)
		data.SenderId = nearWallet.Address()
		transactionParam := base.TransactionHashParam(data)
		*param = transactionParam

		return nil
	}
	return fmt.Errorf("failed to get signer ID")
}

func (c *Client) UnmarshalFromSegment(string, *base.RelayMessageClient) error {

	return nil
}

////////----------------------------------------------------//////////

/*
purpose : to monitor the reciever
Input   : blockrequest , callback(function)
Output  : errror
*/

func (c *Client) MonitorReceiverBlock(blockRequest *base.BlockRequest, callback func(rxgo.Observable) error, singleCallBack func()) error {
	return callback(c.MonitorBlock(blockRequest, singleCallBack).TakeUntil(func(i interface{}) bool {
		return c.isMonitorClosed
	}))
}

/*
purpose : to monitor the sender
Input   : blockrequest , callback(function)
Output  : errror
*/

func (c *Client) MonitorSenderBlock(blockRequest *base.BlockRequest, callback func(rxgo.Observable) error, singleCallBack func()) error {
	return callback(c.MonitorBlockUpdates(blockRequest, singleCallBack).TakeUntil(func(i interface{}) bool {
		return c.isMonitorClosed
	}))
}

/*
purpose : to monitor the block based on height
Input   : height
Output  : obeservable
*/

func (c *Client) MonitorBlockHeight(offset int64) rxgo.Observable {
	channel := make(chan rxgo.Item)
	go func(offset int64) {
		defer close(channel)
		off := offset
		lastestBlockHeight, err := c.api.getLatestBlockHeight()
		if err != nil {
			// TODO: Handle Error
			channel <- rxgo.Error(err)
			return
		}

		if lastestBlockHeight == -1 {
			channel <- rxgo.Error(errors.New("invalid block height"))
			return
		}

		for {
			// r = 13 , lb = 40 off = 27
			rangeHeight := lastestBlockHeight - off

			// 3
			if rangeHeight < 5 {
				lastestBlockHeight, err = c.api.getLatestBlockHeight() //82
				if err != nil {
					// TODO: Handle Error
					fmt.Println(err)
				}

				rangeHeight = lastestBlockHeight - off // 3
				if rangeHeight < 3 {
					time.Sleep(time.Second * 2)
					continue
				}
			}

			channel <- rxgo.Of(off)
			off += 1
		}
	}(offset)

	return rxgo.FromChannel(channel)
}

/*
purpose : to monitor the block updates
Input   : blockrequest & callback
Output  : observable
*/

func (c *Client) MonitorBlockUpdates(blockRequestPointer *base.BlockRequest, singleCallBack func()) rxgo.Observable {
	blockRequest := (*blockRequestPointer).(*BlockHeightParam)
	singleCallBack() //TODO: Handle

	return c.GetBlock(blockRequest.BlockID).Map(func(_ context.Context, block interface{}) (interface{}, error) {
		if block != nil {
			result, err := c.api.getBlockByHeight(block.(*MonitorBlockData).BlockId)
			if err != nil {
				// TODO: Handle Error
				fmt.Println(err)
			}
			return &result, nil
		}
		return nil, nil
	}, rxgo.WithCPUPool())
}

/*
purpose : to monitor block
Input   : blockrequest & callback
Output  : observable
*/

func (c *Client) MonitorBlock(blockRequestPointer *base.BlockRequest, singleCallBack func()) rxgo.Observable {
	blockRequest := (*blockRequestPointer).(*BlockRequest)
	singleCallBack()

	return c.GetBlock(blockRequest.Height).Map(func(_ context.Context, block interface{}) (interface{}, error) {
		if block != nil {
			result, err := c.api.getBlockByHeight(block.(*MonitorBlockData).BlockId)
			if err != nil {
				// TODO: Handle Error
				fmt.Println(err)
			}
			return &result, nil
		}
		return nil, nil
	}, rxgo.WithCPUPool())
}

/*
purpose : to monitor the light client block
Input   : blockrequest & callback
Output  : observable
*/

// func (c *Client) MonitorLightClient(blockRequestPointer *base.BlockRequest, singleCallBack func()) rxgo.Observable {
// 	if blockRequest, Ok := (*blockRequestPointer).(*BlockRequest); Ok {
// 		singleCallBack() //TODO: Handle

// 		return c.GetBlock(blockRequest.Height).Map(func(_ context.Context, block interface{}) (interface{}, error) {
// 			if block != nil {
// 				result, err := c.api.getLightClientBlock(block.(*MonitorBlockData).BlockHash)
// 				if err != nil {
// 					// TODO: Handle Error
// 					fmt.Println(err)
// 				}
// 				return result, nil
// 			}
// 			return nil, nil
// 		}, rxgo.WithCPUPool())
// 	}
// 	return rxgo.Just(errors.New("failed to cast parameters"))()
// }

//TODO: For Verification
// func (c *Client) MonitorStateChange(offest int64) rxgo.Observable {

// 	return c.GetBlock(offest).Map(func(_ context.Context, block interface{}) (interface{}, error) {
// 		if block != nil {

// 			result, receipthash, err := c.GetEvents("", offest)
// 			if err != nil {
// 				fmt.Println(err)
// 			}

// 			receiptsProofRequest[offest+1] = receipthash
// 			return result, nil
// 		}
// 		return nil, nil
// 	}, rxgo.WithCPUPool())
// }
