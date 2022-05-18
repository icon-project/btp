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

package foundation.icon.btp.bmv.icon;

import foundation.icon.score.util.StringUtil;
import score.*;
import scorex.util.ArrayList;
import scorex.util.Base64;

import java.util.List;

public class Validators {
    private Address[] addresses;

    public Address[] getAddresses() {
        return addresses;
    }

    public void setAddresses(Address[] addresses) {
        this.addresses = addresses;
    }

    public boolean contains(Address target) {
        for(Address address : addresses) {
            if (address.equals(target)){
                return true;
            }
        }
        return false;
    }

    /**
     * Create instance using comma separated string or base64 url encoded string
     * which is a list of address of validator.
     *
     * @param str comma separated string or base64 url encoded string
     * @return Validators
     */
    public static Validators fromString(String str) {
        Validators validators;
        List<String> tokenized = StringUtil.tokenize(str, ',');
        try {
            Address[] addresses = new Address[tokenized.size()];
            for (int i=0; i<tokenized.size(); i++) {
                addresses[i] = Address.fromString(tokenized.get(i));
            }
            validators = new Validators();
            validators.setAddresses(addresses);
        } catch (IllegalArgumentException e) {
            if (tokenized.size() == 1) {
                //try decode
                byte[] bytes = Base64.getDecoder().decode(str.getBytes());
                validators = fromBytes(bytes);
            } else {
                throw e;
            }
        }
        return validators;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Validators{");
        sb.append("addresses=").append(StringUtil.toString(addresses));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Validators obj) {
        obj.writeObject(writer);
    }

    public static Validators readObject(ObjectReader reader) {
        Validators obj = new Validators();
        reader.beginList();
        Address[] addresses = null;
        List<Address> addressesList = new ArrayList<>();
        while(reader.hasNext()) {
            addressesList.add(reader.readNullable(Address.class));
        }
        addresses = new Address[addressesList.size()];
        for(int i=0; i<addressesList.size(); i++) {
            addresses[i] = addressesList.get(i);
        }
        obj.setAddresses(addresses);
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        Address[] addresses = this.getAddresses();
        writer.beginList(addresses.length);
        for(Address v : addresses) {
            writer.write(v);
        }
        writer.end();
    }

    public static Validators fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Validators.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Validators.writeObject(writer, this);
        return writer.toByteArray();
    }

}
