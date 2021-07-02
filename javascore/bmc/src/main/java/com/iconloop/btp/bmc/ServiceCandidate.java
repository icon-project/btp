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

package com.iconloop.btp.bmc;

import score.*;

public class ServiceCandidate {
    private String svc;
    private Address address;
    private Address owner;

    public String getSvc() {
        return svc;
    }

    public void setSvc(String svc) {
        this.svc = svc;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Address getOwner() {
        return owner;
    }

    public void setOwner(Address owner) {
        this.owner = owner;
    }


    public static void writeObject(ObjectWriter writer, ServiceCandidate obj) {
        obj.writeObject(writer);
    }

    public static ServiceCandidate readObject(ObjectReader reader) {
        ServiceCandidate obj = new ServiceCandidate();
        reader.beginList();
        obj.setSvc(reader.readNullable(String.class));
        obj.setAddress(reader.readNullable(Address.class));
        obj.setOwner(reader.readNullable(Address.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.writeNullable(this.getSvc());
        writer.writeNullable(this.getAddress());
        writer.writeNullable(this.getOwner());
        writer.end();
    }

    public static ServiceCandidate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ServiceCandidate.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        ServiceCandidate.writeObject(writer, this);
        return writer.toByteArray();
    }
}
