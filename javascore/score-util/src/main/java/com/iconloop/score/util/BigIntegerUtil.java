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

import score.Context;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

public class BigIntegerUtil {
    /**
     * calculate multiply
     *
     * @param x the first value
     * @param y the second value
     * @return BigInteger multiply
     */
    public static BigInteger multiply(BigInteger x, double y) {
        double ret = StrictMath.floor(x.doubleValue() * y);
        return new BigDecimal(ret).toBigInteger();
    }

    /**
     * floor(x/y*pow(10^s))/pow(10^s)
     *
     * @param x the first value
     * @param y the second value
     * @param s scale factor
     * @return double floor(x/y*pow(10^s))/pow(10^s)
     */
    public static double floorDivide(BigInteger x, BigInteger y, int s) {
        double scale = StrictMath.pow(10, s);
        return StrictMath.floor(x.doubleValue() / y.doubleValue() * scale) / scale;
    }

}
