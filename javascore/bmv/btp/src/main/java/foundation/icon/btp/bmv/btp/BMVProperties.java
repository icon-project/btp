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

package foundation.icon.btp.bmv.btp;

import score.*;

public class BMVProperties {
    public static final BMVProperties DEFAULT;

    static {
        DEFAULT = new BMVProperties();
    }

    private byte[] srcNetworkID;
    private int networkTypeID;
    private int networkID;
    private byte[] proofContextHash;
    private byte[] proofContext;
    private byte[] lastNetworkSectionHash;
    private Address bmc;
    private int lastSequence;
    private byte[] lastMessagesRoot;
    private int lastMessageCount;
    private int lastFirstMessageSN;

    public byte[] getSrcNetworkID() {
        return srcNetworkID;
    }

    public void setSrcNetworkID(byte[] srcNetworkID) {
        this.srcNetworkID = srcNetworkID;
    }

    public int getNetworkTypeID() {
        return networkTypeID;
    }

    public void setNetworkTypeID(int networkTypeID) {
        this.networkTypeID = networkTypeID;
    }

    public int getNetworkID() {
        return networkID;
    }

    public void setNetworkID(int networkID) {
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

    public int getLastSequence() {
        return lastSequence;
    }

    public void setLastSequence(int lastSequence) {
        this.lastSequence = lastSequence;
    }

    public byte[] getLastMessagesRoot() {
        return lastMessagesRoot;
    }

    public void setLastMessagesRoot(byte[] lastMessagesRoot) {
        this.lastMessagesRoot = lastMessagesRoot;
    }

    public int getLastMessageCount() {
        return lastMessageCount;
    }

    public void setLastMessageCount(int lastMessageCount) {
        this.lastMessageCount = lastMessageCount;
    }

    public int getLastFirstMessageSN() {
        return lastFirstMessageSN;
    }

    public void setLastFirstMessageSN(int lastFirstMessageSN) {
        this.lastFirstMessageSN = lastFirstMessageSN;
    }


    public Address getBmc() {
        return bmc;
    }

    public void setBmc(Address bmc) {
        this.bmc = bmc;
    }

    public static BMVProperties readObject(ObjectReader reader) {
        BMVProperties obj = new BMVProperties();
        reader.beginList();
        obj.setSrcNetworkID(reader.readByteArray());
        obj.setNetworkTypeID(reader.readInt());
        obj.setNetworkID(reader.readNullable(Integer.class));
        obj.setProofContextHash(reader.readNullable(byte[].class));
        obj.setProofContext(reader.readNullable(byte[].class));
        obj.setLastNetworkSectionHash(reader.readNullable(byte[].class));
        obj.setBmc(reader.readAddress());
        obj.setLastSequence(reader.readNullable(Integer.class));
        obj.setLastMessagesRoot(reader.readNullable(byte[].class));
        obj.setLastMessageCount(reader.readNullable(Integer.class));
        obj.setLastFirstMessageSN(reader.readNullable(Integer.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(11);
        writer.write(getSrcNetworkID());
        writer.write(getNetworkTypeID());
        writer.writeNullable(getNetworkID());
        writer.write(getProofContextHash());
        writer.write(getProofContext());
        writer.write(getLastNetworkSectionHash());
        writer.write(getBmc());
        writer.write(getLastSequence());
        writer.write(getLastMessagesRoot());
        writer.write(getLastMessageCount());
        writer.write(getLastFirstMessageSN());
        writer.end();
    }

    public static BMVProperties fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMVProperties.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writeObject(writer);
        return writer.toByteArray();
    }
}
