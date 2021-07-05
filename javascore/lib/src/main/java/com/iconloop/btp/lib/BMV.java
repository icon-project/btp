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

package com.iconloop.btp.lib;

import score.annotation.External;

import java.math.BigInteger;

public interface BMV {
    /**
     * Decodes Relay Messages and process BTP Messages
     * If there is an error, then it sends a BTP Message containing the Error Message
     * BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
     *
     * @param _bmc String ( BTP Address of the BMC handling the message )
     * @param _prev String ( BTP Address of the previous BMC )
     * @param _seq Integer ( next sequence number to get a message )
     * @param _msg String ( base64 encoded string of serialized bytes of Relay Message )
     * @return List of serialized bytes of a BTP Message
     */
    @External
    byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, String _msg);

    /**
     * Get status of BMV.
     * Used by the relay to resolve next BTP Message to send.
     * Called by BMC.
     *
     * @return The object contains followings fields.
     */
    @External(readonly = true)
    BMVStatus getStatus();
}
