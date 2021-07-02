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

package com.iconloop.btp.bmv.icon;

import com.iconloop.score.util.StringUtil;

import java.math.BigInteger;

public class MessageEvent {
    private String next;
    private BigInteger seq;
    private byte[] msg;

    public String getNext() {
        return next;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public BigInteger getSeq() {
        return seq;
    }

    public void setSeq(BigInteger seq) {
        this.seq = seq;
    }

    public byte[] getMsg() {
        return msg;
    }

    public void setMsg(byte[] msg) {
        this.msg = msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageEvent{");
        sb.append("next='").append(next).append('\'');
        sb.append(", seq=").append(seq);
        sb.append(", msg=").append(StringUtil.toString(msg));
        sb.append('}');
        return sb.toString();
    }
}
