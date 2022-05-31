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

package foundation.icon.btp.arbcall;

import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public interface CallService {

    /*======== At the source CALL_BSH ========*/
    /**
     * Sends a call message to the contract on the destination chain.
     * Only allowed to be called from the contract.
     *
     * @param _to The BTP address of the callee on the destination chain
     * @param _data The calldata specific to the target contract
     * @param _rollback (Optional) The data for restoring the caller state when an error occurred
     * @return The serial number of the request
     */
    @External
    BigInteger sendCallMessage(String _to, byte[] _data, @Optional byte[] _rollback);

    /**
     * Notifies the user that a rollback operation is required for the request '_sn'.
     *
     * @param _sn The serial number of the previous request
     * @param _rollback The data for recovering that was given by the caller
     */
    @EventLog(indexed=1)
    void RollbackMessage(BigInteger _sn, byte[] _rollback);

    /**
     * Rollbacks the caller state of the request '_sn'.
     *
     * @param _sn The serial number of the previous request
     */
    @External
    void executeRollback(BigInteger _sn);

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
     * Executes the requested call.
     *
     * @param _reqId The request Id
     */
    @External
    void executeCall(BigInteger _reqId);
}
