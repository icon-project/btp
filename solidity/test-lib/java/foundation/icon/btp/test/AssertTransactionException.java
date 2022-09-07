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

package foundation.icon.btp.test;

import org.junit.jupiter.api.function.Executable;
import org.web3j.protocol.exceptions.TransactionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssertTransactionException {
    public static TransactionException assertRevertReason(String expected, Executable executable) {
        TransactionException e = assertThrows(TransactionException.class, executable);
        if (expected != null) {
            String reason = e.getTransactionReceipt().orElseThrow().getRevertReason();
            if (reason.startsWith("execution reverted: ")) {
                reason = reason.substring("execution reverted: ".length());
            }
            assertEquals(expected, reason);
        }
        return e;
    }
}
