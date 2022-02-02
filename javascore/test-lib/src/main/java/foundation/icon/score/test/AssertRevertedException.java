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

package foundation.icon.score.test;

import foundation.icon.jsonrpc.JsonrpcClient;
import foundation.icon.score.client.RevertedException;
import org.junit.jupiter.api.function.Executable;
import score.UserRevertException;
import score.UserRevertedException;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class AssertRevertedException {

    public static RevertedException assertReverted(int expected, Executable executable) {
        return assertReverted(new RevertedException(expected, ""), executable);
    }

    public static RevertedException assertReverted(RevertedException expected, Executable executable) {
        RevertedException e = assertThrows(RevertedException.class, executable);
        assertEquals(expected.getCode(), e.getCode(),
                String.format("assertReverted expected: %d, actual: %d", expected.getCode(), e.getCode()));
        return e;
    }

    public static UserRevertedException assertUserReverted(int expected, Executable executable) {
        return assertUserReverted(expected, executable, null);
    }

    public static UserRevertedException assertUserReverted(int expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        return assertUserReverted(new UserRevertedException(expected), executable, messageSupplier);
    }

    public static <T extends UserRevertedException> UserRevertedException assertUserReverted(T expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        UserRevertedException e = assertThrows(UserRevertedException.class, executable);
        assertEquals(expected.getCode(), e.getCode(), messageSupplier == null ?
                () -> String.format("assertUserReverted expected: %d, actual: %d", expected.getCode(), e.getCode()) :
                () -> messageSupplier.apply(e));
        return e;
    }

    public static <T extends UserRevertException> UserRevertedException assertUserRevert(T expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        return assertUserReverted(expected.getCode(), executable, messageSupplier == null ?
                (e) -> String.format("assertUserRevertException expected:%s, actual: %s", expected, e.toString()) :
                messageSupplier);
    }

    public static UserRevertedException assertUserRevertedFromJsonrpcError(int expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        return assertUserRevertedFromJsonrpcError(new UserRevertedException(expected), executable, messageSupplier);
    }

    public static <T extends UserRevertedException> UserRevertedException assertUserRevertedFromJsonrpcError(T expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        JsonrpcClient.JsonrpcError rpcError = assertThrows(JsonrpcClient.JsonrpcError.class, executable);
        //ErrorCodeScore(30000) + UserReverted(32)
        assertTrue(rpcError.getCode() <= -30032 && rpcError.getCode() > -31000);
        UserRevertedException e = new UserRevertedException(-1 * ((int)rpcError.getCode() + 30032), rpcError.getMessage());
        assertEquals(expected.getCode(), e.getCode(), messageSupplier == null ?
                () -> String.format("assertUserRevertedFromJsonrpcError expected: %d, actual: %d", expected.getCode(), e.getCode()) :
                () -> messageSupplier.apply(e));
        return e;
    }

    public static <T extends UserRevertException> UserRevertedException assertUserRevertFromJsonrpcError(T expected, Executable executable, Function<UserRevertedException, String> messageSupplier) {
        return assertUserRevertedFromJsonrpcError(expected.getCode(), executable, messageSupplier == null ?
                (e) -> String.format("assertUserRevertFromJsonrpcError expected:%s, actual: %s", expected, e.toString()) :
                messageSupplier);
    }
}
