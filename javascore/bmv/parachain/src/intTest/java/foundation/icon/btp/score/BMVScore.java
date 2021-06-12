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
        long relayMtaOffset,
        long paraMtaOffset,
        int mtaRootSize,
        int mtaCacheSize,
        boolean mtaIsAllowNewerWitness,
        byte[] relayLastBlockHash,
        byte[] paraLastBlockHash,
        Address relayEventDecoderAddress,
        Address paraEventDecoderAddress,
        BigInteger currentSetId,
        BigInteger paraChainId
    ) throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "BMV");
        RpcObject params = new RpcObject.Builder()
                .put("bmc", new RpcValue(bmc))
                .put("net", new RpcValue(net))
                .put("encodedValidators", new RpcValue(encodedValidators))
                .put("relayMtaOffset", new RpcValue(Long.toString(relayMtaOffset)))
                .put("paraMtaOffset", new RpcValue(Long.toString(paraMtaOffset)))
                .put("mtaRootSize", new RpcValue(Integer.toString(mtaRootSize)))
                .put("mtaCacheSize", new RpcValue(Integer.toString(mtaCacheSize)))
                .put("mtaIsAllowNewerWitness", new RpcValue(mtaIsAllowNewerWitness))
                .put("relayLastBlockHash", new RpcValue(HexConverter.bytesToHex(relayLastBlockHash)))
                .put("paraLastBlockHash", new RpcValue(HexConverter.bytesToHex(paraLastBlockHash)))
                .put("relayEventDecoderAddress", new RpcValue(relayEventDecoderAddress))
                .put("paraEventDecoderAddress", new RpcValue(paraEventDecoderAddress))
                .put("relayCurrentSetId", new RpcValue(currentSetId.toString()))
                .put("paraChainId", new RpcValue(paraChainId.toString()))
                .build();
        Score score = txHandler.deploy(owner, getFilePath("parachain"), params);
        LOG.info("bmv score address = " + score.getAddress());
        LOG.infoExiting();
        return new BMVScore(score);
    }

    public String paraMta() throws IOException {
        return call("paraMta", null).asString();
    }

    public String relayMta() throws IOException {
        return call("relayMta", null).asString();
    }

    public BigInteger paraMtaHeight() throws IOException {
        return call("paraMtaHeight", null).asInteger();
    }

    public BigInteger relayMtaHeight() throws IOException {
        return call("relayMtaHeight", null).asInteger();
    }

    public List<byte[]> paraMtaRoot() throws IOException {
        List<RpcItem> listRpcItem = call("paraMtaRoot", null).asArray().asList();
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

    public List<byte[]> relayMtaRoot() throws IOException {
        List<RpcItem> listRpcItem = call("relayMtaRoot", null).asArray().asList();
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

    public byte[] relayMtaLastBlockHash() throws IOException {
        return call("relayMtaLastBlockHash", null).asByteArray();
    }

    public byte[] paraMtaLastBlockHash() throws IOException {
        return call("paraMtaLastBlockHash", null).asByteArray();
    }

    public BigInteger paraMtaOffset() throws IOException {
        return call("paraMtaOffset", null).asInteger();
    }

    public BigInteger relayMtaOffset() throws IOException {
        return call("relayMtaOffset", null).asInteger();
    }

    public List<byte[]> paraMtaCaches() throws IOException {
        List<RpcItem> listRpcItem = call("paraMtaCaches", null).asArray().asList();
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

    public List<byte[]> relayMtaCaches() throws IOException {
        List<RpcItem> listRpcItem = call("relayMtaCaches", null).asArray().asList();
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

    public Address paraEventDecoder() throws IOException {
        return call("paraEventDecoder", null).asAddress();
    }

    public Address relayEventDecoder() throws IOException {
        return call("relayEventDecoder", null).asAddress();
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
