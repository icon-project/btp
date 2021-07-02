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
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class Result {
    private byte[] stateHash;
    private byte[] patchReceiptHash;
    private byte[] receiptHash;

    public byte[] getStateHash() {
        return stateHash;
    }

    public void setStateHash(byte[] stateHash) {
        this.stateHash = stateHash;
    }

    public byte[] getPatchReceiptHash() {
        return patchReceiptHash;
    }

    public void setPatchReceiptHash(byte[] patchReceiptHash) {
        this.patchReceiptHash = patchReceiptHash;
    }

    public byte[] getReceiptHash() {
        return receiptHash;
    }

    public void setReceiptHash(byte[] receiptHash) {
        this.receiptHash = receiptHash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Result{");
        sb.append("stateHash=").append(StringUtil.bytesToHex(stateHash));
        sb.append(", patchReceiptHash=").append(StringUtil.bytesToHex(patchReceiptHash));
        sb.append(", receiptHash=").append(StringUtil.bytesToHex(receiptHash));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Result obj) {
        obj.writeObject(writer);
    }

    public static Result readObject(ObjectReader reader) {
        Result obj = new Result();
        reader.beginList();
        obj.setStateHash(reader.readNullable(byte[].class));
        obj.setPatchReceiptHash(reader.readNullable(byte[].class));
        obj.setReceiptHash(reader.readNullable(byte[].class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.writeNullable(this.getStateHash());
        writer.writeNullable(this.getPatchReceiptHash());
        writer.writeNullable(this.getReceiptHash());
        writer.end();
    }

    public static Result fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Result.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Result.writeObject(writer, this);
        return writer.toByteArray();
    }
}
