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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;

import java.util.ArrayList;
import java.util.List;

public class RelayMessage {

    final static String RLPn = "RLPn";

    private ArrayList<ReceiptProof> receiptProofs; //TODO: work on this link it with Event Logs & proof to get BTP message


    public RelayMessage(List<ReceiptProof> receiptProof) {
        this.receiptProofs = new ArrayList<>(receiptProof);
    }

    public static RelayMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, bytes);

        reader.beginList();
/*

        reader.beginList();
        var blockUpdates = new ArrayList<BlockUpdate>();
        while (reader.hasNext())
            blockUpdates.add(BlockUpdate.fromBytes(reader.readNullable(byte[].class)));
        reader.end();

        BlockProof blockProof = BlockProof.fromBytes(reader.readNullable(byte[].class));
*/

        reader.beginList();
        var receiptProofs = new ArrayList<ReceiptProof>();
        while (reader.hasNext())
            receiptProofs.add(ReceiptProof.fromBytes(reader.readByteArray()));
        reader.end();

        return new RelayMessage(
                receiptProofs);
    }

    public ReceiptProof[] getReceiptProofs() {
        ReceiptProof[] tmp = new ReceiptProof[receiptProofs.size()];
        for (int i = 0; i < tmp.length; i++) {
            tmp[i] = receiptProofs.get(i);
        }
        return tmp;
    }

    static byte[] toBytes(List<EventDataBTPMessage> events) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(events.size());
        for (EventDataBTPMessage event : events) {
            writer.beginList(3);
            writer.write(event.getNext_bmc());
            writer.write(event.getSeq());
            writer.write(event.getMsg());
            writer.end();
        }
        writer.end();
        return writer.toByteArray();
    }

    static byte[] toBytes(ReceiptProof rp) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(3);
        writer.write(rp.getIndex());
        writer.write(toBytes(rp.getEvents()));
        writer.write(rp.getHeight());
        writer.end();
        return writer.toByteArray();
    }

    static byte[] toBytes(RelayMessage rm) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(1);
        ReceiptProof[] rps = rm.getReceiptProofs();
        writer.beginList(rps.length);
        for(ReceiptProof rp : rps) {
            writer.write(toBytes(rp));
        }
        writer.end();
        writer.end();
        return writer.toByteArray();
    }

}
