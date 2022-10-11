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

package foundation.icon.btp.lib;

import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.client.ScoreInterface;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

@ScoreInterface
@ScoreClient
public interface BMC {
    /**
     * Registers BMV for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _net  String (Network Address of the blockchain )
     * @param _addr Address (the address of BMV)
     */
    @External
    void addVerifier(String _net, Address _addr);

    /**
     * Unregisters BMV for the network.
     * May fail if it's referred by the link.
     * Called by the operator to manage the BTP network.
     *
     * @param _net String (Network Address of the blockchain )
     */
    @External
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
    @External(readonly = true)
    Map getVerifiers();

    /**
     * Registers the smart contract for the service.
     * Called by the operator to manage the BTP network.
     *
     * @param _svc  String (the name of the service)
     * @param _addr Address (the address of the smart contract handling the service)
     */
    @External
    void addService(String _svc, Address _addr);

    /**
     * Unregisters the smart contract for the service.
     * Called by the operator to manage the BTP network.
     *
     * @param _svc String (the name of the service)
     */
    @External
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
    @External(readonly = true)
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
    @External
    void addLink(String _link);

    /**
     * Removes the link and status information.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     */
    @External
    void removeLink(String _link);

    /**
     * Get status of BMC.
     * Used by the relay to resolve next BTP Message to send.
     * If target is not registered, it will fail.
     *
     * @param _link String ( BTP Address of the connected BMC )
     * @return The object contains followings fields.
     */
    @External(readonly = true)
    Map getStatus(String _link);

    /**
     * Get registered links.
     *
     * @return A list of links ( BTP Addresses of the BMCs )
     * <br>For Example::<br>
     * [
     * "btp://0x1.iconee/cx9f8a75111fd611710702e76440ba9adaffef8656"
     * ]
     */
    @External(readonly = true)
    String[] getLinks();

    /**
     * Add route to the BMC.
     * May fail if there more than one BMC for the network.
     * Called by the operator to manage the BTP network.
     *
     * @param _dst  String ( BTP Address of the destination BMC )
     * @param _link String ( BTP Address of the next BMC for the destination )
     */
    @External
    void addRoute(String _dst, String _link);

    /**
     * Remove route to the BMC.
     * Called by the operator to manage the BTP network.
     *
     * @param _dst String ( BTP Address of the destination BMC )
     */
    @External
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
    @External(readonly = true)
    Map getRoutes();

    /**
     * Sends the message to a specific network.
     * Only allowed to be called by registered BSHs.
     * _sn : positive for two-way message, zero for one-way message, negative for response
     *
     * @param _to  String ( Network Address of destination network )
     * @param _svc String ( name of the service )
     * @param _sn  Integer ( serial number of the message )
     * @param _msg Bytes ( serialized bytes of Service Message )
     * @return Integer ( network serial number of message )
     */
    @Payable
    @External
    BigInteger sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg);

    /**
     * It verifies and decodes the Relay Message with BMV and dispatches BTP Messages to registered BSHs.
     * It's allowed to be called by registered Relay.
     *
     * @param _prev String ( BTP Address of the previous BMC )
     * @param _msg  String ( base64 encoded string of serialized bytes of Relay Message )
     */
    @External
    void handleRelayMessage(String _prev, String _msg);

    /**
     * TODO [TBD] add 'getBtpAddress' to IIP-25.BMC.Read-only methods
     * Returns BTP Address of BMC
     *
     * @return String (BTP Address of BMC)
     */
    @External(readonly = true)
    String getBtpAddress();

    /**
     * Sets fee table
     * Called by the operator to manage the BTP network.
     *
     * @param _dst   String[] ( List of BTP Network Address of the destination BMC )
     * @param _value Integer[][] ( List of lists of relay fees in the path including return path.
     *               If it provides an empty relay fee list, then it removes the entry from the table. )
     */
    @External
    void setFeeTable(String[] _dst, BigInteger[][] _value);

    /**
     * Get fee to the target network
     * _response should be true if it uses positive value for _sn of {@link #sendMessage}.
     * If _to is not reachable, then it reverts.
     * If _to does not exist in the fee table, then it returns zero.
     *
     * @param _to       String ( BTP Network Address of the destination BMC )
     * @param _response Boolean ( Whether the responding fee is included )
     * @return Integer (The fee of sending a message to a given destination network )
     */
    @External(readonly = true)
    BigInteger getFee(String _to, boolean _response);

    /**
     * Get fee table
     * It reverts if the one of destination networks is not reachable.
     * If there is no corresponding fee table, then it returns an empty list.
     *
     * @param _dst String[] ( List of BTP Network Address of the destination BMC )
     * @return Integer[][] ( List of lists of relay fees in the path including return path )
     */
    @External(readonly = true)
    BigInteger[][] getFeeTable(String[] _dst);

    /**
     * Sends the claim message to a given network if a claimable reward exists.
     * It expects a response, so it would use a positive serial number for the message.
     * If _network is the current network then it transfers a reward and a sender pays nothing.
     * If the <sender> is FeeHandler, then it transfers the remaining reward to the receiver.
     *
     * @param _network  String ( Network address to claim )
     * @param _receiver String ( Address of the receiver of target chain )
     */
    @Payable
    @External
    void claimReward(String _network, String _receiver);

    /**
     * FIXME
     * @param _network
     * @param _receiver
     * @param _amount
     * @param _sn
     * @param _nsn
     */
    @EventLog
    void ClaimReward(String _network, String _receiver, BigInteger _amount, BigInteger _sn, BigInteger _nsn);

    /**
     * It returns the amount of claimable reward to the target
     *
     * @param _network String ( Network address to claim )
     * @param _addr    Address ( Address of the relay )
     * @return Integer (The claimable reward to the target )
     */
    @External(readonly = true)
    BigInteger getReward(String _network, Address _addr);

    /**
     * Sets the address to handle the remaining reward fee.
     * @param _addr Address ( the address to handle the remaining reward fee )
     */
    @External
    void setFeeHandler(Address _addr);

    /**
     * Gets the address to handle the remaining reward fee.
     *
     * @return Address ( the address to handle the remaining reward fee )
     */
    @External(readonly = true)
    Address getFeeHandler();
}
