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

package foundation.icon.btp.nativecoin;

import score.Address;
import score.annotation.EventLog;

import java.math.BigInteger;

public interface NCSEvents {

    /**
     * (EventLog) Sends a receipt to sender
     *
     * @param _from   The {@code _from} sender. (Indexed)
     * @param _to     The {@code _to} receiver.
     * @param _sn     The {@code _sn} sequence number of the service message.
     * @param _assets The {@code _assets} asset details that is the serialized data of AssetTransferDetail
     */
    @EventLog(indexed = 1)
    void TransferStart(Address _from, String _to, BigInteger _sn, byte[] _assets);

    /**
     * (EventLog) Sends a receipt to sender to notify the transfer's result
     *
     * @param _sender The {@code _sender} account sends the service message. (Indexed)
     * @param _sn     The {@code _sn} sequence number of the service message.
     * @param _code   The {@code _code} response code.
     * @param _msg    The {@code _msg} response message.
     */
    @EventLog(indexed = 1)
    void TransferEnd(Address _sender, BigInteger _sn, BigInteger _code, byte[] _msg);

    /**
     * Notify to the BSH owner that it has received unknown response
     *
     * @param _from The {@code _from} Network Address of source network.
     * @param _sn   The {@code _sn} sequence number of the service message.
     */
    @EventLog(indexed = 1)
    void UnknownResponse(String _from, BigInteger _sn);
}
