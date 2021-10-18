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
package foundation.icon.btp.bmv.types;

import foundation.icon.btp.bmv.lib.ExtraDataTypeDecoder;
import foundation.icon.btp.bmv.lib.HexConverter;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;

public class BlockHeader {

    final static String RLPn = "RLPn";
    private final BigInteger network; // extra appended as part of binance smart chains parlia engine
    private final byte[] parentHash;
    private final byte[] uncleHash;
    private final byte[] coinBase;
    private final byte[] stateRoot;
    private final byte[] transactionsRoot;
    private final byte[] receiptsRoot;
    private final byte[] logsBloom;
    private final BigInteger difficulty;
    private final BigInteger number;
    private final long gasLimit;
    private final long gasUsed;
    private final long timestamp;
    private byte[] extraData;
    private final byte[] mixHash;
    private final byte[] nonce;
    private final byte[] hash;

    public BlockHeader(BigInteger network,
                       byte[] parentHash,
                       byte[] uncleHash,
                       byte[] coinBase,
                       byte[] stateRoot,
                       byte[] transactionsRoot,
                       byte[] receiptsRoot,
                       byte[] logsBloom,
                       BigInteger difficulty,
                       BigInteger number,
                       long gasLimit,
                       long gasUsed,
                       long timestamp,
                       byte[] extraData,
                       byte[] mixHash,
                       byte[] nonce,
                       byte[] hash) {
        this.network = network;
        this.parentHash = parentHash;
        this.uncleHash = uncleHash;
        this.coinBase = coinBase;
        this.stateRoot = stateRoot;
        this.transactionsRoot = transactionsRoot;
        this.receiptsRoot = receiptsRoot;
        this.logsBloom = logsBloom;
        this.difficulty = difficulty;
        this.number = number;
        this.gasLimit = gasLimit;
        this.gasUsed = gasUsed;
        this.timestamp = timestamp;
        this.extraData = extraData;
        this.mixHash = mixHash;
        this.nonce = nonce;
        this.hash = hash;
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        if (bytes == null)
            return null;
        byte[] hash = Context.hash("keccak-256", bytes);
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, bytes);
        reader.beginList();
        //TODO: check if needed still after adding headerBytes field with has the eth rlp encoding
        //BigInteger network = reader.readBigInteger();
        byte[] parentHash = reader.readByteArray();
        byte[] uncleHash = reader.readByteArray();
        byte[] coinBase = reader.readByteArray();
        byte[] stateRoot = reader.readByteArray();
        byte[] transactionsRoot = reader.readByteArray();
        byte[] receiptsRoot = reader.readByteArray();
        byte[] logsBloom = reader.readByteArray();
        BigInteger difficulty = reader.readBigInteger();
        BigInteger number = reader.readBigInteger();
        long gasLimit = reader.readLong();
        long gasUsed = reader.readLong();
        long timestamp = reader.readLong();
        byte[] extraData = reader.readByteArray();
        byte[] mixHash = reader.readByteArray();
        byte[] nonce = reader.readByteArray();
        reader.end();
        //TODO: change later the network param
        return new BlockHeader(BigInteger.valueOf(97),
                parentHash,
                uncleHash,
                coinBase,
                stateRoot,
                transactionsRoot,
                receiptsRoot,
                logsBloom,
                difficulty,
                number,
                gasLimit,
                gasUsed,
                timestamp,
                extraData,
                mixHash,
                nonce,
                hash);
    }


    public static byte[] toBytes(BlockHeader bh) {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter(RLPn);
        w.beginList(16);
        w.write(bh.getNetwork());
        w.write(bh.getParentHash());
        w.write(bh.getUncleHash());
        w.write(bh.getCoinBase());
        w.write(bh.getStateRoot());
        w.write(bh.getTransactionsRoot());
        w.write(bh.getReceiptsRoot());
        w.write(bh.getLogsBloom());
        w.write(bh.getDifficulty());
        w.write(bh.getNumber());
        w.write(bh.getGasLimit());
        w.write(bh.getGasUsed());
        w.write(bh.getTimestamp());
        w.write(bh.getExtraData());
        w.write(bh.getMixHash());
        w.write(bh.getNonce());
        w.end();
        return w.toByteArray();
    }

    //todo: commented out for testing in local now until we get proper bsc data
    public static boolean verifyValidatorSignature(byte[] evmHeader) {
        try {
            BlockHeader bh = BlockHeader.fromBytes(evmHeader);
            String coinbase = HexConverter.bytesToHex(bh.getCoinBase());
            ExtraDataTypeDecoder typeDecoder = new ExtraDataTypeDecoder(bh.getExtraData());
            byte[] modifiedExtraData = ExtraDataTypeDecoder.getBytes(0, bh.getExtraData().length - 65);
            // epoch block: 32 bytes of extraVanity + N*{20 bytes of validator address} + 65 bytes of signature
            // non epoch block: 32 bytes of extraVanity + 65 bytes of signature.
            byte[] signature = ExtraDataTypeDecoder.getBytes(32, 65);
            //Context.println("signature: " + HexConverter.bytesToHex(signature));
            bh.setExtraData(modifiedExtraData);
            byte[] modifiedHeaderBytes = BlockHeader.toBytes(bh);
            byte[] signedBH = Context.hash("keccak-256", modifiedHeaderBytes);
            //Context.println("signedContent: " + HexConverter.bytesToHex(signedBH));
            byte[] publicKey = Context.recoverKey("ecdsa-secp256k1", signedBH, signature, false);
            //Context.println("PK: " + HexConverter.bytesToHex(publicKey));
            byte[] pkwithoutPrefix = new byte[64];
            System.arraycopy(publicKey, 1, pkwithoutPrefix, 0, publicKey.length - 1);
            byte[] address = getAddressBytesFromKey(pkwithoutPrefix);
            Context.println("address1: " + HexConverter.bytesToHex(address));
            Context.println("coinbase: " + HexConverter.bytesToHex(bh.getCoinBase()));
            return Context.verifySignature("ecdsa-secp256k1", signedBH, signature, publicKey);
        } catch (Exception e) {
            return false;
        }
    }

    public static byte[] getAddressBytesFromKey(byte[] pubKey) {
        //checkArgument(pubKey.length == 32 || pubKey.length == 64, "Invalid key length");
        byte[] hash = Context.hash("keccak-256", pubKey);
        //Context.println("hashed pk:"+ HexConverter.bytesToHex(hash));
        byte[] address = new byte[20];
        System.arraycopy(hash, hash.length - 20, address, 0, 20);
        return address;
    }

    /*
    private static void checkArgument(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }*/

    public static String getRLPn() {
        return RLPn;
    }

    public BigInteger getNetwork() {
        return network;
    }

    public byte[] getUncleHash() {
        return uncleHash;
    }

    public long getGasLimit() {
        return gasLimit;
    }

    public long getGasUsed() {
        return gasUsed;
    }

    public byte[] getMixHash() {
        return mixHash;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public byte[] getParentHash() {
        return parentHash;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public byte[] getTransactionsRoot() {
        return transactionsRoot;
    }

    public byte[] getReceiptsRoot() {
        return receiptsRoot;
    }

    public BigInteger getDifficulty() {
        return difficulty;
    }

    public BigInteger getNumber() {
        return number;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getLogsBloom() {
        return logsBloom;
    }

    public byte[] getCoinBase() {
        return coinBase;
    }

    public byte[] getExtraData() {
        return extraData;
    }

    public void setExtraData(byte[] extraData) {
        this.extraData = extraData;
    }

    public byte[] getHash() {
        return hash;
    }
}
