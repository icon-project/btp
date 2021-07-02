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

package com.iconloop.btp.nativecoin;

import com.iconloop.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class TransferRequest {
    private String from;
    private String to;
    private Asset[] assets;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public Asset[] getAssets() {
        return assets;
    }

    public void setAssets(Asset[] assets) {
        this.assets = assets;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferRequest{");
        sb.append("from='").append(from).append('\'');
        sb.append(", to='").append(to).append('\'');
        sb.append(", assets=").append(StringUtil.toString(assets));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, TransferRequest obj) {
        obj.writeObject(writer);
    }

    public static TransferRequest readObject(ObjectReader reader) {
        TransferRequest obj = new TransferRequest();
        reader.beginList();
        obj.setFrom(reader.readNullable(String.class));
        obj.setTo(reader.readNullable(String.class));
        if (reader.beginNullableList()) {
            Asset[] assets = null;
            List<Asset> assetsList = new ArrayList<>();
            while(reader.hasNext()) {
                assetsList.add(reader.readNullable(Asset.class));
            }
            assets = new Asset[assetsList.size()];
            for(int i=0; i<assetsList.size(); i++) {
                assets[i] = (Asset)assetsList.get(i);
            }
            obj.setAssets(assets);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.writeNullable(this.getFrom());
        writer.writeNullable(this.getTo());
        Asset[] assets = this.getAssets();
        if (assets != null) {
            writer.beginNullableList(assets.length);
            for(Asset v : assets) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static TransferRequest fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return TransferRequest.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        TransferRequest.writeObject(writer, this);
        return writer.toByteArray();
    }
}
