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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class RelayerManagerProperties {
    public static final RelayerManagerProperties DEFAULT;

    static {
        DEFAULT = new RelayerManagerProperties();
        DEFAULT.setRelayerMinBond(BigInteger.ONE);
        DEFAULT.setRelayerTerm(43200);
        DEFAULT.setNextRewardDistribution(0);
        DEFAULT.setRelayerRewardRank(25);
        DEFAULT.setDistributed(BigInteger.ZERO);
        DEFAULT.setCarryover(BigInteger.ZERO);
        DEFAULT.setBond(BigInteger.ZERO);
    }

    private BigInteger relayerMinBond;
    private long relayerTerm;
    private int relayerRewardRank;
    private long nextRewardDistribution;
    private BigInteger distributed;
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

    public BigInteger getDistributed() {
        return distributed;
    }

    public void setDistributed(BigInteger distributed) {
        this.distributed = distributed;
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
        sb.append(", remained=").append(distributed);
        sb.append(", carryover=").append(carryover);
        sb.append(", bond=").append(bond);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, RelayerManagerProperties obj) {
        obj.writeObject(writer);
    }

    public static RelayerManagerProperties readObject(ObjectReader reader) {
        RelayerManagerProperties obj = new RelayerManagerProperties();
        reader.beginList();
        obj.setRelayerMinBond(reader.readNullable(BigInteger.class));
        obj.setRelayerTerm(reader.readLong());
        obj.setRelayerRewardRank(reader.readInt());
        obj.setNextRewardDistribution(reader.readLong());
        obj.setDistributed(reader.readNullable(BigInteger.class));
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
        writer.writeNullable(this.getDistributed());
        writer.writeNullable(this.getCarryover());
        writer.writeNullable(this.getBond());
        writer.end();
    }

    public static RelayerManagerProperties fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return RelayerManagerProperties.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        RelayerManagerProperties.writeObject(writer, this);
        return writer.toByteArray();
    }
}
