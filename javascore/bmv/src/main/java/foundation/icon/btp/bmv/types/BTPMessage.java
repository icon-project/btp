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

public class BTPMessage {
    final static String RLPn = "RLPn";
    private String src;
    private String dst;
    private String srv;
    private int sn;
    private byte[] msg;

    public BTPMessage(String src, String dst, String srv, int sn, byte[] msg) {
        this.src = src;
        this.dst = dst;
        this.srv = srv;
        this.sn = sn;
        this.msg = msg;
    }

    public static BTPMessage fromBytes(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        //src
        String src = reader.readString();
        //dst
        String dst = reader.readString();
        //dst
        String svc = reader.readString();
        //sr.no
        int sno = reader.readInt();
        //msg
        byte[] msg = reader.readByteArray();
        reader.end();
        return new BTPMessage(src, dst, svc, sno, msg);
    }

    public String getSrc() {
        return src;
    }

    public String getDst() {
        return dst;
    }

    public String getSrv() {
        return srv;
    }

    public int getSn() {
        return sn;
    }

    public byte[] getMsg() {
        return msg;
    }
}
