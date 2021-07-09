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
package foundation.icon.btp.bmv.types;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

/**
 * Receipt Structure from geth
 * Type              uint8  `json:"type,omitempty"`
 * PostState         []byte `json:"root"`
 * Status            uint64 `json:"status"`
 * CumulativeGasUsed uint64 `json:"cumulativeGasUsed" gencodec:"required"`
 * Bloom             Bloom  `json:"logsBloom"         gencodec:"required"`
 * Logs              []*Log `json:"logs"              gencodec:"required"`
 * https://pkg.go.dev/github.com/ethereum/go-ethereum/core/types#Log
 */

public class Receipt {

    final static String RLPn = "RLPn";
    private final int status;
    private final byte[] logsBloom;
    private final List<ReceiptEventLog> logs;

    public Receipt(int status, byte[] logsBloom, List<ReceiptEventLog> logs) {
        this.status = status;
        this.logsBloom = logsBloom;
        this.logs = logs;
    }

    public static Receipt fromBytes(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();
        if (!reader.hasNext())
            return new Receipt(0, new byte[]{0}, new ArrayList<>());

        //status
        int status = reader.readInt();
        reader.skip();
        //logsBloom
        byte[] logsBloom = reader.readByteArray();
        //logs
        List<ReceiptEventLog> eventLogs = new ArrayList<>();
        reader.beginList();
        while (reader.hasNext()) {
            ReceiptEventLog eventLog = ReceiptEventLog.readObject(reader);
            eventLogs.add(eventLog);
        }
        reader.end();
        reader.end();
        return new Receipt(status, logsBloom, eventLogs);
    }


    public static Receipt readObject(ObjectReader reader) {
        reader.beginList();
        if (!reader.hasNext())
            return new Receipt(0, new byte[]{0}, new ArrayList<>());

        //status
        int status = reader.readInt();
        //logsBloom
        byte[] logsBloom = reader.readByteArray();
        //logs
        List<ReceiptEventLog> eventLogs = new ArrayList<>();
        while (reader.hasNext()) {
            ReceiptEventLog eventLog = ReceiptEventLog.readObject(reader);
            eventLogs.add(eventLog);
        }
        reader.end();
        return new Receipt(status, logsBloom, eventLogs);
    }


    public void setEventLogsWithProofs(List<EventProof> eventProofs) {
        List<ReceiptEventLog> eventLogs = new ArrayList<>();
        for (EventProof eventProof : eventProofs) {
            // eventLog= eventProof.prove()
            //TODO: check if the logs should be set by proving? where to find the event_logs hash?
        }
    }

    public int getStatus() {
        return status;
    }

    public byte[] getLogsBloom() {
        return logsBloom;
    }

    public List<ReceiptEventLog> getLogs() {
        return logs;
    }

}
