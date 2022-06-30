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

package foundation.icon.btp.bmv.btp.score;

import foundation.icon.btp.bmv.btp.ResultTimeoutException;
import foundation.icon.btp.bmv.btp.TransactionFailureException;
import foundation.icon.btp.bmv.btp.TransactionHandler;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.btp.bmv.btp.Env.LOG;

public class BMVScore extends Score {

    public static BMVScore mustDeploy(
            TransactionHandler txHandler,
            Wallet wallet,
            byte[] networkID,
            BigInteger networkTypeID,
            Address bmc,
            byte[] blockUpdate,
            BigInteger seqOffset
    )
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "bmv");
        RpcObject params = new RpcObject.Builder()
                .put("srcNetworkID", new RpcValue(networkID))
                .put("networkTypeID", new RpcValue(networkTypeID))
                .put("bmc", new RpcValue(bmc))
                .put("firstBlockUpdate", new RpcValue(blockUpdate))
                .put("seqOffset", new RpcValue(seqOffset))
                .build();
        Score score = txHandler.deploy(wallet, "bmv-btp-0.1.0-optimized.jar", params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new BMVScore(score);
    }

    public BMVScore(Score other) {
        super(other);
    }

    public RpcObject getStatus() throws IOException {
        RpcObject params = new RpcObject.Builder().build();
        return call("getStatus", params).asObject();
    }

    public TransactionResult handleRelayMessage(Wallet wallet, String bmc, String prev, BigInteger seq, byte[] msg)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder builder = new RpcObject.Builder()
                .put("_bmc", new RpcValue(bmc))
                .put("_prev", new RpcValue(prev))
                .put("_seq", new RpcValue(seq))
                .put("_msg", new RpcValue(msg));
        return this.invokeAndWaitResult(wallet, "handleRelayMessage", builder.build());
    }
}
