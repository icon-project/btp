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

package foundation.icon.btp.bmv.btpblock.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.Log;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;
import foundation.icon.test.Constants;
import scorex.util.HashMap;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Map;


public class BMCScore extends Score {
    private static final Log LOG = Log.getGlobal();
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

    public Bytes handleRelayMessage(Wallet wallet, Address bmv, String prev, BigInteger seq, byte[] msg)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder builder = new RpcObject.Builder()
                .put("_addr", new RpcValue(bmv))
                .put("_prev", new RpcValue(prev))
                .put("_seq", new RpcValue(seq))
                .put("_msg", new RpcValue(msg));
        return this.invoke(wallet, "handleRelayMessage", builder.build(), BigInteger.ZERO, Constants.DEFAULT_STEPS.multiply(BigInteger.valueOf(100)));
    }
}
