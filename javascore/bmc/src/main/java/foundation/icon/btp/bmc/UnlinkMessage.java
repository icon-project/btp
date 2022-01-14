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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BTPAddress;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class UnlinkMessage {
    private BTPAddress link;

    public BTPAddress getLink() {
        return link;
    }

    public void setLink(BTPAddress link) {
        this.link = link;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UnlinkMessage{");
        sb.append("link=").append(link);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, UnlinkMessage obj) {
        obj.writeObject(writer);
    }

    public static UnlinkMessage readObject(ObjectReader reader) {
        UnlinkMessage obj = new UnlinkMessage();
        reader.beginList();
        obj.setLink(reader.readNullable(BTPAddress.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(1);
        writer.writeNullable(this.getLink());
        writer.end();
    }

    public static UnlinkMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return UnlinkMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        UnlinkMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
