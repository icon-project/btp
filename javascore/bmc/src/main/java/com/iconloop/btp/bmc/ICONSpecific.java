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

package com.iconloop.btp.bmc;

import score.Address;
import score.annotation.External;

import java.math.BigInteger;

public interface ICONSpecific {

    /**
     * TODO [TBD] add 'handleFragment' to IIP BMC.Writable methods
     * Assemble fragments of the Relay Message and call {@link com.iconloop.btp.lib.BMC::handleRelayMessage}
     * It's allowed to be called by registered Relay.
     *
     * @param _prev String ( BTP Address of the previous BMC )
     * @param _msg  String ( Fragmented base64 encoded string of serialized bytes of Relay Message )
     * @param _idx  Integer ( Index of fragment )
     */
    @External
    void handleFragment(String _prev, String _msg, int _idx);

    /**
     * Set properties of link for term of relay rotation
     * Called by the operator to manage the BTP network.
     *
     * @param _link           String (BTP Address of connected BMC)
     * @param _block_interval Integer (Interval of block creation, milliseconds)
     * @param _max_agg        Integer (Maximum aggregation of block update of a relay message)
     */
    @External
    void setLinkRotateTerm(String _link, int _block_interval, int _max_agg);

    /**
     * Set properties of link for delay limitation of relay rotation
     * Called by the operator to manage the BTP network.
     *
     * @param _link     String (BTP Address of connected BMC)
     * @param _value    Integer (Maximum delay at BTP Event relay, block count)
     */
    @External
    void setLinkDelayLimit(String _link, int _value);

    /**
     * Set properties of link for term of sending SACK message
     * Called by the operator to manage the BTP network.
     *
     * @param _link     String (BTP Address of connected BMC)
     * @param _value    Integer (Term of sending SACK message, block count)
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
     * Get registered relays.
     *
     * @param _link String (BTP Address of connected BMC)
     * @return A list of relays.
     *
     * <br>For Example::<br>
     * [
     * "hx..."
     * ]
     */
    @External(readonly = true)
    Address[] getRelays(String _link);

    /**
     * Registers candidate for the smart contract for the service.
     * Called by the BSH-Owner.
     *
     * @param _svc  String (the name of the service)
     * @param _addr Address (the address of the smart contract handling the service)
     */
    @External
    void addServiceCandidate(String _svc, Address _addr);

    /**
     * Unregisters candidate for the smart contract for the service.
     * Called by the operator to manage the BTP network.
     *
     * @param _svc  String (the name of the service)
     * @param _addr Address (the address of the smart contract handling the service)
     */
    @External
    void removeServiceCandidate(String _svc, Address _addr);

    /**
     * Get registered service candidate
     *
     * @return A list of service candidates
     * <br>For Example::<br>
     * [
     * {
     * "svc":"the name of the service",
     * "address":"cx...",
     * "owner":"hx...",
     * }
     * ]
     */
    @External(readonly = true)
    ServiceCandidate[] getServiceCandidates();

    /**
     * Optional External method
     */
    @External
    void sendFeeGathering();

    @External(readonly = true)
    long getFeeGatheringTerm();

    @External
    void setFeeGatheringTerm(long _value);

    @External(readonly = true)
    Address getFeeAggregator();

    @External
    void setFeeAggregator(Address _addr);
}
