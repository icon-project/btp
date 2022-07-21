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

import java.math.BigInteger;
import java.util.Map;

public class BMVStatus {
    private long height;

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BMVStatus{");
        sb.append("height=").append(height);
        sb.append('}');
        return sb.toString();
    }

    public static BMVStatus fromMap(Map<String, Object> map) {
        BMVStatus obj = new BMVStatus();
        obj.height = ((BigInteger)map.get("height")).longValue();
        return obj;
    }

}
