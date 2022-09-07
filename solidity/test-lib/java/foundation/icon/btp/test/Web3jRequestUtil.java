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

import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.Response;

import java.io.IOException;

public class Web3jRequestUtil {
    public static <T extends Response> T send(Request<?, T> req) {
        try {
            return req.send();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
