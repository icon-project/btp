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

package foundation.icon.btp.bmc;

import foundation.icon.score.util.BigIntegerUtil;
import foundation.icon.score.util.Logger;
import score.Address;
import score.Context;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.Map;

public class RelayerManagerImpl implements RelayerManager {
    private static final Logger logger = Logger.getLogger(RelayerManagerImpl.class);
    public static final int DEFAULT_REWARD_PERCENT_SCALE_FACTOR = 4;
    private final Relayers relayers;

    public RelayerManagerImpl(String id) {
        relayers = new Relayers(id);
    }

    private void requireOwnerAccess() {
        //FIXME Using foundation.icon.btp.lib.OwnerManager.isOwner(Context.Caller())
    }

    @Payable
    @External
    public void registerRelayer(String _desc) {
        Address addr = Context.getCaller();
        if (relayers.containsKey(addr)) {
            throw BMCException.unknown("already registered relayer");
        }
        BigInteger bond = Context.getValue();
        BigInteger relayerMinBond = getRelayerMinBond();
        if (bond == null || bond.compareTo(relayerMinBond) < 0) {
            throw BMCException.unknown("require bond at least " + relayerMinBond + " icx");
        }

        Relayer relayer = new Relayer();
        relayer.setAddr(addr);
        relayer.setDesc(_desc);
        relayer.setSince(Context.getBlockHeight());
        relayer.setSinceExtra(Context.getTransactionIndex());
        relayer.setBond(bond);
        relayer.setReward(BigInteger.ZERO);
        logger.println("registerRelayer", relayer);
        relayers.put(addr, relayer);

        RelayersProperties properties = relayers.getProperties();
        properties.setBond(properties.getBond().add(bond));
        relayers.setProperties(properties);
    }

    private void removeRelayerAndRefund(Address _addr, Address _refund) {
        if (!relayers.containsKey(_addr)) {
            throw BMCException.unknown("not found registered relayer");
        }
        Relayer relayer = relayers.remove(_addr);

        RelayersProperties properties = relayers.getProperties();
        BigInteger bond = relayer.getBond();
        Context.transfer(_refund, bond);
        properties.setBond(properties.getBond().subtract(bond));

        BigInteger reward = relayer.getReward();
        if (reward.compareTo(BigInteger.ZERO) > 0) {
            Context.transfer(_refund, reward);
            properties.setDistributed(properties.getDistributed().subtract(reward));
        }
        relayers.setProperties(properties);
    }

    @External
    public void unregisterRelayer() {
        Address _addr = Context.getCaller();
        removeRelayerAndRefund(_addr, _addr);
    }

    @External
    public void removeRelayer(Address _addr, Address _refund) {
        requireOwnerAccess();
        removeRelayerAndRefund(_addr, _refund);
    }

    @External(readonly = true)
    public Map<String, Relayer> getRelayers() {
        return relayers.toMapWithKeyToString();
    }

