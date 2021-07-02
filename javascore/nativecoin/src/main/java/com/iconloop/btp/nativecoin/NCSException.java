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

package com.iconloop.btp.nativecoin;

import com.iconloop.btp.lib.BTPException;

public class NCSException extends BTPException.BSH {

    public NCSException(Code c) {
        super(c, c.name());
    }

    public NCSException(Code c, String message) {
        super(c, message);
    }

    public static NCSException unknown(String message) {
        return new NCSException(Code.Unknown, message);
    }

    public static NCSException unauthorized() {
        return new NCSException(Code.Unauthorized);
    }
    public static NCSException unauthorized(String message) {
        return new NCSException(Code.Unauthorized, message);
    }

    public static NCSException irc31Failure() {
        return new NCSException(Code.IRC31Failure);
    }
    public static NCSException irc31Failure(String message) {
        return new NCSException(Code.IRC31Failure, message);
    }

    public static NCSException irc31Reverted() {
        return new NCSException(Code.IRC31Reverted);
    }
    public static NCSException irc31Reverted(String message) {
        return new NCSException(Code.IRC31Reverted, message);
    }


    //BTPException.BSH => 40 ~ 54
    public enum Code implements BTPException.Coded{
        Unknown(0),
        Unauthorized(1),
        IRC31Failure(2),
        IRC31Reverted(3);

        final int code;
        Code(int code){ this.code = code; }

        @Override
        public int code() { return code; }

    }
}
