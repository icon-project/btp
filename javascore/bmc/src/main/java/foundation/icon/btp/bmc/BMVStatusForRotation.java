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

import foundation.icon.btp.lib.BMVStatus;

public class BMVStatusForRotation extends BMVStatus {
    private long last_height;

    public long getLast_height() {
        return last_height;
    }

    public void setLast_height(long last_height) {
        this.last_height = last_height;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMVStatusForRotation{");
        sb.append("last_height=").append(last_height);
        sb.append('}');
        return sb.toString();
    }
}
