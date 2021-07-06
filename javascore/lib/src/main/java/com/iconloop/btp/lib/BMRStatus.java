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

import score.Address;
import score.annotation.Keep;

import java.math.BigInteger;

public class BMRStatus {
    private Address address;
    private long block_count;
    private BigInteger msg_count;

    @Keep
    public Address getAddress() {
        return address;
    }

    @Keep
    public void setAddress(Address address) {
        this.address = address;
    }

    @Keep
    public long getBlock_count() {
        return block_count;
    }

    @Keep
    public void setBlock_count(long block_count) {
        this.block_count = block_count;
    }

    @Keep
    public BigInteger getMsg_count() {
        return msg_count;
    }

    @Keep
    public void setMsg_count(BigInteger msg_count) {
        this.msg_count = msg_count;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMRStatus{");
        sb.append("address=").append(address);
        sb.append(", block_count=").append(block_count);
        sb.append(", msg_count=").append(msg_count);
        sb.append('}');
        return sb.toString();
    }
}
