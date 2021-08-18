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

import foundation.icon.btp.lib.BTPException;
import org.junit.jupiter.api.function.Executable;

import java.util.function.Function;

import static foundation.icon.score.test.AssertRevertedException.assertUserRevert;

public class AssertBTPException {
    public static <T extends BTPException> BTPException assertBTPException(T expected, Executable executable) {
        return BTPException.of(assertUserRevert(expected, executable,
                (e) -> String.format("assertBTPException expected: %s, actual: %s", expected, BTPException.of(e))));
    }

    public static <T extends BTPException> BTPException assertBTPException(T expected, Executable executable, Function<BTPException, String> messageSupplier) {
        return BTPException.of(assertUserRevert(expected, executable, (e) -> messageSupplier.apply(BTPException.of(e))));
    }
}
