/*
 * Copyright 2020 ICON Foundation
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

package foundation.icon.test.score;

import foundation.icon.test.cases.HexConverter;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import scorex.util.ArrayList;

import static foundation.icon.test.Env.LOG;

public class BMVScore extends Score {
    public BMVScore(Score other) {
        super(other);
    }

    public static BMVScore mustDeploy(
        TransactionHandler txHandler, 
        Wallet owner,
        String bmc,
        String net,
        String encodedValidators,
        long mtaOffset,
        int mtaRootSize,
        int mtaCacheSize,
        boolean mtaIsAllowNewerWitness,
        byte[] lastBlockHash,
        Address eventDecoderAddress,
        BigInteger currentSetId
    ) throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BMV");
        RpcObject params = new RpcObject.Builder()
                .put("bmc", new RpcValue(bmc))
                .put("net", new RpcValue(net))
                .put("encodedValidators", new RpcValue(encodedValidators))
                .put("mtaOffset", new RpcValue(Long.toString(mtaOffset)))
                .put("mtaRootSize", new RpcValue(Integer.toString(mtaRootSize)))
                .put("mtaCacheSize", new RpcValue(Integer.toString(mtaCacheSize)))
                .put("mtaIsAllowNewerWitness", new RpcValue(mtaIsAllowNewerWitness))
                .put("lastBlockHash", new RpcValue(HexConverter.bytesToHex(lastBlockHash)))
                .put("eventDecoderAddress", new RpcValue(eventDecoderAddress))
                .put("currentSetId", new RpcValue(currentSetId.toString()))
                .build();
        Score score = txHandler.deploy(owner, getFilePath("sovereignChain"), params);
        LOG.info("bmv score address = " + score.getAddress());
        LOG.infoExiting();
        return new BMVScore(score);
    }

    public String mta() throws IOException {
        return call("mta", null).asString();
    }

    public BigInteger mtaHeight() throws IOException {
        return call("mtaHeight", null).asInteger();
    }

    public List<byte[]> mtaRoot() throws IOException {
        List<RpcItem> listRpcItem = call("mtaRoot", null).asArray().asList();
        List<byte[]> result = new ArrayList<byte[]>(listRpcItem.size());
        for (RpcItem rpcItem : listRpcItem) {
            if (rpcItem.isNull()) {
                result.add(null);    
            } else {
                result.add(rpcItem.asByteArray());
            }
        }
        return result;
    }

    public byte[] mtaLastBlockHash() throws IOException {
        return call("mtaLastBlockHash", null).asByteArray();
    }

    public BigInteger mtaOffset() throws IOException {
        return call("mtaOffset", null).asInteger();
    }

    public List<byte[]> mtaCaches() throws IOException {
        List<RpcItem> listRpcItem = call("mtaCaches", null).asArray().asList();
        List<byte[]> result = new ArrayList<byte[]>(listRpcItem.size());
        for (RpcItem rpcItem : listRpcItem) {
            if (rpcItem.isNull()) {
                result.add(null);    
            } else {
                result.add(rpcItem.asByteArray());
            }
        }
        return result;
    }

    public Address bmc() throws IOException {
        return call("bmc", null).asAddress();
    }

    public Address eventDecoder() throws IOException {
        return call("eventDecoder", null).asAddress();
    }

    public String netAddress() throws IOException {
        return call("netAddress", null).asString();
    }

    public BigInteger lastHeight() throws IOException {
        return call("lastHeight", null).asInteger();
    }

    public BigInteger setId() throws IOException {
        return call("setId", null).asInteger();
    }

    public List<byte[]> validators() throws IOException {
        List<RpcItem> listRpcItem = call("validators", null).asArray().asList();
        List<byte[]> result = new ArrayList<byte[]>(listRpcItem.size());
        for (RpcItem rpcItem : listRpcItem) {
            if (rpcItem.isNull()) {
                result.add(null);    
            } else {
                result.add(rpcItem.asByteArray());
            }
        }
        return result;
    }

    public Bytes handleRelayMessage(Wallet wallet, String bmc, String prev, BigInteger seq, String msg) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("bmc", new RpcValue(bmc))
                .put("prev", new RpcValue(prev))
                .put("seq", new RpcValue(seq))
                .put("msg", new RpcValue(msg))
                .build();
        return invoke(wallet, "handleRelayMessage", params);
    }
}
