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

import java.util.List;

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
    void handleFragment(String _prev, String _msg, int _idx);

    /**
     * TODO [TBD] add 'addRelay' to IIP-25.BMC.Writable methods
     * Registers relay for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     * @param _addr Address (the address of Relay)
     */
    void addRelay(String _link, Address _addr);

    /**
     * TODO [TBD] add 'removeRelay' to IIP-25.BMC.Writable methods
     * Unregisters relay for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     * @param _addr Address (the address of Relay)
     */
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
    List getRelays(String _link);

    /**
     * Registers candidate for the smart contract for the service.
     * Called by the BSH-Owner.
     *
     * @param _svc  String (the name of the service)
     * @param _addr Address (the address of the smart contract handling the service)
     */
    void addServiceCandidate(String _svc, Address _addr);

    /**
     * Unregisters candidate for the smart contract for the service.
     * Called by the operator to manage the BTP network.
     *
     * @param _svc  String (the name of the service)
     * @param _addr Address (the address of the smart contract handling the service)
     */
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
    List getServiceCandidates();

    long getFeeGatheringTerm();

    void setFeeGatheringTerm(long _value);

    Address getFeeAggregator();

    void setFeeAggregator(Address _addr);
}
