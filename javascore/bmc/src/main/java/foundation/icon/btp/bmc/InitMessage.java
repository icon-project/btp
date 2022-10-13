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
import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class InitMessage {
    private BTPAddress[] links;

    public InitMessage() {
    }

    public InitMessage(BTPAddress[] links) {
        this.links = links;
    }

    public BTPAddress[] getLinks() {
        return links;
    }

    public void setLinks(BTPAddress[] links) {
        this.links = links;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("InitMessage{");
        sb.append("links=").append(StringUtil.toString(links));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, InitMessage obj) {
        obj.writeObject(writer);
    }

    public static InitMessage readObject(ObjectReader reader) {
        InitMessage obj = new InitMessage();
        reader.beginList();
        reader.beginList();
        BTPAddress[] links = null;
        List<BTPAddress> linksList = new ArrayList<>();
        while(reader.hasNext()) {
            linksList.add(reader.readNullable(BTPAddress.class));
        }
        links = new BTPAddress[linksList.size()];
        for(int i=0; i<linksList.size(); i++) {
            links[i] = (BTPAddress)linksList.get(i);
        }
        obj.setLinks(links);
        reader.end();
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(1);
        BTPAddress[] links = this.getLinks();
        writer.beginList(links.length);
        for(BTPAddress v : links) {
            writer.writeNullable(v);
        }
        writer.end();
        writer.end();
    }

    public static InitMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return InitMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        InitMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
