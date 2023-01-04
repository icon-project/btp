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

package foundation.icon.btp.mock;

import score.Address;
import score.annotation.External;

import java.math.BigInteger;

public class MockGovImpl implements MockGov {

    static ChainScore chainScore() {
        return new ChainScoreInterface(Address.fromString(ChainScore.ADDRESS));
    }

    @External
    public void setRevision(int code) {
        chainScore().setRevision(code);
    }

    @External
    public void setMaxStepLimit(String contextType, BigInteger limit) {
        chainScore().setMaxStepLimit(contextType, limit);
    }

    @External
    public long openBTPNetwork(String networkTypeName, String name, Address owner) {
        return chainScore().openBTPNetwork(networkTypeName, name, owner);
    }

    @External
    public void closeBTPNetwork(long id) {
        chainScore().closeBTPNetwork(id);
    }

}
