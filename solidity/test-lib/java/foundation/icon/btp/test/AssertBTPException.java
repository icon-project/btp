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

package foundation.icon.btp.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.icon.btp.lib.BTPException;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;
import org.web3j.protocol.exceptions.TransactionException;
import score.UserRevertException;
import score.UserRevertedException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AssertBTPException {
    public static UserRevertedException toUserRevertedException(TransactionException e) {
        return toUserRevertedException(
                AssertTransactionException.getReason(e)
                        .orElseThrow(() -> new RuntimeException("fail to get revert reason", e)));
    }
    public static UserRevertedException toUserRevertedException(String reason) {
        int code = 0;
        int idx = reason.indexOf(":");
        if (idx >= 0) {
            try {
                code = Integer.parseInt(reason.substring(0, idx).trim());
                reason = reason.substring(idx);
            } catch (NumberFormatException nfe) {
                System.out.printf("not found code in \"%s\"\n", reason);
            }
        }
        return new UserRevertedException(code, reason);
    }

    public static <T extends UserRevertedException> UserRevertedException assertUserReverted(T expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        TransactionException te = null;
        try {
            te = assertThrows(TransactionException.class, executable);
        } catch (AssertionFailedError e) {
            Throwable t = e.getCause();
            if (t.getClass().getName().equals(RuntimeException.class.getName()) &&
                    t.getCause() instanceof TransactionException) {
                te = (TransactionException)t.getCause();
            } else {
                throw e;
            }
        }
        UserRevertedException e = toUserRevertedException(te);
        assertEquals(expected.getCode(), e.getCode(), messageSupplier == null ?
                () -> String.format("assertUserReverted expected: %d, actual: %d", expected.getCode(), e.getCode()) :
                () -> messageSupplier.apply(e));
        return e;
    }

    public static UserRevertedException assertUserReverted(int expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        return assertUserReverted(new UserRevertedException(expected), executable, messageSupplier);
    }

    public static <T extends UserRevertException> UserRevertedException assertUserRevert(T expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        return assertUserReverted(expected.getCode(), executable, messageSupplier == null ?
                (e) -> String.format("assertUserRevertException expected:%s, actual: %s", expected, e.toString()) :
                messageSupplier);
    }

    public static <T extends BTPException> BTPException assertBTPException(T expected, Executable executable) {
        return BTPException.of(assertUserRevert(expected, executable,
                (e) -> String.format("assertBTPException expected: %s, actual: %s", expected, BTPException.of(e))));
    }

    public static <T extends BTPException> BTPException assertBTPException(T expected, Executable executable, Function<BTPException, String> messageSupplier) {
        return BTPException.of(assertUserRevert(expected, executable, (e) -> messageSupplier.apply(BTPException.of(e))));
    }
}
