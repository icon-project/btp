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

import score.*;
import scorex.util.ArrayList;

import java.util.List;

public class ValidatorList {

    final static String RLPn = "RLPn";

    private final List<byte[]> validators;
    private byte[] hash;

    public ValidatorList(List<byte[]> validators) {
        this.validators = validators;
    }

    public static ValidatorList fromBytes(byte[] serialized) {
        if (serialized == null)
            return new ValidatorList(new ArrayList<byte[]>());
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        var validators = new ArrayList<byte[]>();

        while (reader.hasNext())
            validators.add(reader.readByteArray());
        reader.end();
        ValidatorList validatorList = new ValidatorList(validators);
        validatorList.hash = Context.hash("keccak-256", serialized);
        return validatorList;
    }

    public static ValidatorList fromAddressBytes(byte[][] bytes) {
        List<byte[]> validators = new ArrayList<>();
        for (int i = 0; i < bytes.length; i++)
            validators.add(bytes[i]);
        return new ValidatorList(validators);
    }

    public static byte[] formatAddress(byte[] addr) {
        if (addr.length == 21)
            return addr;
        var ba2 = new byte[addr.length + 1];
        System.arraycopy(addr, 0, ba2, 1, addr.length);
        ba2[0] = 0;//todo: check
        return ba2;
    }

    // Below methods for javaloop to serialize
    public static void writeObject(ObjectWriter w, ValidatorList obj) {
        List<byte[]> validators = obj.getValidators();
        w.beginList(validators.size());

        for (int i = 0; i < validators.size(); i++) {
            w.write(validators.get(i));
        }

        w.end();
    }

    public static ValidatorList readObject(ObjectReader r) {
        r.beginList();
        List<byte[]> validators = new ArrayList<>();
        while (r.hasNext()) {
            byte[] v = r.readByteArray();
            validators.add(v);
        }
        r.end();
        return new ValidatorList(validators);
    }

    public List<byte[]> getValidators() {
        return validators;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter(RLPn);
        w.beginList(validators.size());
        for (byte[] address : validators)
            w.write(address);
        w.end();
        return w.toByteArray();
    }

    public byte[] getHash() {
        if (hash == null) {
            hash = Context.hash("sha3-256", toBytes());
        }
        return hash;
    }

    public boolean contains(Address addr) {
        for (byte[] validator : validators) {
            if (validator.equals(addr)) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(Address addr) {
        for (int i = 0; i < validators.size(); i++) {
            if (validators.get(i).equals(addr)) {
                return i;
            }
        }
        return -1;
    }

    public int size() {
        return validators.size();
    }
}
