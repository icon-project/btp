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

package foundation.icon.btp.xcall;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssertCallService {
    public static void assertEqualsCSMessage(CSMessage exp, CSMessage got) {
        assertEquals(exp.getType(), got.getType());
        assertArrayEquals(exp.getData(), got.getData());
    }

    public static void assertEqualsCSMessageRequest(CSMessageRequest exp, CSMessageRequest got) {
//        org.web3j.crypto.Keys.toChecksumAddress()
        assertEquals(exp.getFrom().toLowerCase(), got.getFrom().toLowerCase());
        assertEquals(exp.getTo().toLowerCase(), got.getTo().toLowerCase());
        assertArrayEquals(exp.getData(), got.getData());
    }

    public static void assertEqualsCSMessageResponse(CSMessageResponse exp, CSMessageResponse got) {
        assertEquals(exp.getSn(), got.getSn());
        assertEquals(exp.getCode(), got.getCode());
        assertEquals(exp.getMsg(), got.getMsg());
    }
}
