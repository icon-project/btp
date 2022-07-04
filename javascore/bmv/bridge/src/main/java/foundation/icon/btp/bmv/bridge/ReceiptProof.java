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
package foundation.icon.btp.bmv.bridge;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class ReceiptProof {

    final static String RLPn = "RLPn";

    private final int index;
    private final List<EventDataBTPMessage> events;
    private final BigInteger height;

    public ReceiptProof(int index, List<EventDataBTPMessage> events, BigInteger height) {
        this.index = index;
        this.events = events;
        this.height = height;
    }

    public static ReceiptProof fromBytes(byte[] serialized) {
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();
        //Index
        int index = reader.readInt();
        List<EventDataBTPMessage> eventsLogs = new ArrayList<>();

        ObjectReader eventLogReader = Context.newByteArrayObjectReader(RLPn, reader.readByteArray());
        eventLogReader.beginList();
        while (eventLogReader.hasNext()) {
            eventsLogs.add(EventDataBTPMessage.fromRLPBytes(eventLogReader));
        }
        eventLogReader.end();

        BigInteger height = reader.readBigInteger();
        return new ReceiptProof(index, eventsLogs, height);
    }

    public int getIndex() {
        return index;
    }

    public List<EventDataBTPMessage> getEvents() {
        return events;
    }

    public BigInteger getHeight() {
        return height;
    }
}
