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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class TransferResponse {
    public static BigInteger RC_OK = BigInteger.ZERO;
    public static BigInteger RC_ERR = BigInteger.ONE;
    public static String OK_MSG = "Transfer Success";
    public static String ERR_MSG_UNKNOWN_TYPE = "UNKNOWN_TYPE";

    private BigInteger code;
    private String message;

    public BigInteger getCode() {
        return code;
    }

    public void setCode(BigInteger code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferResponse{");
        sb.append("code=").append(code);
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }


    public static void writeObject(ObjectWriter writer, TransferResponse obj) {
        obj.writeObject(writer);
    }

    public static TransferResponse readObject(ObjectReader reader) {
        TransferResponse obj = new TransferResponse();
        reader.beginList();
        obj.setCode(reader.readBigInteger());
        obj.setMessage(reader.readNullable(String.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getCode());
        writer.writeNullable(this.getMessage());
        writer.end();
    }

    public static TransferResponse fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return TransferResponse.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        TransferResponse.writeObject(writer, this);
        return writer.toByteArray();
    }
}
