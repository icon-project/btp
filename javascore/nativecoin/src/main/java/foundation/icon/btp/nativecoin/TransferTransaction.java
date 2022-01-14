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

package foundation.icon.btp.nativecoin;

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class TransferTransaction {
    private String from;
    private String to;
    private AssetTransferDetail[] assets;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public AssetTransferDetail[] getAssets() {
        return assets;
    }

    public void setAssets(AssetTransferDetail[] assets) {
        this.assets = assets;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferTransaction{");
        sb.append("from='").append(from).append('\'');
        sb.append(", to='").append(to).append('\'');
        sb.append(", assets=").append(StringUtil.toString(assets));
        sb.append('}');
        return sb.toString();
    }
    
    public static void writeObject(ObjectWriter writer, TransferTransaction obj) {
        obj.writeObject(writer);
    }

    public static TransferTransaction readObject(ObjectReader reader) {
        TransferTransaction obj = new TransferTransaction();
        reader.beginList();
        obj.setFrom(reader.readNullable(String.class));
        obj.setTo(reader.readNullable(String.class));
        if (reader.beginNullableList()) {
            AssetTransferDetail[] assets = null;
            List<AssetTransferDetail> assetsList = new ArrayList<>();
            while(reader.hasNext()) {
                assetsList.add(reader.readNullable(AssetTransferDetail.class));
            }
            assets = new AssetTransferDetail[assetsList.size()];
            for(int i=0; i<assetsList.size(); i++) {
                assets[i] = (AssetTransferDetail)assetsList.get(i);
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
        AssetTransferDetail[] assets = this.getAssets();
        if (assets != null) {
            writer.beginNullableList(assets.length);
            for(AssetTransferDetail v : assets) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static TransferTransaction fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return TransferTransaction.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        TransferTransaction.writeObject(writer, this);
        return writer.toByteArray();
    }

}
