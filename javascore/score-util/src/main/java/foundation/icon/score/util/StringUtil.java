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

package foundation.icon.score.util;

import scorex.util.ArrayList;
import scorex.util.StringTokenizer;

import java.util.List;

public class StringUtil {
    public static String toLowerCamelCase(String str) {
        String camelCase = toCamelCase(str);
        if (camelCase.length() > 0) {
            StringBuilder builder = new StringBuilder();
            builder.append(camelCase.substring(0, 1).toLowerCase());
            if (camelCase.length() > 1) {
                builder.append(camelCase.substring(1));
            }
        }
        return camelCase;
    }

    public static String toCamelCase(String str) {
        StringBuilder builder = new StringBuilder();
        List<String> words = tokenize(str, '_');
        for (String word : words) {
            if (word.length() > 0) {
                String lower = word.toLowerCase();
                builder.append(lower.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    builder.append(lower.substring(1));
                }
            }
        }
        return builder.toString();
    }

    public static List<String> tokenize(String str, char... delimiters) {
        List<String> list = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(str, new String(delimiters));
        while (st.hasMoreTokens()) {
            list.add(st.nextToken());
        }
        return list;
    }

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hexString) {
        if (hexString == null) {
            return null;
        }
        if (hexString.length() % 2 > 0) {
            throw new IllegalArgumentException("hex cannot has odd length");
        }
        int l = hexString.length()/2;
        int j = 0;
        byte[] bytes = new byte[l];
        for (int i = 0; i < l; i++) {
            bytes[i] = (byte)((Character.digit(hexString.charAt(j++), 16) << 4) |
                    Character.digit(hexString.charAt(j++), 16) & 0xFF);
        }
        return bytes;
    }

    public static boolean isAlphaNumeric(String str) {
        for(char c : str.toCharArray()) {
            if(!Character.isLetterOrDigit(c)) {
                return false;
            }
        }
        return true;
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
