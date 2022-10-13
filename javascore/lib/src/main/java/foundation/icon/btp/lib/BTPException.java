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

import score.UserRevertException;
import score.UserRevertedException;

public class BTPException extends UserRevertException {
    /**
     * BTPException.BTP => 0 ~ 9
     * BTPException.BMC => 10 ~ 24
     * BTPException.BMV => 25 ~ 39
     * BTPException.BSH => 40 ~
     */
    public enum Type {
        BTP(0),
        BMC(10),
        BMV(25),
        BSH(40);

        int offset;
        Type(int offset) {
            this.offset = offset;
        }
        int apply(int code) {
            if (code < 0) {
                throw new IllegalArgumentException();
            }
            code = offset + code;
            if (!this.equals(BSH) && code >= values()[ordinal() + 1].offset) {
                throw new IllegalArgumentException();
            }
            return code;
        }
        int recover(int code) {
            if (code < 0) {
                throw new IllegalArgumentException();
            }
            return code - offset;
        }
        static Type valueOf(int code) throws IllegalArgumentException {
            if (code < 0) {
                throw new IllegalArgumentException();
            }
            Type prev = null;
            for(Type v : values()) {
                if (prev != null && code < prev.offset) {
                    return v;
                }
                prev = v;
            }
            return prev;
        }
    };

    private final Type type;
    private final int code;

    BTPException(Code c) {
        this(Type.BTP, c, c.name());
    }

    BTPException(Code code, String message) {
        this(Type.BTP, code, message);
    }

    BTPException(Type type, Coded code, String message) {
        this(type, code.code(), message);
    }

    BTPException(Type type, int code, String message) {
        super(message);
        this.type = type;
        this.code = type.apply(code);
    }

    BTPException(UserRevertedException e) {
        super(e.getMessage(), e);
        this.code = e.getCode();
        this.type = Type.valueOf(code);
    }

    BTPException(Type type, Throwable cause) {
        super(cause.getMessage(), cause);
        this.type = type;
        this.code = type.apply(0);
    }

    public static BTPException of(Throwable cause) {
        return of(cause, Type.BTP);
    }

    public static BTPException of(Throwable cause, Type defaultType) {
        if (cause instanceof BTPException) {
            return (BTPException)cause;
        } else if (cause instanceof UserRevertedException) {
            return new BTPException((UserRevertedException) cause);
        } else {
            return new BTPException(defaultType, cause);
        }
    }

    public Type getType() {
        return type;
    }

    @Override
    public int getCode() {
        return code;
    }

    public int getCodeOfType() {
        return type.recover(code);
    }

    public interface Coded {
        int code();
        default boolean equals(BTPException e) {
            return code() == e.getCodeOfType();
        }
    }

    //BTPException.BTP => 0 ~ 9
    public enum Code implements Coded {
        Unknown(0);

        final int code;
        Code(int code){ this.code = code; }

        public int code() { return code; }

    }

    public static BTPException unknown(String message) {
        return new BTPException(Code.Unknown, message);
    }

    public static class BMC extends BTPException {

        public BMC(int code, String message) {
            super(Type.BMC, code, message);
        }

        public BMC(Coded code, String message) {
            this(code.code(), message);
        }

    }

    public static class BMV extends BTPException {

        public BMV(int code, String message) {
            super(Type.BMV, code, message);
        }

        public BMV(Coded code, String message) {
            this(code.code(), message);
        }

    }

    public static class BSH extends BTPException {

        public BSH(int code, String message) {
            super(Type.BSH, code, message);
        }

        public BSH(Coded code, String message) {
            this(code.code(), message);
        }

    }

}
