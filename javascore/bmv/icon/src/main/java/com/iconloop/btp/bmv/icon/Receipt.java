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

public class Receipt {
    private long status;
    private byte[] to;
    private byte[] cumulativeStepUsed;
    private byte[] stepUsed;
    private byte[] stepPrice;
    private byte[] logsBloom;
    private EventLog[] eventLogs;
    private byte[] scoreAddress;
    private byte[] eventLogsHash;

    public long getStatus() {
        return status;
    }

    public void setStatus(long status) {
        this.status = status;
    }

    public byte[] getTo() {
        return to;
    }

    public void setTo(byte[] to) {
        this.to = to;
    }

    public byte[] getCumulativeStepUsed() {
        return cumulativeStepUsed;
    }

    public void setCumulativeStepUsed(byte[] cumulativeStepUsed) {
        this.cumulativeStepUsed = cumulativeStepUsed;
    }

    public byte[] getStepUsed() {
        return stepUsed;
    }

    public void setStepUsed(byte[] stepUsed) {
        this.stepUsed = stepUsed;
    }

    public byte[] getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(byte[] stepPrice) {
        this.stepPrice = stepPrice;
    }

    public byte[] getLogsBloom() {
        return logsBloom;
    }

    public void setLogsBloom(byte[] logsBloom) {
        this.logsBloom = logsBloom;
    }

    public EventLog[] getEventLogs() {
        return eventLogs;
    }

    public void setEventLogs(EventLog[] eventLogs) {
        this.eventLogs = eventLogs;
    }

    public byte[] getScoreAddress() {
        return scoreAddress;
    }

    public void setScoreAddress(byte[] scoreAddress) {
        this.scoreAddress = scoreAddress;
    }

    public byte[] getEventLogsHash() {
        return eventLogsHash;
    }

    public void setEventLogsHash(byte[] eventLogsHash) {
        this.eventLogsHash = eventLogsHash;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Receipt{");
        sb.append("status=").append(status);
        sb.append(", to=").append(StringUtil.toString(to));
        sb.append(", cumulativeStepUsed=").append(StringUtil.toString(cumulativeStepUsed));
        sb.append(", stepUsed=").append(StringUtil.toString(stepUsed));
        sb.append(", stepPrice=").append(StringUtil.toString(stepPrice));
        sb.append(", logsBloom=").append(StringUtil.toString(logsBloom));
        sb.append(", eventLogs=").append(StringUtil.toString(eventLogs));
        sb.append(", scoreAddress=").append(StringUtil.toString(scoreAddress));
        sb.append(", eventLogsHash=").append(StringUtil.toString(eventLogsHash));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Receipt obj) {
        obj.writeObject(writer);
    }

    public static Receipt readObject(ObjectReader reader) {
        Receipt obj = new Receipt();
        reader.beginList();
        obj.setStatus(reader.readLong());
        obj.setTo(reader.readNullable(byte[].class));
        obj.setCumulativeStepUsed(reader.readNullable(byte[].class));
        obj.setStepUsed(reader.readNullable(byte[].class));
        obj.setStepPrice(reader.readNullable(byte[].class));
        obj.setLogsBloom(reader.readNullable(byte[].class));
        reader.beginList();
        EventLog[] eventLogs = null;
        List<EventLog> eventLogsList = new ArrayList<>();
        while(reader.hasNext()) {
            byte[] eventLogsElementBytes = reader.readNullable(byte[].class);
            if (eventLogsElementBytes != null) {
                ObjectReader eventLogsElementReader = Context.newByteArrayObjectReader("RLPn",eventLogsElementBytes);
                eventLogsList.add(eventLogsElementReader.read(EventLog.class));
            }
        }
        eventLogs = new EventLog[eventLogsList.size()];
        for(int i=0; i<eventLogsList.size(); i++) {
            eventLogs[i] = (EventLog)eventLogsList.get(i);
        }
        obj.setEventLogs(eventLogs);
        reader.end();
        obj.setScoreAddress(reader.readNullable(byte[].class));
        obj.setEventLogsHash(reader.readNullable(byte[].class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(9);
        writer.write(this.getStatus());
        writer.writeNullable(this.getTo());
        writer.writeNullable(this.getCumulativeStepUsed());
        writer.writeNullable(this.getStepUsed());
        writer.writeNullable(this.getStepPrice());
        writer.writeNullable(this.getLogsBloom());
        EventLog[] eventLogs = this.getEventLogs();
        if (eventLogs != null) {
            writer.beginNullableList(eventLogs.length);
            for(EventLog v : eventLogs) {
                ByteArrayObjectWriter vWriter = Context.newByteArrayObjectWriter("RLPn");
                vWriter.write(v);
                writer.write(vWriter.toByteArray());
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.writeNullable(this.getScoreAddress());
        writer.writeNullable(this.getEventLogsHash());
        writer.end();
    }

    public static Receipt fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Receipt.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Receipt.writeObject(writer, this);
        return writer.toByteArray();
    }
}
