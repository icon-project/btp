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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BTPAddress;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class Route {
    private BTPAddress destination;
    private BTPAddress next;

    public Route() {
    }

    public Route(BTPAddress destination, BTPAddress next) {
        this.destination = destination;
        this.next = next;
    }

    public BTPAddress getDestination() {
        return destination;
    }

    public void setDestination(BTPAddress destination) {
        this.destination = destination;
    }

    public BTPAddress getNext() {
        return next;
    }

    public void setNext(BTPAddress next) {
        this.next = next;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Route{");
        sb.append("destination=").append(destination);
        sb.append(", next=").append(next);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Route obj) {
        obj.writeObject(writer);
    }

    public static Route readObject(ObjectReader reader) {
        Route obj = new Route();
        reader.beginList();
        obj.setDestination(reader.readNullable(BTPAddress.class));
        obj.setNext(reader.readNullable(BTPAddress.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.writeNullable(this.getDestination());
        writer.writeNullable(this.getNext());
        writer.end();
    }

    public static Route fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Route.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Route.writeObject(writer, this);
        return writer.toByteArray();
    }
}
