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

import score.Address;
import score.Context;
import score.ObjectReader;

public class ProofContext {
    private Address[] validators;

    public Address[] getValidators() {
        return validators;
    }

    public void setValidators(Address[] validators) {
        this.validators = validators;
    }

    public boolean isValidator(Address address) {
        Address[] validators = this.validators;
        for (Address addr : validators) {
            if (addr.equals(address)) return true;
        }
        return false;
    }

    public static ProofContext readObject(ObjectReader reader) {
        reader.beginList();
        ProofContext obj = new ProofContext();
        obj.setValidators(reader.read(Address[].class));
        reader.end();
        return obj;
    }

    public static ProofContext fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ProofContext.readObject(reader);
    }
}
