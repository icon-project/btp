/*
 * Copyright 2020 ICONLOOP Inc.
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

package score;

public class Address {
    public static final int LENGTH = 21;
    private final byte[] raw = new byte[LENGTH];

    public Address(byte[] raw) throws IllegalArgumentException {
        if (raw == null) {
            throw new NullPointerException();
        }
        if (raw.length != LENGTH) {
            throw new IllegalArgumentException();
        }
        System.arraycopy(raw, 0, this.raw, 0, LENGTH);
    }

    public static Address fromString(String str) {
        if (str == null) {
            throw new NullPointerException();
        }
        if (str.length() != LENGTH * 2) {
            throw new IllegalArgumentException();
        }
        if (str.startsWith("hx") || str.startsWith("cx")) {
            byte[] bytes = new byte[LENGTH];
            bytes[0] = (byte) (str.startsWith("hx") ? 0x0 : 0x1);
            for (int i = 1; i < LENGTH; i++) {
                int j = i * 2;
                bytes[i] = (byte) Integer.parseInt(str.substring(j, j + 2), 16);
            }
            return new Address(bytes);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public boolean isContract() {
        return this.raw[0] == 0x1;
    }

    public byte[] toByteArray() {
        byte[] copy = new byte[LENGTH];
        System.arraycopy(this.raw, 0, copy, 0, LENGTH);
        return copy;
    }

    @Override
    public int hashCode() {
        int code = 0;
        for (byte b : this.raw) {
            code += b;
        }
        return code;
    }

    @Override
    public boolean equals(Object obj) {
        boolean isEqual = this == obj;
        if (!isEqual && (obj instanceof Address)) {
            Address other = (Address) obj;
            isEqual = true;
            for (int i = 0; isEqual && (i < LENGTH); ++i) {
                isEqual = (this.raw[i] == other.raw[i]);
            }
        }
        return isEqual;
    }

    @Override
    public String toString() {
        byte prefix = this.raw[0];
        byte[] body = new byte[LENGTH - 1];
        System.arraycopy(this.raw, 1, body, 0, body.length);
        return ((prefix == 0x0) ? "hx" : "cx") + toHexString(body);
    }

    private static String toHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = hexArray[v >>> 4];
            hexChars[i * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static final char[] hexArray = "0123456789abcdef".toCharArray();
}
