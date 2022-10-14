/*
 * Copyright 2022 ICON Foundation
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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class ResponseMessage {
    public static final long CODE_SUCCESS = 0;
    public static final long CODE_UNKNOWN = 1;
    public static final long CODE_NO_ROUTE = 2;
    public static final long CODE_NO_BSH = 3;
    public static final long CODE_REVERT = 4;

    private long code;
    private String msg;

    public ResponseMessage() {
    }

    public ResponseMessage(long code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResponseMessage{");
        sb.append("code=").append(code);
        sb.append(", msg='").append(msg).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, ResponseMessage obj) {
        obj.writeObject(writer);
    }

    public static ResponseMessage readObject(ObjectReader reader) {
        ResponseMessage obj = new ResponseMessage();
        reader.beginList();
        obj.setCode(reader.readLong());
        obj.setMsg(reader.readNullable(String.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getCode());
        writer.writeNullable(this.getMsg());
        writer.end();
    }

    public static ResponseMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ResponseMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        ResponseMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
