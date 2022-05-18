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

package foundation.icon.btp.bmv.btp;

import score.ObjectReader;
import score.ObjectWriter;

import java.util.Arrays;

public class Address {
    private byte[] data;
    public static final int ADDRESS_LEN = 20;

    public Address(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public static Address readObject(ObjectReader r) {
        r.beginList();
        Address obj = new Address(r.readByteArray());
        r.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(1);
        writer.write(data);
        writer.end();
    }

    public boolean equal(Address other) {
        return Arrays.equals(data, other.data);
    }
}
