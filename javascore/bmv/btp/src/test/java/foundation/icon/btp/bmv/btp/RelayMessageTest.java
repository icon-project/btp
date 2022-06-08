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
package foundation.icon.btp.bmv.btp;

import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class RelayMessageTest {
    @Test
    void typePrefixedMessageTest() {
        var typedMessage = new RelayMessage.TypePrefixedMessage(1, StringUtil.hexToBytes("0x11"));
        var bytes = typedMessage.toBytes();
        var fromBytes = RelayMessage.TypePrefixedMessage.fromBytes(bytes);
        assertEquals(typedMessage, fromBytes);
    }

    @Test
    void relayMessageTest() {
        var typedMessage1 = new RelayMessage.TypePrefixedMessage(1, StringUtil.hexToBytes("0x11"));
        var typedMessage2 = new RelayMessage.TypePrefixedMessage(2, StringUtil.hexToBytes("0x11"));
        var messages = new RelayMessage.TypePrefixedMessage[]{typedMessage1, typedMessage2};
        var relayMessage = new RelayMessage();
        relayMessage.setMessages(messages);
        var bytes = relayMessage.toBytes();
        var fromBytes = RelayMessage.fromBytes(bytes);
        assertArrayEquals(relayMessage.getMessages(), fromBytes.getMessages());
    }
}
