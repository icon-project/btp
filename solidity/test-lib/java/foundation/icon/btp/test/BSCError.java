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

import java.util.Optional;

public class BSCError {
    public static final String REASON_PREFIX = "execution reverted: ";
    public static Optional<String> getReason(String s) {
        if (s.startsWith(REASON_PREFIX)) {
            return Optional.of(s.substring(REASON_PREFIX.length()));
        } else {
            System.out.println("does not start with prefix:"+s);
            return Optional.of(s);
        }
    }
}
