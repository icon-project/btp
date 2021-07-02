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
    private final VarDB<Properties> properties;

    public RelayerManagerImpl(String id) {
        super(id, Address.class, Relayer.class, logger);
        properties = Context.newVarDB(super.concatId("properties"), Properties.class);
    }

    public Properties getProperties() {
        return properties.getOrDefault(Properties.DEFAULT);
    }

    public void setProperties(Properties properties) {
        this.properties.set(properties);
    }

    @Override
    public BigInteger getRelayerMinBond() {
        Properties properties = getProperties();
        return properties.getRelayerMinBond();
    }

    @Override
    public void setRelayerMinBond(BigInteger _value) {
        if (_value.compareTo(BigInteger.ZERO) < 0) {
            throw BMCException.unknown("minBond must be positive");
        }
        Properties properties = getProperties();
        properties.setRelayerMinBond(_value);
        setProperties(properties);
    }

    @Override
    public long getRelayerTerm() {
        Properties properties = getProperties();
        return properties.getRelayerTerm();
    }

    @Override
    public void setRelayerTerm(long _value) {
        if (_value < 1) {
            throw BMCException.unknown("term must be positive");
        }
        Properties properties = getProperties();
        properties.setRelayerTerm(_value);
        setProperties(properties);
    }

    @Override
    public int getRelayerRewardRank() {
        Properties properties = getProperties();
        return properties.getRelayerRewardRank();
    }

    @Override
    public void setRelayerRewardRank(int _value) {
        if (_value < 1) {
            throw BMCException.unknown("rewardRank must be positive");
        }
        Properties properties = getProperties();
        properties.setRelayerRewardRank(_value);
        setProperties(properties);
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
                if (compare(v, a[j]) < 0) {
                    v = a[j];
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
        Properties properties = getProperties();
        long nextRewardDistribution = properties.getNextRewardDistribution();
        if (nextRewardDistribution <= currentHeight) {
            long relayerTerm = properties.getRelayerTerm();
            if ((currentHeight - nextRewardDistribution) >= relayerTerm) {
                logger.println("WARN","rewardDistribution was omitted", currentHeight - nextRewardDistribution);
            }
            BigInteger balance = Context.getBalance(Context.getAddress());
            BigInteger remained = properties.getRemained();
            BigInteger bond = properties.getBond();
            BigInteger current = balance.subtract(bond);
            logger.println("distributeRelayerReward","balance:", balance, "remained:", remained, "bond:", bond);
            if (current.compareTo(remained) > 0) {
                remained = current;
                BigInteger carryover = properties.getCarryover();
                BigInteger budget = current.subtract(remained).add(carryover);
                carryover = budget;
                Relayer[] relayers = filterSince(values(), nextRewardDistribution - relayerTerm);
                sortAsc(relayers);
                BigInteger sum = BigInteger.ZERO;
                int len = StrictMath.min(properties.getRelayerRewardRank(), relayers.length);
                for (int i = 0; i < len; i++) {
                    sum = sum.add(relayers[i].getBond());
                }
                logger.println("distributeRelayerReward","budget:", budget, "carryover:", carryover, "sum:", sum, "len:");
                for (int i = 0; i < len; i++) {
                    Relayer relayer = relayers[i];
                    double percentage = BigIntegerUtil.floorDivide(relayer.getBond(), sum, DEFAULT_REWARD_PERCENT_SCALE_FACTOR);
                    BigInteger reward = BigIntegerUtil.multiply(budget, percentage);
                    relayer.setReward(relayer.getReward().add(reward));
                    logger.println("distributeRelayerReward", "relayer:",relayer.getAddr(), "percentage:",percentage, "reward:", reward);
                    put(relayer.getAddr(), relayer);
                    carryover = carryover.subtract(reward);
                }
                while(nextRewardDistribution < currentHeight) {
                    nextRewardDistribution += relayerTerm;
                }
                logger.println("distributeRelayerReward","carryover:", carryover);
                properties.setRemained(remained);
                properties.setCarryover(carryover);
                setProperties(properties);
            } else {
                //reward is zero or negative
                logger.println("WARN","transferred reward is zero or negative",
                        "balance:",balance, "remain:", remained);
            }
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
        Properties properties = getProperties();
        properties.setRemained(properties.getRemained().subtract(reward));
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
        Properties properties = getProperties();
        properties.setBond(properties.getBond().add(bond));
        setProperties(properties);
    }

    @Override
    public void unregisterRelayer() {
        Address _addr = Context.getCaller();
        if (!containsKey(_addr)) {
            throw BMCException.unknown("not found registered relayer");
        }
        Relayer relayer = remove(_addr);
        Properties properties = getProperties();
        BigInteger bond = relayer.getBond();
        Context.transfer(_addr, bond);
        properties.setBond(properties.getBond().subtract(bond));

        BigInteger reward = relayer.getReward();
        if (reward.compareTo(BigInteger.ZERO) > 0) {
            Context.transfer(_addr, reward);
            properties.setRemained(properties.getRemained().subtract(reward));
        }
        setProperties(properties);
    }

    @Override
    public Map getRelayers() {
        //couldn't use EnumerableDictDB.toMap,
        //  because Unshadower support only String type for key
        return toMapWithKeyToString();
    }

    public static class Properties {
        public static final Properties DEFAULT;
        static {
            DEFAULT = new Properties();
            DEFAULT.setRelayerMinBond(BigInteger.ONE);
            DEFAULT.setRelayerTerm(43200);
            DEFAULT.setNextRewardDistribution(0);
            DEFAULT.setRelayerRewardRank(25);
            DEFAULT.setRemained(BigInteger.ZERO);
            DEFAULT.setCarryover(BigInteger.ZERO);
            DEFAULT.setBond(BigInteger.ZERO);
        }

        private BigInteger relayerMinBond;
        private long relayerTerm;
        private int relayerRewardRank;
        private long nextRewardDistribution;
        private BigInteger remained;
        private BigInteger carryover;
        private BigInteger bond;

        public BigInteger getRelayerMinBond() {
            return relayerMinBond;
        }

        public void setRelayerMinBond(BigInteger relayerMinBond) {
            this.relayerMinBond = relayerMinBond;
        }

        public long getRelayerTerm() {
            return relayerTerm;
        }

        public void setRelayerTerm(long relayerTerm) {
            this.relayerTerm = relayerTerm;
        }

        public int getRelayerRewardRank() {
            return relayerRewardRank;
        }

        public void setRelayerRewardRank(int relayerRewardRank) {
            this.relayerRewardRank = relayerRewardRank;
        }

        public long getNextRewardDistribution() {
            return nextRewardDistribution;
        }

        public void setNextRewardDistribution(long nextRewardDistribution) {
            this.nextRewardDistribution = nextRewardDistribution;
        }

        public BigInteger getRemained() {
            return remained;
        }

        public void setRemained(BigInteger remained) {
            this.remained = remained;
        }

        public BigInteger getCarryover() {
            return carryover;
        }

        public void setCarryover(BigInteger carryover) {
            this.carryover = carryover;
        }

        public BigInteger getBond() {
            return bond;
        }

        public void setBond(BigInteger bond) {
            this.bond = bond;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Properties{");
            sb.append("relayerMinBond=").append(relayerMinBond);
            sb.append(", relayerTerm=").append(relayerTerm);
            sb.append(", relayerRewardRank=").append(relayerRewardRank);
            sb.append(", nextRewardDistribution=").append(nextRewardDistribution);
            sb.append(", remained=").append(remained);
            sb.append(", carryover=").append(carryover);
            sb.append(", bond=").append(bond);
            sb.append('}');
            return sb.toString();
        }

        public static void writeObject(ObjectWriter writer, Properties obj) {
            obj.writeObject(writer);
        }

        public static Properties readObject(ObjectReader reader) {
            Properties obj = new Properties();
            reader.beginList();
            obj.setRelayerMinBond(reader.readNullable(BigInteger.class));
            obj.setRelayerTerm(reader.readLong());
            obj.setRelayerRewardRank(reader.readInt());
            obj.setNextRewardDistribution(reader.readLong());
            obj.setRemained(reader.readNullable(BigInteger.class));
            obj.setCarryover(reader.readNullable(BigInteger.class));
            obj.setBond(reader.readNullable(BigInteger.class));
            reader.end();
            return obj;
        }

        public void writeObject(ObjectWriter writer) {
            writer.beginList(7);
            writer.writeNullable(this.getRelayerMinBond());
            writer.write(this.getRelayerTerm());
            writer.write(this.getRelayerRewardRank());
            writer.write(this.getNextRewardDistribution());
            writer.writeNullable(this.getRemained());
            writer.writeNullable(this.getCarryover());
            writer.writeNullable(this.getBond());
            writer.end();
        }

        public static Properties fromBytes(byte[] bytes) {
            ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
            return Properties.readObject(reader);
        }

        public byte[] toBytes() {
            ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
            Properties.writeObject(writer, this);
            return writer.toByteArray();
        }
    }
}
