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

import com.iconloop.btp.lib.BTPException;

public class BMVException extends BTPException.BMV {

    public BMVException(Code c) {
        super(c, c.name());
    }

    public BMVException(Code c, String message) {
        super(c, message);
    }

    public static BMVException unknown(String message) {
        return new BMVException(Code.Unknown, message);
    }

    public static BMVException invalidMPT() {
        return new BMVException(Code.InvalidMPT);
    }

    public static BMVException invalidMPT(String message) {
        return new BMVException(Code.InvalidMPT, message);
    }

    public static BMVException invalidVotes() {
        return new BMVException(Code.InvalidVotes);
    }

    public static BMVException invalidVotes(String message) {
        return new BMVException(Code.InvalidVotes, message);
    }

    public static BMVException invalidSequence() {
        return new BMVException(Code.InvalidSequence);
    }

    public static BMVException invalidSequence(String message) {
        return new BMVException(Code.InvalidSequence, message);
    }

    public static BMVException invalidBlockUpdate() {
        return new BMVException(Code.InvalidBlockUpdate);
    }

    public static BMVException invalidBlockUpdate(String message) {
        return new BMVException(Code.InvalidBlockUpdate, message);
    }

    public static BMVException invalidBlockProof() {
        return new BMVException(Code.InvalidBlockProof);
    }

    public static BMVException invalidBlockProof(String message) {
        return new BMVException(Code.InvalidBlockProof, message);
    }

    public static BMVException invalidBlockWitness() {
        return new BMVException(Code.InvalidBlockWitness);
    }

    public static BMVException invalidBlockWitness(String message) {
        return new BMVException(Code.InvalidBlockWitness, message);
    }

    public static BMVException invalidSequenceHigher() {
        return new BMVException(Code.InvalidSequenceHigher);
    }

    public static BMVException invalidSequenceHigher(String message) {
        return new BMVException(Code.InvalidSequenceHigher, message);
    }

    public static BMVException invalidBlockUpdateHeightHigher() {
        return new BMVException(Code.InvalidBlockUpdateHeightHigher);
    }

    public static BMVException invalidBlockUpdateHeightHigher(String message) {
        return new BMVException(Code.InvalidBlockUpdateHeightHigher, message);
    }

    public static BMVException invalidBlockUpdateHeightLower() {
        return new BMVException(Code.InvalidBlockUpdateHeightLower);
    }

    public static BMVException invalidBlockUpdateHeightLower(String message) {
        return new BMVException(Code.InvalidBlockUpdateHeightLower, message);
    }

    public static BMVException invalidBlockProofHeightHigher() {
        return new BMVException(Code.InvalidBlockProofHeightHigher);
    }

    public static BMVException invalidBlockProofHeightHigher(String message) {
        return new BMVException(Code.InvalidBlockProofHeightHigher, message);
    }

    public static BMVException invalidBlockWitnessOld() {
        return new BMVException(Code.InvalidBlockWitnessOld);
    }

    public static BMVException invalidBlockWitnessOld(String message) {
        return new BMVException(Code.InvalidBlockWitnessOld, message);
    }

    //BTPException.BMV => 25 ~ 39
    public enum Code implements BTPException.Coded{
        Unknown(0),
        InvalidMPT(1),
        InvalidVotes(2),
        InvalidSequence(3),
        InvalidBlockUpdate(4),
        InvalidBlockProof(5),
        InvalidBlockWitness(6),
        InvalidSequenceHigher(7),
        InvalidBlockUpdateHeightHigher(8),
        InvalidBlockUpdateHeightLower(9),
        InvalidBlockProofHeightHigher(10),
        InvalidBlockWitnessOld(11);
        
        final int code;
        Code(int code){ this.code = code; }

        @Override
        public int code() { return code; }

        static public Code of(int code) {
            for(Code c : values()) {
                if (c.code == code) {
                    return c;
                }
            }
            throw new IllegalArgumentException();
        }
    }
}
