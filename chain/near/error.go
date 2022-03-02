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

package near

import (
	"fmt"
	"net/url"

	"github.com/icon-project/btp/chain"
	"github.com/icon-project/btp/common"
	"github.com/icon-project/btp/common/jsonrpc"
)

/*-------------------Constans---------------*/

var HandleError = []string{"TIMEOUT_ERROR",
	"INVALID_TRANSACTION", "UNKNOWN_TRANSACTION",
	"UNKNOWN_RECEIPT", "UNKNOWN_BLOCK", "NOT_SYNCED_YET",
	"UNKNOWN_CHUNK", "INVALID_SHARD_ID", "INVALID_ACCOUNT",
	"UNKNOWN_ACCOUNT", "UNKNOWN_ACCESS_KEY", "UNAVAILABLE_SHARD",
	"NO_SYNCED_BLOCKS", "NO_CONTRACT_CODE", "CONTRACT_EXECUTION_ERROR"}
var RequestValidationError = []string{"PARSE_ERROR"}
var InetrnalError = []string{"INTERNAL_ERROR"}

var NearJsonRpcErrors = map[string][]string{

	"HANDLER_ERROR":            HandleError,
	"REQUEST_VALIDATION_ERROR": RequestValidationError,
	"INTERNAL_ERROR":           InetrnalError,
}

/*----------------------functions-----------------*/

func (c *Client) MapError(err error) error {
	if err != nil {
		switch re := err.(type) {
		case *jsonrpc.Error:
			switch re.Name {
			case "HANDLER_ERROR":
				for _, value := range NearJsonRpcErrors["HANDLER_ERROR"] {

					if re.Cause.Name == value {
						return fmt.Errorf("%s , %#v ", value, re.Data)
					}

				}

			case "REQUEST_VALIDATION_ERROR":
				for _, value := range NearJsonRpcErrors["REQUEST_VALIDATION_ERROR"] {

					return fmt.Errorf(value)
				}

			case "INTERNAL_ERROR":
				for _, value := range NearJsonRpcErrors["INTERNAL_ERROR"] {

					return fmt.Errorf(value)
				}

			}

		case *common.HttpError:
			fmt.Printf("*common.HttpError:%+v", re)
			return chain.ErrConnectFail

		case *url.Error:
			if common.IsConnectRefusedError(re.Err) {
				return chain.ErrConnectFail
			}
		}
	}
	return err
}

func (c *Client) MapErrorWithTransactionResult(transactionResultPointer *chain.TransactionResult, err error) error {
	transactionResult := (*transactionResultPointer).(TransactionResult)
	emptyFailure := Failure{}

	err = c.MapError(err)

	if err != nil && transactionResult.Status.Failure != emptyFailure {
		//TODO : Handle error
		return c.MapError(err)
	}
	return err
}
