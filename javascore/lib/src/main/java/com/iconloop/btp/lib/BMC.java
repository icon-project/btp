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

package com.iconloop.btp.lib;

import score.Address;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public interface BMC {
    /**
     * Registers BMV for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _net  String (Network Address of the blockchain )
     * @param _addr Address (the address of BMV)
     */
    void addVerifier(String _net, Address _addr);

    /**
     * Unregisters BMV for the network.
     * May fail if it's referred by the link.
     * Called by the operator to manage the BTP network.
     *
     * @param _net String (Network Address of the blockchain )
     */
    void removeVerifier(String _net);

    /**
     * Get registered verifiers.
     *
     * @return A dictionary with the Network Address as a key and smart contract address of the BMV as a value.
     * <br>
     * For Example::
     * <br>
     * {
     * "0x1.iconee": "cx72eaed466599ca5ea377637c6fa2c5c0978537da"
     * }
     */
    Map getVerifiers();

    /**
     * Registers the smart contract for the service.
     * Called by the operator to manage the BTP network.
     *
     * @param _svc  String (the name of the service)
     * @param _addr Address (the address of the smart contract handling the service)
     */
    void addService(String _svc, Address _addr);

    /**
     * Unregisters the smart contract for the service.
     * Called by the operator to manage the BTP network.
     *
     * @param _svc String (the name of the service)
     */
    void removeService(String _svc);
    /**
     * Get registered services.
     *
     * @return A dictionary with the name of the service as key and address of the BSH related to the service as value.
     * <br>For example::<br>
     * {
     * "token": "cx72eaed466599ca5ea377637c6fa2c5c0978537da"
     * }
     */
    Map getServices();

    /**
     * If it generates the event related to the link, the relay shall handle the event to deliver BTP Message to the BMC.
     * If the link is already registered, or its network is already registered then it fails.
     * If there is no verifier related with the network of the link, then it fails.
     * Initializes status information for the link.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     */
    void addLink(String _link);

    /**
     * Removes the link and status information.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     */
    void removeLink(String _link);
    /**
     * Set properties of link for relay rotation
     * Called by the operator to manage the BTP network.
     *
     * @param _link           String (BTP Address of connected BMC)
     * @param _block_interval Integer (Interval of block creation, milliseconds)
     * @param _max_agg        Integer (Maximum aggregation of block update of a relay message)
     * @param _delay_limit    Integer (Maximum delay at BTP Event relay, block count)
     */
    void setLink(String _link, int _block_interval, int _max_agg, int _delay_limit);
    /**
     * Get status of BMC.
     * Used by the relay to resolve next BTP Message to send.
     * If target is not registered, it will fail.
     *
     * @param _link String ( BTP Address of the connected BMC )
     * @return The object contains followings fields.
     */
    BMCStatus getStatus(String _link);
    /**
     * Get registered links.
     *
     * @return A list of links ( BTP Addresses of the BMCs )
     * <br>For Example::<br>
     * [
     * "btp://0x1.iconee/cx9f8a75111fd611710702e76440ba9adaffef8656"
     * ]
     */
    List getLinks();

    /**
     * Add route to the BMC.
     * May fail if there more than one BMC for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _dst  String ( BTP Address of the destination BMC )
     * @param _link String ( BTP Address of the next BMC for the destination )
     */
    void addRoute(String _dst, String _link);
    /**
     * Remove route to the BMC.
     * Called by the operator to manage the BTP network.
     *
     * @param _dst String ( BTP Address of the destination BMC )
     */
    void removeRoute(String _dst);
    /**
     * Get routing information.
     *
     * @return A dictionary with the BTP Address of the destination BMC as key and the BTP Address of the next as value.
     *
     * <br>For Example::<br>
     * {
     * "btp://0x2.iconee/cx1d6e4decae8160386f4ecbfc7e97a1bc5f74d35b": "btp://0x1.iconee/cx9f8a75111fd611710702e76440ba9adaffef8656"
     * }
     */
    Map getRoutes();

    /**
     * Sends the message to a specific network.
     * Only allowed to be called by registered BSHs.
     *
     * @param _to  String ( Network Address of destination network )
     * @param _svc String ( name of the service )
     * @param _sn  Integer ( serial number of the message, must be positive )
     * @param _msg Bytes ( serialized bytes of Service Message )
     */
    void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg);

    /**
     * It verifies and decodes the Relay Message with BMV and dispatches BTP Messages to registered BSHs.
     * It's allowed to be called by registered Relay.
     *
     * @param _prev String ( BTP Address of the previous BMC )
     * @param _msg  String ( base64 encoded string of serialized bytes of Relay Message )
     */
    void handleRelayMessage(String _prev, String _msg);

    /**
     * TODO [TBD] add 'getBtpAddress' to IIP-25.BMC.Read-only methods
     * Returns BTP Address of BMC
     *
     * @return String (BTP Address of BMC)
     */
    String getBtpAddress();
}
