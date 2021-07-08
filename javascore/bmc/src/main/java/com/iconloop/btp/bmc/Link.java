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

import com.iconloop.btp.lib.BTPAddress;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class Link {
    //TODO define default max_aggregation
    public static final int DEFAULT_MAX_AGGREGATION = 10;
    //TODO define default delay_limit, if delay_limit < 3, too sensitive
    public static final int DEFAULT_DELAY_LIMIT = 3;

    private Relays relays;

    //
    private BTPAddress addr; //primary key
    private BigInteger rxSeq;
    private BigInteger txSeq;
    //

    private int blockIntervalSrc;
    private int blockIntervalDst;
    private int maxAggregation = DEFAULT_MAX_AGGREGATION;
    private int delayLimit = DEFAULT_DELAY_LIMIT;

    private int relayIdx;
    private long rotateHeight;
    private long rxHeight; //initialize with BMC.block_height
    private long rxHeightSrc; //initialize with BMV._offset

    private int sackTerm; //0: disable sack
    private long sackNext;
    private long sackHeight;
    private BigInteger sackSeq;

    //with suffix("reachable") ArrayDB<String>
    private List<BTPAddress> reachable;

    private double scale() {
        if (blockIntervalSrc < 1 || blockIntervalDst < 1) {
            return 0;
        } else {
            return (double)blockIntervalSrc/(double)blockIntervalDst;
        }
    }

    public int rotateTerm() {
        double scale = scale();
        if (scale > 0) {
            return (int)StrictMath.ceil((double) maxAggregation / scale);
        } else {
            return 0;
        }
    }

    public Relay rotate(long currentHeight, long msgHeight, boolean hasMsg) {
        long rotateTerm = rotateTerm();
        if (rotateTerm > 0) {
            int rotateCnt;
            long baseHeight;
            if (hasMsg) {
//                long guessHeight = rxHeight + Util.ceilDiv((currentHeight - rxHeightSrc), scale()) - 1;
                long guessHeight = rxHeight + (long)StrictMath.ceil((double) (currentHeight - rxHeightSrc) / scale()) - 1;
                if (guessHeight > currentHeight) {
                    guessHeight = currentHeight;
                }

//                rotateCnt = (int)Util.ceilDiv(guessHeight - rotateHeight, rotateTerm);
                rotateCnt = (int)StrictMath.ceil((double)(guessHeight - rotateHeight)/(double)rotateTerm);
                if (rotateCnt < 0) {
                    rotateCnt = 0;
                }
                baseHeight = rotateHeight + ((rotateCnt - 1) * rotateTerm);
//                int skipCnt = (int)Util.ceilDiv((currentHeight - guessHeight), delayLimit) - 1;
                int skipCnt = (int)StrictMath.ceil((double)(currentHeight - guessHeight)/(double)delayLimit) - 1;
                if (skipCnt > 0) {
                    rotateCnt += skipCnt;
                    baseHeight = currentHeight;
                }
                rxHeight = currentHeight;
                rxHeightSrc = msgHeight;
            } else {
//                rotateCnt = (int)Util.ceilDiv((currentHeight - rotateHeight), rotateTerm);
                rotateCnt = (int)StrictMath.ceil((double)(currentHeight - rotateHeight)/(double)rotateTerm);
                baseHeight = rotateHeight + ((rotateCnt - 1) * rotateTerm);
            }
            if (rotateCnt > 0) {
                rotateHeight = baseHeight + rotateTerm;
                relayIdx += rotateCnt;
                if (relayIdx >= relays.size()) {
                    relayIdx = relayIdx % relays.size();
                }
            }
            return relays.getByIndex(relayIdx);
        } else {
            return null;
        }
    }

    public BTPAddress getAddr() {
        return addr;
    }

    public void setAddr(BTPAddress addr) {
        this.addr = addr;
    }

    public BigInteger getRxSeq() {
        return rxSeq;
    }

    public void setRxSeq(BigInteger rxSeq) {
        this.rxSeq = rxSeq;
    }

    public BigInteger getTxSeq() {
        return txSeq;
    }

    public void setTxSeq(BigInteger txSeq) {
        this.txSeq = txSeq;
    }

    public int getBlockIntervalSrc() {
        return blockIntervalSrc;
    }

    public void setBlockIntervalSrc(int blockIntervalSrc) {
        this.blockIntervalSrc = blockIntervalSrc;
    }

    public int getBlockIntervalDst() {
        return blockIntervalDst;
    }

    public void setBlockIntervalDst(int blockIntervalDst) {
        this.blockIntervalDst = blockIntervalDst;
    }

    public int getMaxAggregation() {
        return maxAggregation;
    }

    public void setMaxAggregation(int maxAggregation) {
        this.maxAggregation = maxAggregation;
    }

    public int getDelayLimit() {
        return delayLimit;
    }

    public void setDelayLimit(int delayLimit) {
        this.delayLimit = delayLimit;
    }

    public int getRelayIdx() {
        return relayIdx;
    }

    public void setRelayIdx(int relayIdx) {
        this.relayIdx = relayIdx;
    }

    public long getRotateHeight() {
        return rotateHeight;
    }

    public void setRotateHeight(long rotateHeight) {
        this.rotateHeight = rotateHeight;
    }

    public long getRxHeight() {
        return rxHeight;
    }

    public void setRxHeight(long rxHeight) {
        this.rxHeight = rxHeight;
    }

    public long getRxHeightSrc() {
        return rxHeightSrc;
    }

    public void setRxHeightSrc(long rxHeightSrc) {
        this.rxHeightSrc = rxHeightSrc;
    }

    public int getSackTerm() {
        return sackTerm;
    }

    public void setSackTerm(int sackTerm) {
        this.sackTerm = sackTerm;
    }

    public long getSackNext() {
        return sackNext;
    }

    public void setSackNext(long sackNext) {
        this.sackNext = sackNext;
    }

    public long getSackHeight() {
        return sackHeight;
    }

    public void setSackHeight(long sackHeight) {
        this.sackHeight = sackHeight;
    }

    public BigInteger getSackSeq() {
        return sackSeq;
    }

    public void setSackSeq(BigInteger sackSeq) {
        this.sackSeq = sackSeq;
    }

    public List<BTPAddress> getReachable() {
        return reachable;
    }

    public void setReachable(List<BTPAddress> reachable) {
        this.reachable = reachable;
    }

    public Relays getRelays() {
        return relays;
    }

    public void setRelays(Relays relays) {
        this.relays = relays;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Link{");
        sb.append("relays=").append(relays);
        sb.append(", addr=").append(addr);
        sb.append(", rxSeq=").append(rxSeq);
        sb.append(", txSeq=").append(txSeq);
        sb.append(", blockIntervalSrc=").append(blockIntervalSrc);
        sb.append(", blockIntervalDst=").append(blockIntervalDst);
        sb.append(", maxAggregation=").append(maxAggregation);
        sb.append(", delayLimit=").append(delayLimit);
        sb.append(", relayIdx=").append(relayIdx);
        sb.append(", rotateHeight=").append(rotateHeight);
        sb.append(", rxHeight=").append(rxHeight);
        sb.append(", rxHeightSrc=").append(rxHeightSrc);
        sb.append(", sackTerm=").append(sackTerm);
        sb.append(", sackNext=").append(sackNext);
        sb.append(", sackHeight=").append(sackHeight);
        sb.append(", sackSeq=").append(sackSeq);
        sb.append(", reachable=").append(reachable);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Link obj) {
        obj.writeObject(writer);
    }

    public static Link readObject(ObjectReader reader) {
        Link obj = new Link();
        reader.beginList();
        obj.setAddr(reader.readNullable(BTPAddress.class));
        obj.setRxSeq(reader.readNullable(BigInteger.class));
        obj.setTxSeq(reader.readNullable(BigInteger.class));
        obj.setBlockIntervalSrc(reader.readInt());
        obj.setBlockIntervalDst(reader.readInt());
        obj.setMaxAggregation(reader.readInt());
        obj.setDelayLimit(reader.readInt());
        obj.setRelayIdx(reader.readInt());
        obj.setRotateHeight(reader.readLong());
        obj.setRxHeight(reader.readLong());
        obj.setRxHeightSrc(reader.readLong());
        obj.setSackTerm(reader.readInt());
        obj.setSackNext(reader.readLong());
        obj.setSackHeight(reader.readLong());
        obj.setSackSeq(reader.readNullable(BigInteger.class));
        if (reader.beginNullableList()) {
            List<BTPAddress> reachable = new ArrayList<>();
            while(reader.hasNext()) {
                reachable.add(reader.readNullable(BTPAddress.class));
            }
            obj.setReachable(reachable);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(16);
        writer.writeNullable(this.getAddr());
        writer.writeNullable(this.getRxSeq());
        writer.writeNullable(this.getTxSeq());
        writer.write(this.getBlockIntervalSrc());
        writer.write(this.getBlockIntervalDst());
        writer.write(this.getMaxAggregation());
        writer.write(this.getDelayLimit());
        writer.write(this.getRelayIdx());
        writer.write(this.getRotateHeight());
        writer.write(this.getRxHeight());
        writer.write(this.getRxHeightSrc());
        writer.write(this.getSackTerm());
        writer.write(this.getSackNext());
        writer.write(this.getSackHeight());
        writer.writeNullable(this.getSackSeq());
        List<BTPAddress> reachable = this.getReachable();
        if (reachable != null) {
            writer.beginNullableList(reachable.size());
            for(BTPAddress v : reachable) {
                writer.write(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static Link fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Link.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Link.writeObject(writer, this);
        return writer.toByteArray();
    }
}
