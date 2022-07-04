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

package foundation.icon.btp.bmv.btpblock;

import score.*;

import java.math.BigInteger;

public class BMVProperties {
    public static final BMVProperties DEFAULT;

    static {
        DEFAULT = new BMVProperties();
    }

    private byte[] srcNetworkID;
    private int networkTypeID;
    private BigInteger networkID;
    private byte[] proofContextHash;
    private byte[] proofContext;
    private byte[] lastNetworkSectionHash;
    private Address bmc;
    private BigInteger lastSequence;
    private byte[] lastMessagesRoot;
    private BigInteger lastMessageCount;
    private BigInteger lastFirstMessageSN;
    private BigInteger height;
    private BigInteger sequenceOffset;

    public byte[] getSrcNetworkID() {
        return srcNetworkID;
    }

    public void setSrcNetworkID(byte[] srcNetworkID) {
        this.srcNetworkID = srcNetworkID;
    }

    public String getNetwork() {
        var stringSrc = new String(srcNetworkID);
        var delimIndex = stringSrc.lastIndexOf("/");
        return stringSrc.substring(delimIndex + 1);
    }

    public int getNetworkTypeID() {
        return networkTypeID;
    }

    public void setNetworkTypeID(int networkTypeID) {
        this.networkTypeID = networkTypeID;
    }

    public BigInteger getNetworkID() {
        return networkID;
    }

    public void setNetworkID(BigInteger networkID) {
        this.networkID = networkID;
    }

    public byte[] getProofContextHash() {
        return proofContextHash;
    }

    public void setProofContextHash(byte[] proofContextHash) {
        this.proofContextHash = proofContextHash;
    }

    public byte[] getProofContext() {
        return proofContext;
    }

    public void setProofContext(byte[] proofContext) {
        this.proofContext = proofContext;
    }

    public byte[] getLastNetworkSectionHash() {
        return lastNetworkSectionHash;
    }

    public void setLastNetworkSectionHash(byte[] lastNetworkSectionHash) {
        this.lastNetworkSectionHash = lastNetworkSectionHash;
    }

    public BigInteger getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(BigInteger lastSequence) {
        this.lastSequence = lastSequence;
    }

    public byte[] getLastMessagesRoot() {
        return lastMessagesRoot;
    }

    public void setLastMessagesRoot(byte[] lastMessagesRoot) {
        this.lastMessagesRoot = lastMessagesRoot;
    }

    public BigInteger getLastMessageCount() {
        return lastMessageCount;
    }

    public void setLastMessageCount(BigInteger lastMessageCount) {
        this.lastMessageCount = lastMessageCount;
    }

    public BigInteger getLastFirstMessageSN() {
        return lastFirstMessageSN;
    }

    public void setLastFirstMessageSN(BigInteger lastFirstMessageSN) {
        this.lastFirstMessageSN = lastFirstMessageSN;
    }

    public BigInteger getProcessedMessageCount() {
        return lastSequence.subtract(lastFirstMessageSN);
    }

    public BigInteger getRemainMessageCount() {
        return lastMessageCount.subtract(getProcessedMessageCount());
    }

    public Address getBmc() {
        return bmc;
    }

    public void setBmc(Address bmc) {
        this.bmc = bmc;
    }

    public BigInteger getHeight() {
        return height;
    }

    public void setHeight(BigInteger height) {
        this.height = height;
    }

    public BigInteger getSequenceOffset() {
        return sequenceOffset;
    }

    public void setSequenceOffset(BigInteger sequenceOffset) {
        this.sequenceOffset = sequenceOffset;
    }


    public static BMVProperties readObject(ObjectReader reader) {
        BMVProperties obj = new BMVProperties();
        reader.beginList();
        obj.setSrcNetworkID(reader.readByteArray());
        obj.setNetworkTypeID(reader.readInt());
        obj.setNetworkID(reader.readNullable(BigInteger.class));
        obj.setProofContextHash(reader.readNullable(byte[].class));
        obj.setProofContext(reader.readNullable(byte[].class));
        obj.setLastNetworkSectionHash(reader.readNullable(byte[].class));
        obj.setBmc(reader.readAddress());
        obj.setLastSequence(reader.readNullable(BigInteger.class));
        obj.setLastMessagesRoot(reader.readNullable(byte[].class));
        obj.setLastMessageCount(reader.readNullable(BigInteger.class));
        obj.setLastFirstMessageSN(reader.readNullable(BigInteger.class));
        obj.setHeight(reader.readBigInteger());
        obj.setSequenceOffset(reader.readBigInteger());
        reader.end();
        return obj;
    }

    public static void writeObject(ObjectWriter writer, BMVProperties obj) {
        writer.beginList(11);
        writer.write(obj.srcNetworkID);
        writer.write(obj.networkTypeID);
        writer.writeNullable(obj.networkID);
        writer.writeNullable(obj.proofContextHash);
        writer.writeNullable(obj.proofContext);
        writer.writeNullable(obj.lastNetworkSectionHash);
        writer.write(obj.bmc);
        writer.writeNullable(obj.lastSequence);
        writer.writeNullable(obj.lastMessagesRoot);
        writer.writeNullable(obj.lastMessageCount);
        writer.writeNullable(obj.lastFirstMessageSN);
        writer.write(obj.height);
        writer.write(obj.sequenceOffset);
        writer.end();
    }
}
