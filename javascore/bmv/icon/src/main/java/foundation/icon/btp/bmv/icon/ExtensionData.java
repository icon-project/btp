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

package foundation.icon.btp.bmv.icon;

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class ExtensionData {
    private byte[][] data;

    public byte[][] getData() {
        return data;
    }

    public void setData(byte[][] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ExtensionData{");
        sb.append("data=").append(StringUtil.toString(data));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, ExtensionData obj) {
        obj.writeObject(writer);
    }

    public static ExtensionData readObject(ObjectReader reader) {
        ExtensionData obj = new ExtensionData();
        if (reader.beginNullableList()) {
            byte[][] data = null;
            List<byte[]> dataList = new ArrayList<>();
            while(reader.hasNext()) {
                dataList.add(reader.readNullable(byte[].class));
            }
            data = new byte[dataList.size()][];
            for(int i=0; i<dataList.size(); i++) {
                data[i] = (byte[])dataList.get(i);
            }
            obj.setData(data);
            reader.end();
        }
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        byte[][] data = this.getData();
        if (data != null) {
            writer.beginNullableList(data.length);
            for(byte[] v : data) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
    }

    public static ExtensionData fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ExtensionData.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        ExtensionData.writeObject(writer, this);
        return writer.toByteArray();
    }
}
