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

package foundation.icon.btp.nativecoin;

import foundation.icon.btp.test.AssertBTPException;
import org.junit.jupiter.api.function.Executable;

@SuppressWarnings("ThrowableNotThrown")
public class AssertNCSException {

    public static void assertUnknown(Executable executable) {
        AssertBTPException.assertBTPException(NCSException.unknown(""), executable);
    }

    public static void assertUnauthorized(Executable executable) {
        AssertBTPException.assertBTPException(NCSException.unauthorized(), executable);
    }

    public static void assertIRC31Failure(Executable executable) {
        AssertBTPException.assertBTPException(NCSException.irc31Failure(), executable);
    }

    public static void assertIRC31Reverted(Executable executable) {
        AssertBTPException.assertBTPException(NCSException.irc31Reverted(), executable);
    }

}
