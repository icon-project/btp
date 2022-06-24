/*
 * Copyright 2022 ICONLOOP Inc.
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

package foundation.icon.btp.bmv.btp;

import foundation.icon.icx.data.TransactionResult;

import java.math.BigInteger;

public class TransactionFailureException extends Exception {
    private final TransactionResult.Failure failure;

    public TransactionFailureException(TransactionResult.Failure failure) {
        this.failure = failure;
    }

    @Override
    public String toString() {
        return this.failure.toString();
    }

    public BigInteger getCode() {
        return this.failure.getCode();
    }

    public String getMessage() {
        return this.failure.getMessage();
    }
}
