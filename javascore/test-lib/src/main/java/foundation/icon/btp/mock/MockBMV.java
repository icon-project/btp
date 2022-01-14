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

package foundation.icon.btp.mock;

import foundation.icon.btp.lib.BMV;
import foundation.icon.score.client.ScoreClient;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.annotation.EventLog;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

/**
 * for BMC
 */
@ScoreClient
public interface MockBMV extends BMV {

    @External
    void setHeight(long _height);

    @External
    void setOffset(long _offset);

    @External
    void setLast_height(long _last_height);

    @EventLog
    void HandleRelayMessage(byte[] _ret);

}
