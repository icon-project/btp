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

import foundation.icon.score.client.ScoreClient;
import score.Address;
import score.annotation.External;

@ScoreClient
public interface MockGov {
    Address ADDRESS = Address.fromString("cx0000000000000000000000000000000000000001");

    @External
    void setRevision(int code);

    @External
    long openBTPNetwork(String networkTypeName, String name, Address owner);

    @External
    void closeBTPNetwork(long id);
}
