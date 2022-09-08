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

import java.math.BigInteger;

@ScoreClient
public interface FixedFees {
    /**
     * Gets the fixed fee for the given network address and type.
     * If there is no mapping to the network address, `default` fee is returned.
     *
     * @param _net The network address
     * @param _type The fee type ("relay" or "protocol")
     * @return The fee amount in loop
     */
    @External(readonly=true)
    BigInteger fixedFee(String _net, String _type);

    /**
     * Gets the total fixed fees for the given network address.
     * If there is no mapping to the network address, `default` fee is returned.
     *
     * @param _net The network address
     * @return The total fees amount in loop
     */
    @External(readonly=true)
    BigInteger totalFixedFees(String _net);

    /**
     * Sets the fixed fees for the given network address.
     * Only the admin wallet can invoke this.
     *
     * @param _net The destination network address
     * @param _relay The relay fee amount in loop
     * @param _protocol The protocol fee amount in loop
     */
    @External
    void setFixedFees(String _net, BigInteger _relay, BigInteger _protocol);

    /**
     * Gets the total accrued fees for the given type.
     *
     * @param _type The fee type ("relay" or "protocol")
     * @return The total accrued fees in loop
     */
    @External(readonly=true)
    BigInteger accruedFees(String _type);

    /**
     * Notifies the user that the fees have been successfully updated.
     *
     * @param _net The destination network address
     * @param _relay The relay fee amount in loop
     * @param _protocol The protocol fee amount in loop
     */
    @EventLog(indexed=1)
    void FixedFeesUpdated(String _net, BigInteger _relay, BigInteger _protocol);
}
