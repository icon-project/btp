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

package foundation.icon.btp.bmv.icon;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.score.util.Logger;
import score.annotation.External;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, String _msg) {
        RelayMessage relayMessages = RelayMessage.fromBytes(_msg.getBytes(StandardCharsets.UTF_8));
        RelayMessage.TypePrefixedMessage[] typePrefixedMessages = relayMessages.getMessages();
        for (RelayMessage.TypePrefixedMessage message : typePrefixedMessages) {
            Object msg = message.getMessage();
            if (msg instanceof BlockUpdate) {
                blockUpdate((BlockUpdate) msg);
            } else if (msg instanceof MessageProof) {
                proveMessage((MessageProof) msg);
            }
        }
        return new byte[0][];
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        return null;
    }

    private void blockUpdate(BlockUpdate blockUpdate) {

    }

    private void proveMessage(MessageProof messageProof) {

    }
}
