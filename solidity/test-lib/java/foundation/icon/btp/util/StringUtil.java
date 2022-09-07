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

package foundation.icon.btp.util;

import org.web3j.utils.Numeric;

public class StringUtil {
    public static String bytesToHex(byte[] bytes) {
        if (bytes != null) {
            return Numeric.toHexString(bytes);
        } else {
            return null;
        }
    }

    public static byte[] hexToBytes(String hexString) {
        if (hexString != null) {
            return Numeric.hexStringToByteArray(hexString);
        } else {
            return null;
        }
    }

    public static String toString(Object obj) {
        if (obj == null) {
            return "null";
        } else {
            return obj.toString();
        }
    }

    public static String toString(byte[] arr) {
        if (arr == null) {
            return "null";
        } else {
            return bytesToHex(arr);
        }
    }

    public static String toString(byte[][] arr) {
        if (arr == null) {
            return "null";
        } else {
            StringBuilder sb = new StringBuilder("[");
            if (arr.length > 0) {
                sb.append(toString(arr[0]));
            }
            for(int i=1;i < arr.length;i++) {
                sb.append(",").append(toString(arr[i]));
            }
            return sb.append("]").toString();
        }
    }

    public static String toString(Object[] arr) {
        if (arr == null) {
            return "null";
        } else {
            StringBuilder sb = new StringBuilder("[");
            if (arr.length > 0) {
                sb.append(toString(arr[0]));
            }
            for(int i=1;i < arr.length;i++) {
                sb.append(",").append(toString(arr[i]));
            }
            return sb.append("]").toString();
        }
    }
}
