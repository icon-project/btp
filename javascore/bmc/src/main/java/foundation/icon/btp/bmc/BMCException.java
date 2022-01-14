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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BTPException;

public class BMCException extends BTPException.BMC {

    public BMCException(Code c) {
        super(c, c.name());
    }

    public BMCException(Code c, String message) {
        super(c, message);
    }

    public static BMCException unknown(String message) {
        return new BMCException(Code.Unknown, message);
    }
    public static BMCException unauthorized() {
        return new BMCException(Code.Unauthorized);
    }
    public static BMCException unauthorized(String message) {
        return new BMCException(Code.Unauthorized, message);
    }
    public static BMCException invalidSn() {
        return new BMCException(Code.InvalidSn);
    }
    public static BMCException alreadyExistsBMV() {
        return new BMCException(Code.AlreadyExistsBMV);
    }
    public static BMCException notExistsBMV() {
        return new BMCException(Code.NotExistsBMV);
    }
    public static BMCException alreadyExistsBSH() {
        return new BMCException(Code.AlreadyExistsBSH);
    }
    public static BMCException notExistsBSH() {
        return new BMCException(Code.NotExistsBSH);
    }
    public static BMCException alreadyExistsLink() {
        return new BMCException(Code.AlreadyExistsLink);
    }
    public static BMCException notExistsLink() {
        return new BMCException(Code.NotExistsLink);
    }
    public static BMCException alreadyExistsBMR() {
        return new BMCException(Code.AlreadyExistsBMR);
    }
    public static BMCException notExistsBMR() {
        return new BMCException(Code.NotExistsBMR);
    }
    public static BMCException unreachable() {
        return new BMCException(Code.Unreachable);
    }
    public static BMCException drop() {
        return new BMCException(Code.Drop);
    }

    //BTPException.BMC => 10 ~ 24
    public enum Code implements BTPException.Coded{
        Unknown(0),
        Unauthorized(1),
        InvalidSn(2),
        AlreadyExistsBMV(3),
        NotExistsBMV(4),
        AlreadyExistsBSH(5),
        NotExistsBSH(6),
        AlreadyExistsLink(7),
        NotExistsLink(8),
        AlreadyExistsBMR(9),
        NotExistsBMR(10),
        Unreachable(11),
        Drop(12);

        final int code;
        Code(int code){ this.code = code; }

        @Override
        public int code() { return code; }

    }

}
