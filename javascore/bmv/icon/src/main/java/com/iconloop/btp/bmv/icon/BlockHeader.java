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

package com.iconloop.btp.bmv.icon;

import com.iconloop.score.util.StringUtil;
import score.*;

public class BlockHeader {
    private long version;
    private long height;
    private long timestamp;
    private Address proposer;
    private byte[] prevHash;
    private byte[] voteHash;
    private byte[] nextValidatorHash;
    private byte[] patchTxHash;
    private byte[] txHash;
    private byte[] logsBloom;
    private Result result;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Address getProposer() {
        return proposer;
    }

    public void setProposer(Address proposer) {
        this.proposer = proposer;
    }

    public byte[] getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(byte[] prevHash) {
        this.prevHash = prevHash;
    }

    public byte[] getVoteHash() {
        return voteHash;
    }

    public void setVoteHash(byte[] voteHash) {
        this.voteHash = voteHash;
    }

    public byte[] getNextValidatorHash() {
        return nextValidatorHash;
    }

    public void setNextValidatorHash(byte[] nextValidatorHash) {
        this.nextValidatorHash = nextValidatorHash;
    }

    public byte[] getPatchTxHash() {
        return patchTxHash;
    }

    public void setPatchTxHash(byte[] patchTxHash) {
        this.patchTxHash = patchTxHash;
    }

    public byte[] getTxHash() {
        return txHash;
    }

    public void setTxHash(byte[] txHash) {
        this.txHash = txHash;
    }

    public byte[] getLogsBloom() {
        return logsBloom;
    }

    public void setLogsBloom(byte[] logsBloom) {
        this.logsBloom = logsBloom;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BlockHeader{");
        sb.append("version=").append(version);
        sb.append(", height=").append(height);
        sb.append(", timestamp=").append(timestamp);
        sb.append(", proposer=").append(proposer);
        sb.append(", prevHash=").append(StringUtil.bytesToHex(prevHash));
        sb.append(", voteHash=").append(StringUtil.bytesToHex(voteHash));
        sb.append(", nextValidatorHash=").append(StringUtil.bytesToHex(nextValidatorHash));
        sb.append(", patchTxHash=").append(StringUtil.bytesToHex(patchTxHash));
        sb.append(", txHash=").append(StringUtil.bytesToHex(txHash));
        sb.append(", logsBloom=").append(StringUtil.bytesToHex(logsBloom));
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BlockHeader obj) {
        obj.writeObject(writer);
    }

    public static BlockHeader readObject(ObjectReader reader) {
        BlockHeader obj = new BlockHeader();
        reader.beginList();
        obj.setVersion(reader.readLong());
        obj.setHeight(reader.readLong());
        obj.setTimestamp(reader.readLong());
        obj.setProposer(reader.readNullable(Address.class));
        obj.setPrevHash(reader.readNullable(byte[].class));
        obj.setVoteHash(reader.readNullable(byte[].class));
        obj.setNextValidatorHash(reader.readNullable(byte[].class));
        obj.setPatchTxHash(reader.readNullable(byte[].class));
        obj.setTxHash(reader.readNullable(byte[].class));
        obj.setLogsBloom(reader.readNullable(byte[].class));
        byte[] resultBytes = reader.readNullable(byte[].class);
        if (resultBytes != null) {
            ObjectReader resultReader = Context.newByteArrayObjectReader("RLPn",resultBytes);
            obj.setResult(resultReader.read(Result.class));
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(11);
        writer.write(this.getVersion());
        writer.write(this.getHeight());
        writer.write(this.getTimestamp());
        writer.writeNullable(this.getProposer());
        writer.writeNullable(this.getPrevHash());
        writer.writeNullable(this.getVoteHash());
        writer.writeNullable(this.getNextValidatorHash());
        writer.writeNullable(this.getPatchTxHash());
        writer.writeNullable(this.getTxHash());
        writer.writeNullable(this.getLogsBloom());
        Result result = this.getResult();
        if (result != null) {
            ByteArrayObjectWriter resultWriter = Context.newByteArrayObjectWriter("RLPn");
            resultWriter.write(result);
            writer.writeNullable(resultWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockHeader.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BlockHeader.writeObject(writer, this);
        return writer.toByteArray();
    }

}
