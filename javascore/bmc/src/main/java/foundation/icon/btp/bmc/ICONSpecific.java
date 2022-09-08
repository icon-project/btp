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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BMC;
import foundation.icon.btp.lib.BMRStatus;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public interface ICONSpecific {

    /**
     * TODO [TBD] add 'handleFragment' to IIP BMC.Writable methods
     * Assemble fragments of the Relay Message and call {@link BMC ::handleRelayMessage}
     * It's allowed to be called by registered Relay.
     *
     * @param _prev String ( BTP Address of the previous BMC )
     * @param _msg  String ( Fragmented base64 encoded string of serialized bytes of Relay Message )
     * @param _idx  Integer ( Index of fragment )
     */
    @External
    void handleFragment(String _prev, String _msg, int _idx);

    /**
     * Set properties of link for term of sending SACK message
     * Called by the operator to manage the BTP network.
     *
     * @param _link  String (BTP Address of connected BMC)
     * @param _value Integer (Term of sending SACK message, block count)
     */
    @External
    void setLinkSackTerm(String _link, int _value);

    /**
     * TODO [TBD] add 'addRelay' to IIP-25.BMC.Writable methods
     * Registers relay for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     * @param _addr Address (the address of Relay)
     */
    @External
    void addRelay(String _link, Address _addr);

    /**
     * TODO [TBD] add 'removeRelay' to IIP-25.BMC.Writable methods
     * Unregisters relay for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     * @param _addr Address (the address of Relay)
     */
    @External
    void removeRelay(String _link, Address _addr);

    /**
     * TODO [TBD] add 'getRelays' to IIP-25.BMC.Read-only methods
     * Get status of registered relays by link..
     *
     * @param _link String (BTP Address of connected BMC)
     * @return The object contains followings fields.
     */
    @External(readonly = true)
    BMRStatus[] getRelays(String _link);

    /**
     * Drop the next message that to be relayed from a specific network
     * Called by the operator to manage the BTP network.
     *
     * @param _src String ( BTP Address of source BMC )
     * @param _seq  Integer ( number of the message from connected BMC )
     * @param _svc  String ( number of the message from connected BMC )
     * @param _sn   Integer ( serial number of the message, must be positive )
     */
    @External
    void dropMessage(String _src, BigInteger _seq, String _svc, BigInteger _sn);

    /**
     * (EventLog) Drop the message of the connected BMC
     * if sn of message is less than zero
     *
     * @param _link String ( BTP Address of connected BMC )
     * @param _seq  Integer ( sequence number of the message from connected BMC )
     * @param _msg  Bytes ( serialized bytes of BTP Message )
     */
    @EventLog(indexed = 2)
    void MessageDropped(String _link, BigInteger _seq, byte[] _msg);

    /**
     * Registers the BTPLink to connect that use the BTP-Block instead of Event-Log to send message.
     * Called by the operator to manage the BTP network.
     *
     * @param _link      String ( BTP Address of BMC to connect )
     * @param _networkId Integer ( To use networkId parameter of ChainSCORE API )
     */
    @External
    void addBTPLink(String _link, long _networkId);

    /**
     * Sets to migrate the Link to the BTPLink which use the BTP-Block to send message.
     * Called by the operator to manage the BTP network.
     *
     * @param _link      String ( BTP Address of connected BMC )
     * @param _networkId Integer (To use networkId parameter of ChainSCORE API)
     */
    @External
    void setBTPLinkNetworkId(String _link, long _networkId);

    /**
     * Get network id of the BTPLink
     *
     * @param _link String ( BTP Address of connected BMC )
     * @return Integer network id of the BTPLink
     */
    @External(readonly = true)
    long getBTPLinkNetworkId(String _link);

    @External(readonly = true)
    long getBTPLinkOffset(String _link);
}
