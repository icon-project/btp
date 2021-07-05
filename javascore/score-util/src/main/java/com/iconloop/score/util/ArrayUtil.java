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

package com.iconloop.score.util;

import score.Address;

import java.math.BigInteger;
import java.util.List;

public class ArrayUtil {

    public static String[] toStringArray(List<String> list) {
        String[] arr = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public static BigInteger[] toBigIntegerArray(List<BigInteger> list) {
        BigInteger[] arr = new BigInteger[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public static Address[] toAddressArray(List<Address> list) {
        Address[] arr = new Address[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i);
        }
        return arr;
    }

    public static int matchCount(byte[] src, byte[] dst) {
        int srcLen = src.length;
        int dstLen = dst.length;
        if (dstLen < srcLen) {
            srcLen = dstLen;
        }
        for (int i = 0; i < srcLen; i++) {
            if (src[i] != dst[i]) {
                return i;
            }
        }
        return srcLen;
    }

    public static <T extends Comparable<T>> void sort(T[] a) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            T v = a[i];
            for (int j = i+1; j < len; j++) {
                if (v.compareTo(a[j]) > 0) {
                    T t = v;
                    v = a[j];
                    a[j] = t;
                }
            }
            a[i] = v;
        }
    }

}
