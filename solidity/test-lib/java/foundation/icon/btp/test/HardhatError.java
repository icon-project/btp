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

package foundation.icon.btp.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

/**
 * JSONRPC error object of hardhat
 * <pre>
 * For example:
 * {
 *   "message": "Error: VM Exception while processing transaction: reverted with reason string 13:AlreadyExistsBMV",
 *   "txHash": "0xdf3e8108a09838d035ec9bf0347fb628e6d7baff035a8354fd72c01367713b1b",
 *   "data": "0x08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000001331333a416c7265616479457869737473424d5600000000000000000000000000"
 * }
 * </pre>
 */
public class HardhatError {
    public static final String REASON_PREFIX =
            "Error: VM Exception while processing transaction: reverted with reason string '";
    private String message;
    private String txHash;
    private String data;

    public String getMessage() {
        return message;
    }

    public String getTxHash() {
        return txHash;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HardhatError{");
        sb.append("message='").append(message).append('\'');
        sb.append(", txHash='").append(txHash).append('\'');
        sb.append(", data='").append(data).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static Optional<String> getReason(String s) {
        String reason = null;
        try {
            HardhatError error = new ObjectMapper().readValue(s, HardhatError.class);
            if (error.message.startsWith(REASON_PREFIX)) {
                reason = error.message.substring(REASON_PREFIX.length(), error.message.length()-2);
            } else {
                System.out.println("does not start with prefix:"+error.message);
                reason = error.message;
            }
        } catch (JsonProcessingException ignored) {
        }
        return Optional.ofNullable(reason);
    }
}
