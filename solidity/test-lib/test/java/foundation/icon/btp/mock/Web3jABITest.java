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

import foundation.icon.btp.test.EVMIntegrationTest;
import org.junit.jupiter.api.Test;
import org.web3j.abi.datatypes.generated.Uint256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Web3jABITest {
    static TestWeb3jABI contract = EVMIntegrationTest.deploy(TestWeb3jABI.class);

    @SuppressWarnings("unchecked")
    static List<List<BigInteger>> get2DArray(List<String> _key) throws Exception {
        return ((List<List<Uint256>>)contract.get2DArray(_key).send()).stream().map(
                (l) -> l.stream().map(
                        Uint256::getValue
                ).collect(Collectors.toList())
        ).collect(Collectors.toList());
    }


    @Test
    void setWith2DArray() throws Exception {
        List<String> _key = List.of("Test");
        List<List<BigInteger>> _value = List.of(
                List.of(
                        BigInteger.valueOf(10),
                        BigInteger.valueOf(11)
                )
        );
        contract.setWith2DArray(_key, _value).send();
        assertEquals(_value, get2DArray(_key));
    }

    @Test
    void setWith2DArrayUsingEmptyArray() throws Exception {
        List<String> _key = List.of("Remove", "Test");
        List<List<BigInteger>> _value = List.of(
                new ArrayList<>(),
                List.of(
                        BigInteger.valueOf(10),
                        BigInteger.valueOf(11)
                )
        );
        contract.setWith2DArray(_key, _value).send();
        assertEquals(_value, get2DArray(_key));
    }
}
