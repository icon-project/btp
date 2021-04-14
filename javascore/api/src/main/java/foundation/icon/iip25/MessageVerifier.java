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
package foundation.icon.iip25;

import score.annotation.External;

public interface MessageVerifier {

    /**
     * Decodes Relay Messages and process BTP Messages, if there is an error, then it sends a
     * BTP Message containing the Error Message, BTP Messages with old sequence numbers are ignored.
     * A BTP Message contains future sequence number will fa
     *
     * @param bmc BTP Address of the BMC handling the message
     * @param prev BTP Address of the previous BMC
     * @param seq next sequence number to get a message
     * @param msg serialized bytes of Relay Message
     *
     * @return list of BTP Messages List of serialized bytes of a BTP Message
     */
    @External
    public byte[] handleRelayMessage(String bmc, String prev, int seq, byte[] msg);
}
