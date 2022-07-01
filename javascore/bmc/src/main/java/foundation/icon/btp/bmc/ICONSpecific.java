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
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

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

    /**
     * (Payable) Registers the Relayer for the network.
     *
     * @param _desc String (description of Relayer)
     */
    @Payable
    @External
    void registerRelayer(String _desc);

    /**
     * Unregisters the Relayer for the network.
     * <p>
     * _addr Address (the address of Relayer)
     */
    @External
    void unregisterRelayer();

    @External
    void removeRelayer(Address _addr, Address _refund);

    /**
     * Get registered the Relayers.
     *
     * @return A dictionary with the address of the Relayer as key and information of the Relayer as value.
     *
     * <br>For Example::<br>
     * {
     * "hx..." : {
     * "description": "description of the Relayer...",
     * "bond": "0x10"
     * }
     * }
     */
    @External(readonly = true)
    Map<String, Relayer> getRelayers();

    /**
     * Optional External method
     */
    @External
    void distributeRelayerReward();

    /**
     * Claim reward of the Relayer
     * Called by relayer
     *
     */
    @External
    void claimRelayerReward();

    /**
     * Set minimum bond of the Relayer
     *
     * @param _value Integer
     */
    @External
    void setRelayerMinBond(BigInteger _value);

    /**
     * Get minimum bond of the Relayer
     *
     * @return Integer minimum bond of the Relayer
     */
    @External(readonly = true)
    BigInteger getRelayerMinBond();

    /**
     * Set period of reward calculation of the Relayer
     *
     * @param _value Integer
     */
    @External
    void setRelayerTerm(long _value);

    /**
     * Get period of reward calculation of the Relayer
     *
     * @return Integer period of reward calculation of the Relayer
     */
    @External(readonly = true)
    long getRelayerTerm();

    @External
    void setRelayerRewardRank(int _value);

    @External(readonly = true)
    int getRelayerRewardRank();

    @External
    void setNextRewardDistribution(long _height);

    /**
     * Get height of next distribution
     *
     * @return Integer height of next distribution
     */
    @External(readonly = true)
    long getNextRewardDistribution();

    /**
     * Drop the next message that to be relayed from a specific network
     * Called by the operator to manage the BTP network.
     *
     * @param _link String ( BTP Address of connected BMC )
     * @param _seq Integer ( number of the message from connected BMC )
     * @param _svc String ( number of the message from connected BMC )
     * @param _sn Integer ( serial number of the message, must be positive )
     */
    @External
    void dropMessage(String _link, BigInteger _seq, String _svc, BigInteger _sn);

    /**
     * Schedule to drop message
     * Called by the operator to manage the BTP network.
     *
     * @param _link String (BTP Address of connected BMC)
     * @param _seq Integer ( sequence number of the message from connected BMC )
     */
    @External
    void scheduleDropMessage(String _link, BigInteger _seq);

    /**
     * Cancel the scheduled drop of message
     * Called by the operator to manage the BTP network.
     *
     * @param _link String ( BTP Address of connected BMC )
     * @param _seq Integer ( sequence number of the message from connected BMC )
     */
    @External
    void cancelDropMessage(String _link, BigInteger _seq);

    /**
     * Get the list of unprocessed the scheduled drop of message
     *
     * @param _link String ( BTP Address of connected BMC )
     * @return A list of registered sequences to drop
     * <br>For Example::<br>
     * [
     *  "0x1"
     * ]
     */
    @External(readonly = true)
    BigInteger[] getScheduledDropMessages(String _link);

    /**
     * (EventLog) Drop the message of the connected BMC
     * if sn of message is less than zero
     *
     * @param _link String ( BTP Address of connected BMC )
     * @param _seq Integer ( sequence number of the message from connected BMC )
     * @param _msg  Bytes ( serialized bytes of BTP Message )
     */
    @EventLog(indexed = 2)
    void MessageDropped(String _link, BigInteger _seq, byte[] _msg);

    @External(readonly = true)
    RelayersProperties getRelayersProperties();

    /**
     * Registers the BTPLink to connect that use the BTP-Block instead of Event-Log to send message.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String ( BTP Address of BMC to connect )
     * @param _networkId Integer ( To use networkId parameter of ChainSCORE API )
     */
    @External
    void addBTPLink(String _link, long _networkId);

    /**
     * Sets to migrate the Link to the BTPLink which use the BTP-Block to send message.
     * Called by the operator to manage the BTP network.
     *
     * @param _link String ( BTP Address of connected BMC )
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
}
