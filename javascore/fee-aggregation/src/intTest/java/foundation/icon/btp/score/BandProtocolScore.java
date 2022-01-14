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

package foundation.icon.btp.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.btp.ResultTimeoutException;
import foundation.icon.btp.TransactionFailureException;
import foundation.icon.btp.TransactionHandler;

import java.io.IOException;
import java.math.BigInteger;

import static foundation.icon.btp.Env.LOG;

public class BandProtocolScore extends Score {
    public final static String NAME = "MySampleToken";

    public static BandProtocolScore mustDeploy(TransactionHandler txHandler, Wallet wallet)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "SampleToken");
        Score score = txHandler.deploy(wallet, "bandProtocol.zip", null);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new BandProtocolScore(score);
    }

    public BandProtocolScore(Score other) {
        super(other);
    }

    public BigInteger getRefData(String symbol) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_symbol", new RpcValue(symbol))
                .build();
        return call("get_ref_data", params).asObject().getItem("rate").asInteger();
    }

    public BigInteger getReferenceData(String base, String quote) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_base", new RpcValue(base))
                .put("_quote", new RpcValue(quote))
                .build();
        return call("get_reference_data", params).asObject().getItem("rate").asInteger();
    }

    public Object getReferenceDataBulk(String bases, String quotes) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_bases", new RpcValue(bases))
                .put("_quotes", new RpcValue(quotes))
                .build();
        return call("get_reference_data_bulk", params).asObject();
    }

    public TransactionResult relay(Wallet wallet, String symbols, String rates, String resolveTimes, String requestIds)
            throws IOException, ResultTimeoutException {
            RpcObject params = new RpcObject.Builder()
            .put("_symbols", new RpcValue(symbols))
            .put("_rates", new RpcValue(rates))
            .put("_resolve_times", new RpcValue(resolveTimes))
            .put("_request_ids", new RpcValue(requestIds)).build();
        return this.invokeAndWaitResult(wallet, "relay", params);
    }
}
