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

package foundation.icon.btp.bmc;

import foundation.icon.btp.test.AssertBTPException;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("ThrowableNotThrown")
public class AssertBMCException {

    public static void assertUnknown(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.unknown(""), executable);
    }

    public static void assertUnauthorized(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.unauthorized(), executable);
    }

    public static void assertInvalidSn(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.invalidSn(), executable);
    }

    public static void assertAlreadyExistsBMV(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.alreadyExistsBMV(), executable);
    }

    public static void assertNotExistsBMV(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.notExistsBMV(), executable);
    }

    public static void assertAlreadyExistsBSH(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.alreadyExistsBSH(), executable);
    }

    public static void assertNotExistsBSH(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.notExistsBSH(), executable);
    }

    public static void assertAlreadyExistsLink(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.alreadyExistsLink(), executable);
    }

    public static void assertNotExistsLink(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.notExistsLink(), executable);
    }

    public static void assertAlreadyExistsBMR(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.alreadyExistsBMR(), executable);
    }

    public static void assertNotExistsBMR(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.notExistsBMR(), executable);
    }

    public static void assertUnreachable(Executable executable) {
        AssertBTPException.assertBTPException(BMCException.unreachable(), executable);
    }
}
