/*
 * Copyright 2019 ICON Foundation
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

package foundation.icon.btp.bmv.btp.score;

import foundation.icon.btp.bmv.btp.Constants;
import foundation.icon.btp.bmv.btp.ResultTimeoutException;
import foundation.icon.btp.bmv.btp.TransactionFailureException;
import foundation.icon.btp.bmv.btp.TransactionHandler;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import scorex.util.HashMap;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;

import static foundation.icon.btp.bmv.btp.Env.LOG;

public class BMCScore extends Score {
    public static Map<Address, String> bmcNetWork = new HashMap<>();
    public static BMCScore mustDeploy(TransactionHandler txHandler, Wallet wallet, String net)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "bmc");
        RpcObject params = new RpcObject.Builder().
                put("_net", new RpcValue(net)).
                build();
        Score score = txHandler.deploy(wallet, getFilePath("bmc-mock.scoreFilePath"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        bmcNetWork.put(score.getAddress(), net);
        return new BMCScore(score);
    }

    public BMCScore(Score other) {
        super(other);
    }

    public Bytes intercallHandleRelayMessage(Wallet wallet, Address bmv, String prev, BigInteger seq, byte[] msg)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder builder = new RpcObject.Builder()
                .put("_addr", new RpcValue(bmv))
                .put("_prev", new RpcValue(prev))
                .put("_seq", new RpcValue(seq))
                .put("_msg", new RpcValue(msg));
        return this.invoke(wallet, "intercallHandleRelayMessage", builder.build(), BigInteger.ZERO, Constants.DEFAULT_STEPS.multiply(BigInteger.valueOf(100)));
    }
}
