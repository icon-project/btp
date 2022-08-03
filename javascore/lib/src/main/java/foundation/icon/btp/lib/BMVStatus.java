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

package foundation.icon.btp.lib;

import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import score.annotation.Keep;

public class BMVStatus {
    private long height;
    private byte[] extra;

    @Keep
    public long getHeight() {
        return height;
    }
    @Keep
    public void setHeight(long height) {
        this.height = height;
    }
    @Keep
    public byte[] getExtra() {
        return extra;
    }
    @Keep
    public void setExtra(byte[] extra) {
        this.extra = extra;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMVStatus{");
        sb.append("height=").append(height);
        sb.append(", extra=").append(StringUtil.toString(extra));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BMVStatus obj) {
        obj.writeObject(writer);
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getHeight());
        writer.write(this.getExtra());
        writer.end();
    }

    public static BMVStatus readObject(ObjectReader reader) {
        BMVStatus obj = new BMVStatus();
        reader.beginList();
        obj.setHeight(reader.readLong());
        obj.setExtra(reader.readByteArray());
        reader.end();
        return obj;
    }

    public static BMVStatus fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMVStatus.readObject(reader);
    }
}
