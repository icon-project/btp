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

import com.iconloop.score.util.BigIntegerUtil;
import com.iconloop.score.util.EnumerableDictDB;
import com.iconloop.score.util.Logger;
import score.*;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class RelayerManagerImpl extends EnumerableDictDB<Address, Relayer> implements RelayerManager {
    private static final Logger logger = Logger.getLogger(RelayerManagerImpl.class);

    public static final int DEFAULT_REWARD_PERCENT_SCALE_FACTOR = 4;
    private final VarDB<RelayerManagerProperties> properties;

    public RelayerManagerImpl(String id) {
        super(id, Address.class, Relayer.class, logger);
        properties = Context.newVarDB(super.concatId("properties"), RelayerManagerProperties.class);
    }

    public RelayerManagerProperties getProperties() {
        return properties.getOrDefault(RelayerManagerProperties.DEFAULT);
    }

    public void setProperties(RelayerManagerProperties properties) {
        this.properties.set(properties);
    }

    @Override
    public RelayerManagerProperties getRelayerManagerProperties() {
        return getProperties();
    }

    @Override
    public void setRelayerMinBond(BigInteger _value) {
        if (_value.compareTo(BigInteger.ZERO) < 0) {
            throw BMCException.unknown("minBond must be positive");
        }
        RelayerManagerProperties properties = getProperties();
        properties.setRelayerMinBond(_value);
        setProperties(properties);
    }

    @Override
    public void setRelayerTerm(long _value) {
        if (_value < 1) {
            throw BMCException.unknown("term must be positive");
        }
        RelayerManagerProperties properties = getProperties();
        properties.setRelayerTerm(_value);
        setProperties(properties);
    }

    @Override
    public void setNextRewardDistribution(long _height) {
        RelayerManagerProperties properties = getProperties();
        properties.setNextRewardDistribution(StrictMath.max(_height, Context.getBlockHeight()));
        setProperties(properties);
    }

    @Override
    public void setRelayerRewardRank(int _value) {
        if (_value < 1) {
            throw BMCException.unknown("rewardRank must be positive");
        }
        RelayerManagerProperties properties = getProperties();
        properties.setRelayerRewardRank(_value);
        setProperties(properties);
    }

    @Override
    public BigInteger getRelayerMinBond() {
        RelayerManagerProperties properties = getProperties();
        return properties.getRelayerMinBond();
    }

    @Override
    public long getRelayerTerm() {
        RelayerManagerProperties properties = getProperties();
        return properties.getRelayerTerm();
    }

    @Override
    public int getRelayerRewardRank() {
        RelayerManagerProperties properties = getProperties();
        return properties.getRelayerRewardRank();
    }

    public static Relayer[] filterSince(List<Relayer> list, long since) {
        int sinceLen=0;
        for (Relayer relayer : list) {
            if (relayer.getSince() < since) {
                sinceLen++;
            }
        }
        Relayer[] array = new Relayer[sinceLen];
        int i=0;
        for (Relayer relayer : list) {
            if (relayer.getSince() < since) {
                array[i++] = relayer;
            }
        }
        return array;
    }

    /**
     * Compare Relayer for sorting
     * bond desc, since asc, sinceExtra asc
     *
     * @apiNote Not allowed to use java.util.Comparator in javaee
     * @apiNote If Relayer implements java.lang.Comparable,
     *          it makes 'No implementation found for compareTo(Ljava/lang/Object;)I' in Enum classes.
     *
     * @param o1 the first Relayer to compare
     * @param o2 the second Relayer to compare
     * @return bond desc, since asc, sinceExtra asc
     */
    public static int compare(Relayer o1, Relayer o2) {
        int compBond = o2.getBond().compareTo(o1.getBond());
        if (compBond == 0) {
            int compSince = Long.compare(o1.getSince(), o2.getSince());
            if (compSince == 0) {
                return Integer.compare(o1.getSinceExtra(), o2.getSinceExtra());
            } else {
                return compSince;
            }
        } else {
            return compBond;
        }
    }

    /**
     * Sorts array of Relayer
     * instead of com.iconloop.score.util.ArrayUtil#sort(java.lang.Comparable[])
     * @see RelayerManagerImpl#compare(Relayer, Relayer)
     *
     * @param a Array of Relayer
     */
    public static void sortAsc(Relayer[] a) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            Relayer v = a[i];
            for (int j = i + 1; j < len; j++) {
                if (compare(v, a[j]) > 0) {
                    Relayer t = v;
                    v = a[j];
                    a[j] = t;
                }
            }
            a[i] = v;
        }
    }

    /**
     * Calculate the reward amount for the Relayer based on the amount of bonded-ICX.
     * Reward is calculated on a 43,200 blocks basis
     * Reward = floor(DELEGATED_INCENTIVE * Bond of relayer / sum of bond of top25-relayers)
     * called when register, unregister, claim, handleRelayMessage
     */
    @Override
    public void distributeRelayerReward() {
        logger.println("distributeRelayerReward");
        long currentHeight = Context.getBlockHeight();
        RelayerManagerProperties properties = getProperties();
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
                Relayer[] relayers = filterSince(values(), since);
                sortAsc(relayers);
                BigInteger sumOfBond = BigInteger.ZERO;
                int lenOfRelayers = StrictMath.min(properties.getRelayerRewardRank(), relayers.length);
                for (int i = 0; i < lenOfRelayers; i++) {
                    sumOfBond = sumOfBond.add(relayers[i].getBond());
                }
                logger.println("distributeRelayerReward","sumOfBond:", sumOfBond, "lenOfRelayers:", lenOfRelayers);
                BigInteger sumOfReward = BigInteger.ZERO;
                for (int i = 0; i < lenOfRelayers; i++) {
                    Relayer relayer = relayers[i];
                    double percentage = BigIntegerUtil.floorDivide(relayer.getBond(), sumOfBond, DEFAULT_REWARD_PERCENT_SCALE_FACTOR);
                    BigInteger reward = BigIntegerUtil.multiply(budget, percentage);
                    relayer.setReward(relayer.getReward().add(reward));
                    logger.println("distributeRelayerReward", "relayer:",relayer.getAddr(), "percentage:",percentage, "reward:", reward);
                    put(relayer.getAddr(), relayer);
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
            setProperties(properties);
        }
    }

    @Override
    public void claimRelayerReward() {
        Address addr = Context.getCaller();
        if (!containsKey(addr)) {
            throw BMCException.unknown("not found registered relayer");
        }
        Relayer relayer = get(addr);
        BigInteger reward = relayer.getReward();
        if (reward.compareTo(BigInteger.ZERO) < 1) {
            throw BMCException.unknown("reward is not remained");
        }
        Context.transfer(addr, reward);
        relayer.setReward(BigInteger.ZERO);
        put(addr, relayer);
        RelayerManagerProperties properties = getProperties();
        properties.setDistributed(properties.getDistributed().subtract(reward));
        setProperties(properties);
    }

    @Override
    public void registerRelayer(String _desc) {
        Address addr = Context.getCaller();
        if (containsKey(addr)) {
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
        put(addr, relayer);
        RelayerManagerProperties properties = getProperties();
        properties.setBond(properties.getBond().add(bond));
        setProperties(properties);
    }

    @Override
    public void unregisterRelayer() {
        Address _addr = Context.getCaller();
        removeRelayer(_addr, _addr);
    }

    @Override
    public void removeRelayer(Address _addr, Address _refund) {
        if (!containsKey(_addr)) {
            throw BMCException.unknown("not found registered relayer");
        }
        Relayer relayer = remove(_addr);
        RelayerManagerProperties properties = getProperties();
        BigInteger bond = relayer.getBond();
        Context.transfer(_refund, bond);
        properties.setBond(properties.getBond().subtract(bond));

        BigInteger reward = relayer.getReward();
        if (reward.compareTo(BigInteger.ZERO) > 0) {
            Context.transfer(_refund, reward);
            properties.setDistributed(properties.getDistributed().subtract(reward));
        }
        setProperties(properties);
    }

    @Override
    public Map<String, Relayer> getRelayers() {
        //couldn't use EnumerableDictDB.toMap,
        //  because Unshadower support only String type for key
        return toMapWithKeyToString();
    }

}
