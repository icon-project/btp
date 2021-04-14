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

public interface ServiceHandler {

    /**
     * Handles BTP Messages from other blockchains, accepts messages only from BMC. If it fails,
     * then BMC will generate a BTP Message that includes error information, then delivered to the source.
     *
     * @param srcNetwork Network Address of source network / blockchain
     * @param service Serial number of the message
     * @param sn Serial number of the message
     * @param msg Serialised byte of service message
     *
     */
    @External
    public void handleBTPMessage(String srcNetwork, String service, int sn, byte[] msg);

    /**
     * Handle the error on delivering the message.
     * Accept the error only from the BMC.
     *
     * @param src BTP Address of BMC that generated the error
     * @param service name of the service
     * @param sn serial number of the original message
     * @param code code of the error
     * @param msg message of the error
     *
     */
    @External
    public void handleBTPError(String src, String service, int sn, int code, String msg);

}
