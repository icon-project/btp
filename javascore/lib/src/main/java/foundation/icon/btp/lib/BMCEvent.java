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

package foundation.icon.btp.lib;

import score.annotation.EventLog;

import java.math.BigInteger;

public interface BMCEvent {
    /**
     * (EventLog) Sends the message to the next BMC.
     * The relay monitors this event.
     * <p>
     * indexed: 2
     *
     * @param _next String ( BTP Address of the BMC to handle the message )
     * @param _seq  Integer ( sequence number of the message from current BMC to the next )
     * @param _msg  Bytes ( serialized bytes of BTP Message )
     */
    @EventLog(indexed = 2)
    void Message(String _next, BigInteger _seq, byte[] _msg);

    /**
     * TODO [TBD] add 'ErrorOnBTPError' to IIP-25.BMC.Events
     * (EventLog) raised BTPException while BSH.handleBTPError
     * <p>
     * indexed: 2
     *
     * @param _svc   String ( name of the service )
     * @param _sn   Integer ( serial number of the message, must be positive )
     * @param _code  Integer ( error code )
     * @param _msg   String ( error message )
     * @param _ecode ( BTPException code )
     * @param _emsg  ( BTPException message )
     */
    @EventLog(indexed = 2)
    void ErrorOnBTPError(String _svc, BigInteger _sn, BigInteger _code, String _msg, BigInteger _ecode, String _emsg);

}
