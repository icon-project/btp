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
package foundation.icon.btp.bmv;

public class BTPAddress {

    private String protocol;
    private String net;
    private String contract;

    public BTPAddress(String protocol, String net, String contract) {
        this.protocol = protocol;
        this.net = net;
        this.contract = contract;

        String nid = net.substring(0, net.lastIndexOf("."));
        String chain = net.substring(net.lastIndexOf(".") + 1);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getNet() {
        return net;
    }

    public String getContract() {
        return contract;
    }

    public static BTPAddress fromString(String addr) {
        // TODO use scorex tokenizer - requires rebuild of the goloop docker image to include the Jar
        String protocol = addr.substring(0, addr.lastIndexOf("://"));
        String net = addr.substring(protocol.length() + 3, addr.lastIndexOf("/"));
        int offset = protocol.length() + net.length() + 4;
        String contract = "";
        if (addr.length() > offset)
            contract = addr.substring(offset, addr.length());
        return new BTPAddress(protocol, net, contract);
    }
}