    @External
    public void distributeRelayerReward() {
        logger.println("distributeRelayerReward");
        long currentHeight = Context.getBlockHeight();
        RelayersProperties properties = relayers.getProperties();
        long nextRewardDistribution = properties.getNextRewardDistribution();
        if (nextRewardDistribution <= currentHeight) {
            long delayOfDistribution = currentHeight - nextRewardDistribution;
            long relayerTerm = properties.getRelayerTerm();
            nextRewardDistribution += relayerTerm;
            if (nextRewardDistribution <= currentHeight) {
                int omitted = 0;
                while(nextRewardDistribution < currentHeight) {
                    nextRewardDistribution += relayerTerm;
                    omitted++;
                }
                logger.println("WARN","rewardDistribution was omitted", omitted, "term:", relayerTerm);
            }
            long since = nextRewardDistribution - (relayerTerm * 2);
            properties.setNextRewardDistribution(nextRewardDistribution);

            BigInteger balance = Context.getBalance(Context.getAddress());
            BigInteger distributed = properties.getDistributed();
            BigInteger bond = properties.getBond();
            BigInteger current = balance.subtract(bond);
            BigInteger carryover = properties.getCarryover();
            logger.println("distributeRelayerReward", "since:", since, "delay:",delayOfDistribution,
                    "balance:", balance, "distributed:", distributed, "bond:", bond, "carryover:", carryover);
            if (current.compareTo(distributed) > 0) {
                BigInteger budget = current.subtract(distributed);
                logger.println("distributeRelayerReward","budget:", budget, "transferred:", budget.subtract(carryover));
                carryover = budget;
                Relayer[] filteredRelayers = relayers.getValuesBySinceAndSortAsc(since);
                BigInteger sumOfBond = BigInteger.ZERO;
                int lenOfRelayers = StrictMath.min(properties.getRelayerRewardRank(), filteredRelayers.length);
                for (int i = 0; i < lenOfRelayers; i++) {
                    sumOfBond = sumOfBond.add(filteredRelayers[i].getBond());
                }
                logger.println("distributeRelayerReward","sumOfBond:", sumOfBond, "lenOfRelayers:", lenOfRelayers);
                BigInteger sumOfReward = BigInteger.ZERO;
                for (int i = 0; i < lenOfRelayers; i++) {
                    Relayer relayer = filteredRelayers[i];
                    double percentage = BigIntegerUtil.floorDivide(relayer.getBond(), sumOfBond, DEFAULT_REWARD_PERCENT_SCALE_FACTOR);
                    BigInteger reward = BigIntegerUtil.multiply(budget, percentage);
                    relayer.setReward(relayer.getReward().add(reward));
                    logger.println("distributeRelayerReward", "relayer:",relayer.getAddr(), "percentage:",percentage, "reward:", reward);
                    relayers.put(relayer.getAddr(), relayer);
                    carryover = carryover.subtract(reward);
                    sumOfReward = sumOfReward.add(reward);
                }

                logger.println("distributeRelayerReward","sumOfReward:", sumOfReward, "carryover:", carryover, "nextRewardDistribution:", nextRewardDistribution);
                properties.setDistributed(distributed.add(sumOfReward));
                properties.setCarryover(carryover);
            } else {
                //reward is zero or negative
                logger.println("WARN","transferred reward is zero or negative");
            }
            relayers.setProperties(properties);
        }
    }

    @External
    public void claimRelayerReward() {
        Address addr = Context.getCaller();
        if (!relayers.containsKey(addr)) {
            throw BMCException.unknown("not found registered relayer");
        }
        Relayer relayer = relayers.get(addr);
        BigInteger reward = relayer.getReward();
        if (reward.compareTo(BigInteger.ZERO) < 1) {
            throw BMCException.unknown("reward is not remained");
        }
        Context.transfer(addr, reward);
        relayer.setReward(BigInteger.ZERO);
        relayers.put(addr, relayer);
        RelayersProperties properties = relayers.getProperties();
        properties.setDistributed(properties.getDistributed().subtract(reward));
        relayers.setProperties(properties);
    }

    @External
    public void setRelayerMinBond(BigInteger _value) {
        requireOwnerAccess();
        if (_value.compareTo(BigInteger.ZERO) < 0) {
            throw BMCException.unknown("minBond must be positive");
        }
        RelayersProperties properties = relayers.getProperties();
        properties.setRelayerMinBond(_value);
        relayers.setProperties(properties);
    }

    @External(readonly = true)
    public BigInteger getRelayerMinBond() {
        RelayersProperties properties = relayers.getProperties();
        return properties.getRelayerMinBond();
    }

    @External
    public void setRelayerTerm(long _value) {
        requireOwnerAccess();
        if (_value < 1) {
            throw BMCException.unknown("term must be positive");
        }
        RelayersProperties properties = relayers.getProperties();
        properties.setRelayerTerm(_value);
        relayers.setProperties(properties);
    }

    @External(readonly = true)
    public long getRelayerTerm() {
        RelayersProperties properties = relayers.getProperties();
        return properties.getRelayerTerm();
    }

    @External
    public void setRelayerRewardRank(int _value) {
        requireOwnerAccess();
        if (_value < 1) {
            throw BMCException.unknown("rewardRank must be positive");
        }
        RelayersProperties properties = relayers.getProperties();
        properties.setRelayerRewardRank(_value);
        relayers.setProperties(properties);
    }

    @External(readonly = true)
    public int getRelayerRewardRank() {
        RelayersProperties properties = relayers.getProperties();
        return properties.getRelayerRewardRank();
    }

    @External
    public void setNextRewardDistribution(long _height) {
        requireOwnerAccess();
        RelayersProperties properties = relayers.getProperties();
        properties.setNextRewardDistribution(StrictMath.max(_height, Context.getBlockHeight()));
        relayers.setProperties(properties);
    }

    @External(readonly = true)
    public long getNextRewardDistribution() {
        RelayersProperties properties = relayers.getProperties();
        return properties.getNextRewardDistribution();
    }

    // for re-deploy test only, temporary only
    @External(readonly = true)
    public RelayersProperties getRelayersProperties() {
        return relayers.getProperties();
    }
}
