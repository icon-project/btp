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

public class NCSMessage {
    public static int REQUEST_COIN_TRANSFER = 0;
    public static int REQUEST_COIN_REGISTER = 1;
    public static int REPONSE_HANDLE_SERVICE = 2;
    public static int UNKNOWN_TYPE = 3;

    private int serviceType;
    private byte[] data;

    public int getServiceType() {
        return serviceType;
    }

    public void setServiceType(int serviceType) {
        this.serviceType = serviceType;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ServiceMessage{");
        sb.append("serviceType='").append(serviceType).append('\'');
        sb.append(", data=").append(StringUtil.toString(data));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, NCSMessage obj) {
        obj.writeObject(writer);
    }

    public static NCSMessage readObject(ObjectReader reader) {
        NCSMessage obj = new NCSMessage();
        reader.beginList();
        obj.setServiceType(reader.readInt());
        obj.setData(reader.readNullable(byte[].class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getServiceType());
        writer.writeNullable(this.getData());
        writer.end();
    }

    public static NCSMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return NCSMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        NCSMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
