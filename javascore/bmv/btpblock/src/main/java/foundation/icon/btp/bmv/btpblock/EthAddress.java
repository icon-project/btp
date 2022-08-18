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

package foundation.icon.btp.bmv.btpblock;

import score.ObjectReader;
import score.ObjectWriter;

import java.util.Arrays;

public class EthAddress {
    private byte[] data;
    public static final int ADDRESS_LEN = 20;

    public EthAddress(byte[] data) {
        if (data.length != ADDRESS_LEN) throw BMVException.unknown("invalid Address data length");
        this.data = data;
    }

    public static EthAddress readObject(ObjectReader r) {
        return new EthAddress(r.readByteArray());
    }

    public void writeObject(ObjectWriter writer) {
        writer.write(data);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EthAddress that = (EthAddress) o;
        return Arrays.equals(data, that.data);
    }

}
