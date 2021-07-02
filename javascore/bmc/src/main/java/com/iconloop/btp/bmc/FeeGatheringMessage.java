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

package com.iconloop.btp.bmc;

import com.iconloop.btp.lib.BTPAddress;
import com.iconloop.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class FeeGatheringMessage {
    private BTPAddress fa;
    private String[] svcs;

    public BTPAddress getFa() {
        return fa;
    }

    public void setFa(BTPAddress fa) {
        this.fa = fa;
    }

    public String[] getSvcs() {
        return svcs;
    }

    public void setSvcs(String[] svcs) {
        this.svcs = svcs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FeeGatheringMessage{");
        sb.append("fa=").append(fa);
        sb.append(", svcs=").append(StringUtil.toString(svcs));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, FeeGatheringMessage obj) {
        obj.writeObject(writer);
    }

    public static FeeGatheringMessage readObject(ObjectReader reader) {
        FeeGatheringMessage obj = new FeeGatheringMessage();
        reader.beginList();
        obj.setFa(reader.readNullable(BTPAddress.class));
        if (reader.beginNullableList()) {
            String[] svcs = null;
            List<String> svcsList = new ArrayList<>();
            while(reader.hasNext()) {
                svcsList.add(reader.readNullable(String.class));
            }
            svcs = new String[svcsList.size()];
            for(int i=0; i<svcsList.size(); i++) {
                svcs[i] = (String)svcsList.get(i);
            }
            obj.setSvcs(svcs);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.writeNullable(this.getFa());
        String[] svcs = this.getSvcs();
        if (svcs != null) {
            writer.beginNullableList(svcs.length);
            for(String v : svcs) {
                writer.write(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static FeeGatheringMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return FeeGatheringMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        FeeGatheringMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
