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

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public class ProofContext {
    private EthAddress[] validators;

    public ProofContext(EthAddress[] validators) {
        this.validators = validators;
    }

    public EthAddress[] getValidators() {
        return validators;
    }

    public boolean isValidator(EthAddress address) {
        for (EthAddress addr : validators) {
            if (addr.equals(address)) return true;
        }
        return false;
    }

    public static ProofContext readObject(ObjectReader reader) {
        reader.beginList();
        List<EthAddress> addressList = new ArrayList<>();
        reader.beginList();
        while(reader.hasNext()) {
            addressList.add(reader.read(EthAddress.class));
        }
        reader.end();
        int len = addressList.size();
        EthAddress[] addresses = new EthAddress[len];
        for (int i = 0; i < len; i++) {
            addresses[i] = addressList.get(i);
        }
        reader.end();
        return new ProofContext(addresses);
    }

    public static ProofContext fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ProofContext.readObject(reader);
    }
}
