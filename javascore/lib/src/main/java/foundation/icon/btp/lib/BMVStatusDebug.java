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

import score.annotation.Keep;

public class BMVStatusDebug {
    private long height;
    private long sequence_offset;
    private long first_message_sn;
    private long message_count;

    @Keep
    public BMVStatusDebug() {
    }

    @Keep
    public long getHeight() {
        return height;
    }

    @Keep
    public void setHeight(long height) {
        this.height = height;
    }

    @Keep
    public long getSequence_offset() {
        return sequence_offset;
    }

    @Keep
    public void setSequence_offset(long sequence_offset) {
        this.sequence_offset = sequence_offset;
    }

    @Keep
    public long getFirst_message_sn() {
        return first_message_sn;
    }

    @Keep
    public void setFirst_message_sn(long first_message_sn) {
        this.first_message_sn = first_message_sn;
    }

    @Keep
    public long getMessage_count() {
        return message_count;
    }

    @Keep
    public void setMessage_count(long message_count) {
        this.message_count = message_count;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMVStatus{");
        sb.append("height=").append(height);
        sb.append(", sequence_offset=").append(sequence_offset);
        sb.append(", first_message_sn=").append(first_message_sn);
        sb.append(", message_count=").append(message_count);
        sb.append('}');
        return sb.toString();
    }
}
