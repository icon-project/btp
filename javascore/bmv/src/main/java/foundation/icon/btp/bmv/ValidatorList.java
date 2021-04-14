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
package foundation.icon.btp.bmv;

import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

public class ValidatorList {

    final static String RLPn = "RLPn";

    private final Address[] validators;
    private byte[] hash;

    public ValidatorList(Address[] validators) {
        this.validators = validators;
    }

    public static ValidatorList fromBytes(byte[] serialized) {
        if (serialized == null)
            return new ValidatorList(new Address[]{});
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        var validators = new ArrayList<Address>();

        while(reader.hasNext())
            validators.add(new Address(formatAddress(reader.readByteArray())));
        reader.end();

        Address[] tmp = new Address[validators.size()];
        for (int i = 0; i < tmp.length; i++)
            tmp[i] = validators.get(i);

        ValidatorList validatorList = new ValidatorList(tmp);
        validatorList.hash = Context.sha3_256(serialized);
        return validatorList;
    }

    public static ValidatorList fromAddressBytes(byte[][] bytes) {
        Address[] validators = new Address[bytes.length];
        for (int i = 0; i < bytes.length; i++)
            validators[i] = new Address(formatAddress(bytes[i]));
        return new ValidatorList(validators);
    }

    public static byte[] formatAddress(byte[] addr){
        if(addr.length == 21)
            return addr;
        var ba2 = new byte[addr.length+1];
        System.arraycopy(addr, 0, ba2, 1, addr.length);
        ba2[0] = 1;
        return ba2;
    }

    public Address[] getValidators() {
        return validators;
    }

    public byte[] toBytes(){
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter(RLPn);
        w.beginList(validators.length);
        for(Address address:validators)
            w.write(address.toByteArray());
        w.end();
        return w.toByteArray();
    }

    public byte[] getHash(){
        if (hash == null){
           hash = Context.sha3_256(toBytes());
        }
        return hash;
    }

    public boolean contains(Address addr) {
        for (Address validator : validators) {
            if (validator.equals(addr)) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(Address addr) {
        for (int i = 0; i < validators.length; i++) {
            if (validators[i].equals(addr)) {
                return i;
            }
        }
        return -1;
    }

    public int size() {
        return validators.length;
    }
}
