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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * JSONRPC error object of ganache
 * <pre>
 * For example:
 * {
 *     "0xfa80ea8e456d67854c5283abd8e364eb281bdbf417d4be6125e98c4ba1983d69": {
 *             "error": "revert",
 *             "program_counter": 25478,
 *             "return": "0x08c379a00000000000000000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000f31343a4e6f74457869737473424d560000000000000000000000000000000000",
 *             "reason": "14:NotExistsBMV"
 *     },
 *     "stack": "c: VM Exception while processing transaction: revert 14:NotExistsBMV\n    at Function.c.fromResults (/Users/adrian/work/src/btp/solidity/bmc/node_modules/ganache-cli/build/ganache-core.node.cli.js:4:192416)\n    at w.processBlock (/Users/adrian/work/src/btp/solidity/bmc/node_modules/ganache-cli/build/ganache-core.node.cli.js:42:50915)\n    at processTicksAndRejections (node:internal/process/task_queues:95:5)",
 *     "name": "c"
 * }
 * </pre>
 */
public class GanacheError {
    private String stack;
    private String name;
    private Map<String, Detail> detailMap = new HashMap<>();

    public String getStack() {
        return stack;
    }

    public String getName() {
        return name;
    }

    public Map<String, Detail> getDetailMap() {
        return detailMap;
    }

    @JsonAnySetter
    private void anySetter(String txHash, Detail detail) {
        detailMap.put(txHash, detail);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GanacheError{");
        sb.append("stack='").append(stack).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", detailMap=").append(detailMap);
        sb.append('}');
        return sb.toString();
    }

    public static Optional<String> getReason(String s) {
        String reason = null;
        try {
            GanacheError error = new ObjectMapper().readValue(s, GanacheError.class);
            if (error.detailMap.size() > 0) {
                for (String txHash : error.detailMap.keySet()) {
                    reason = error.detailMap.get(txHash).reason;
                    break;
                }
            }
        } catch (JsonProcessingException ignored) {
        }
        return Optional.ofNullable(reason);
    }

    public static class Detail {
        private String error;
        @JsonProperty("program_counter")
        private Long programCounter;
        @JsonProperty("return")
        private String returnValue;
        private String reason;

        public String getError() {
            return error;
        }

        public Long getProgramCounter() {
            return programCounter;
        }

        public String getReturnValue() {
            return returnValue;
        }

        public String getReason() {
            return reason;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Detail{");
            sb.append("error='").append(error).append('\'');
            sb.append(", programCounter=").append(programCounter);
            sb.append(", returnValue='").append(returnValue).append('\'');
            sb.append(", reason='").append(reason).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
