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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class BTPAddress {
    public static final String PROTOCOL_BTP="btp";
    private static final String DELIM_PROTOCOL="://";
    private static final String DELIM_NET="/";
    String protocol;
    String net;
    String account;

    public BTPAddress(String protocol, String net, String account) {
        this.protocol = protocol;
        this.net = net;
        this.account = account;
    }

    public String protocol() {
        return protocol;
    }

    public String net() {
        return net;
    }

    public String account() {
        return account;
    }

    @Override
    public String toString() {
        return protocol + DELIM_PROTOCOL + net + DELIM_NET + account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BTPAddress that = (BTPAddress) o;
        return toString().equals(that.toString());
    }

    public boolean isValid() {
        return (!(protocol == null || protocol.isEmpty())) &&
                (!(net == null || net.isEmpty())) &&
                (!(account == null || account.isEmpty()));
    }

    public static BTPAddress parse(String str) {
        if (str == null) {
            return null;
        }
        String protocol = "";
        String net = "";
        String contract = "";
        int protocolIdx = str.indexOf(DELIM_PROTOCOL);
        if(protocolIdx >= 0) {
            protocol = str.substring(0, protocolIdx);
            str = str.substring(protocolIdx + DELIM_PROTOCOL.length());
        }
        int netIdx = str.indexOf(DELIM_NET);
        if (netIdx >= 0) {
            net = str.substring(0, netIdx);
            contract = str.substring(netIdx+DELIM_NET.length());
        } else {
            contract = str;
        }
        return new BTPAddress(protocol, net, contract);
    }

    public static BTPAddress valueOf(String str) throws BTPException {
        BTPAddress btpAddress = parse(str);
        if (btpAddress == null || !btpAddress.isValid()) {
            throw BTPException.unknown("invalid BTPAddress");
        }
        return btpAddress;
    }

    public static void writeObject(ObjectWriter writer, BTPAddress obj) {
        obj.writeObject(writer);
    }

    public void writeObject(ObjectWriter writer) {
        writer.write(this.toString());
    }

    public static BTPAddress readObject(ObjectReader reader) {
        return BTPAddress.parse(reader.readString());
    }

    public static BTPAddress fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BTPAddress.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BTPAddress.writeObject(writer, this);
        return writer.toByteArray();
    }

}
