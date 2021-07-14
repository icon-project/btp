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

public class BMVStatus {
    private long height;
    private long offset;
    private long last_height;

    @Keep
    public long getHeight() {
        return height;
    }

    @Keep
    public void setHeight(long height) {
        this.height = height;
    }

    @Keep
    public long getOffset() {
        return offset;
    }

    @Keep
    public void setOffset(long offset) {
        this.offset = offset;
    }

    @Keep
    public long getLast_height() {
        return last_height;
    }

    @Keep
    public void setLast_height(long last_height) {
        this.last_height = last_height;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMVStatus{");
        sb.append("height=").append(height);
        sb.append(", offset=").append(offset);
        sb.append(", last_height=").append(last_height);
        sb.append('}');
        return sb.toString();
    }
}
