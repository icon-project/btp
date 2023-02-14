/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.xcall;

import foundation.icon.score.client.ScoreClient;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;

@ScoreClient
public interface CallService {
    /**
     * The name of CallService.
     */
    String NAME = "xcall";

    /*======== At the source CALL_BSH ========*/
    /**
     * Sends a call message to the contract on the destination chain.
     *
     * @param _to The BTP address of the callee on the destination chain
     * @param _data The calldata specific to the target contract
     * @param _rollback (Optional) The data for restoring the caller state when an error occurred
     * @return The serial number of the request
     */
    @Payable
    @External
    BigInteger sendCallMessage(String _to, byte[] _data, @Optional byte[] _rollback);

    /**
     * Notifies that a response message has arrived for the `_sn` if the request was a two-way message.
     *
     * @param _sn The serial number of the previous request
     * @param _code The response code
     *              (0: Success, -1: Unknown generic failure, >=1: User defined error code)
     * @param _msg The result message if any
     */
    @EventLog(indexed=1)
    void ResponseMessage(BigInteger _sn, int _code, String _msg);

    /**
     * Notifies the user that a rollback operation is required for the request '_sn'.
     *
     * @param _sn The serial number of the previous request
     */
    @EventLog(indexed=1)
    void RollbackMessage(BigInteger _sn);

    /**
     * Rollbacks the caller state of the request '_sn'.
     *
     * @param _sn The serial number of the previous request
     */
    @External
    void executeRollback(BigInteger _sn);

    /**
     * Notifies that the rollback has been executed.
     *
     * @param _sn The serial number for the rollback
     * @param _code The execution result code
     *              (0: Success, -1: Unknown generic failure, >=1: User defined error code)
     * @param _msg The result message if any
     */
    @EventLog(indexed=1)
    void RollbackExecuted(BigInteger _sn, int _code, String _msg);

    /*======== At the destination CALL_BSH ========*/
    /**
     * Notifies the user that a new call message has arrived.
     *
     * @param _from The BTP address of the caller on the source chain
     * @param _to A string representation of the callee address
     * @param _sn The serial number of the request from the source
     * @param _reqId The request id of the destination chain
     * @param _data The calldata
     */
    @EventLog(indexed=3)
    void CallMessage(String _from, String _to, BigInteger _sn, BigInteger _reqId, byte[] _data);

    /**
     * Executes the requested call message.
     *
     * @param _reqId The request id
     */
    @External
    void executeCall(BigInteger _reqId);

    /**
     * Notifies that the call message has been executed.
     *
     * @param _reqId The request id for the call message
     * @param _code The execution result code
     *              (0: Success, -1: Unknown generic failure, >=1: User defined error code)
     * @param _msg The result message if any
     */
    @EventLog(indexed=1)
    void CallExecuted(BigInteger _reqId, int _code, String _msg);
}
