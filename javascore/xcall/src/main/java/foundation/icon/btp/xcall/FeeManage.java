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
import score.Address;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

@ScoreClient
public interface FeeManage {
    /**
     * Sets the address of FeeHandler.
     * If _addr is null (default), it accrues protocol fees.
     * If _addr is a valid address, it transfers accrued fees to the address and
     * will also transfer the receiving fees hereafter.
     *
     * @param _addr The address of FeeHandler
     * @implNote Only the admin wallet can invoke this.
     */
    @External
    void setProtocolFeeHandler(@Optional Address _addr);

    /**
     * Gets the current protocol fee handler address.
     *
     * @return The protocol fee handler address
     */
    @External(readonly=true)
    Address getProtocolFeeHandler();

    /**
     * Sets the protocol fee amount.
     *
     * @param _value The protocol fee amount in loop
     * @implNote Only the admin wallet can invoke this.
     */
    @External
    void setProtocolFee(BigInteger _value);

    /**
     * Gets the current protocol fee amount.
     *
     * @return The protocol fee amount in loop
     */
    @External(readonly=true)
    BigInteger getProtocolFee();

    /**
     * Gets the fee for delivering a message to the _net.
     * If the sender is going to provide rollback data, the _rollback param should set as true.
     * The returned fee is the sum of the protocol fee and the relay fee.
     *
     * @param _net The network address
     * @param _rollback Indicates whether it provides rollback data
     * @return the sum of the protocol fee and the relay fee
     */
    @External(readonly=true)
    BigInteger getFee(String _net, boolean _rollback);
}
