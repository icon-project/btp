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
import scorex.util.ArrayList;

import java.util.List;

public class Votes {
    private long round;
    private PartSetId partSetId;
    private Vote[] items;

    public long getRound() {
        return round;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public PartSetId getPartSetId() {
        return partSetId;
    }

    public void setPartSetId(PartSetId partSetId) {
        this.partSetId = partSetId;
    }

    public Vote[] getItems() {
        return items;
    }

    public void setItems(Vote[] items) {
        this.items = items;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Votes{");
        sb.append("round=").append(round);
        sb.append(", partSetId=").append(partSetId);
        sb.append(", items=").append(StringUtil.toString(items));
        sb.append('}');
        return sb.toString();
    }


    public static void writeObject(ObjectWriter writer, Votes obj) {
        obj.writeObject(writer);
    }

    public static Votes readObject(ObjectReader reader) {
        Votes obj = new Votes();
        reader.beginList();
        obj.setRound(reader.readLong());
        obj.setPartSetId(reader.readNullable(PartSetId.class));
        reader.beginList();
        Vote[] items = null;
        List<Vote> itemsList = new ArrayList<>();
        while(reader.hasNext()) {
            itemsList.add(reader.read(Vote.class));
        }
        items = new Vote[itemsList.size()];
        for(int i=0; i<itemsList.size(); i++) {
            items[i] = (Vote)itemsList.get(i);
        }
        obj.setItems(items);
        reader.end();
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.getRound());
        PartSetId partSetId = this.getPartSetId();
        writer.writeNullable(partSetId);
        Vote[] items = this.getItems();
        writer.beginList(items.length);
        for(Vote v : items) {
            writer.write(v);
        }
        writer.end();
        writer.end();
    }

    public static Votes fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Votes.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Votes.writeObject(writer, this);
        return writer.toByteArray();
    }
}
