package iconee

import (
	"fmt"
	"github.com/icon-project/btp/cmd/btpsimple/module/base"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/jsonrpc"
	"net/url"
	"strconv"
)

const (
	JsonrpcErrorCodeSystem         jsonrpc.ErrorCode = -31000
	JsonrpcErrorCodeTxPoolOverflow jsonrpc.ErrorCode = -31001
	JsonrpcErrorCodePending        jsonrpc.ErrorCode = -31002
	JsonrpcErrorCodeExecuting      jsonrpc.ErrorCode = -31003
	JsonrpcErrorCodeNotFound       jsonrpc.ErrorCode = -31004
	JsonrpcErrorLackOfResource     jsonrpc.ErrorCode = -31005
	JsonrpcErrorCodeTimeout        jsonrpc.ErrorCode = -31006
	JsonrpcErrorCodeSystemTimeout  jsonrpc.ErrorCode = -31007
	JsonrpcErrorCodeScore          jsonrpc.ErrorCode = -30000
)

func (c *Client) MapError(err error) error {
	if err != nil {
		switch re := err.(type) {
		case *jsonrpc.Error:
			switch re.Code {
			case JsonrpcErrorCodeTxPoolOverflow:
				return base.ErrSendFailByOverflow
			case JsonrpcErrorCodeSystem:
				if subEc, err := strconv.ParseInt(re.Message[1:5], 0, 32); err == nil {
					switch subEc {
					case DuplicateTransactionError:
						return base.ErrSendDuplicateTransaction
					case ExpiredTransactionError:
						return base.ErrSendFailByExpired
					case FutureTransactionError:
						return base.ErrSendFailByFuture
					case TransactionPoolOverflowError:
						return base.ErrSendFailByOverflow
					}
				}
			case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
				return base.ErrGetResultFailByPending
			}
		case *common.HttpError:
			fmt.Printf("*common.HttpError:%+v", re)
			return base.ErrConnectFail
		case *url.Error:
			if common.IsConnectRefusedError(re.Err) {
				return base.ErrConnectFail
			}
		}
	}
	return err
}

func (c *Client) MapErrorWithTransactionResult(t *base.TransactionResult, err error) error {
	txr := (*t).(TransactionResult)
	err = c.MapError(err)
	if err == nil && &txr != nil && txr.Status != ResultStatusSuccess {
		fc, _ := txr.Failure.CodeValue.Value()
		if fc < ResultStatusFailureCodeRevert || fc > ResultStatusFailureCodeEnd {
			err = fmt.Errorf("failure with code:%s, message:%s",
				txr.Failure.CodeValue, txr.Failure.MessageValue)
		} else {
			err = base.NewRevertError(int(fc - ResultStatusFailureCodeRevert))
		}
	}
	return err
}

func (c *Client) CheckRPCPendingErrorCode(err *jsonrpc.ErrorCode) bool {
	switch *err {
		case JsonrpcErrorCodePending, JsonrpcErrorCodeExecuting:
			return true
	}
	return false
}
