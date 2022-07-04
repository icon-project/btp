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

import foundation.icon.btp.test.MockGovIntegrationTest;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.client.ScoreInterface;
import foundation.icon.score.test.ScoreIntegrationTest;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;

import java.util.List;

@ScoreClient(suffix = "Client")
@ScoreInterface(suffix = "Interface")
public interface ChainScore {
    enum RevertCode {
        StatusIllegalArgument,
        StatusNotFound;
        public RevertCode codeOf(int code) {
            RevertCode[] values = values();
            if (code < 0 || code >= values.length) {
                throw new IllegalArgumentException();
            }
            return values[code];
        }
    }

    Address ADDRESS = Address.fromString("cx0000000000000000000000000000000000000000");

    @External
    void setRevision(int code);

    @External
    long openBTPNetwork(String networkTypeName, String name, Address owner);

    @EventLog(indexed = 2)
    void BTPNetworkTypeActivated(String networkTypeName, long networkTypeID);

    @EventLog(indexed = 2)
    void BTPNetworkOpened(long networkTypeID, long networkID);

    @External
    void closeBTPNetwork(long id);

    @EventLog(indexed = 2)
    void BTPNetworkClosed(long networkTypeID, long networkID);

    @External(readonly=true)
    int getRevision();

    @External
    void setBTPPublicKey(String name, byte[] pubKey);

    @External(readonly=true)
    byte[] getBTPPublicKey(Address address, String name);

    @External(readonly=true)
    long getBTPNetworkTypeID(String name);

    @External
    void sendBTPMessage(long networkId, byte[] message);

}
