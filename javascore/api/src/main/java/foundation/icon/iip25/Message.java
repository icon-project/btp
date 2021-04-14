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

public interface Message {

    /**
     *
     * @return BTP address of source BMC
     */
    public String getSource();

    /**
     *
     * @return BTP address the destination BMC
     */
    public String getDestination();

    /**
     *
     * @return name of the service
     */
    public String getService();

    /**
     *
     * @return serial number of the service
     */
    public int getSerialNumber();

    /**
     *
     * @return serialized bytes of Service Message or Error Message
     */
    public byte[] getMessage();
}
