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

public class KusamaEventDecoderScore extends Score {
    public KusamaEventDecoderScore(Score other) {
        super(other);
    }

    public static KusamaEventDecoderScore mustDeploy(
        TransactionHandler txHandler, 
        Wallet owner
    ) throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "KusamaEventDecoder");
        Score score = txHandler.deploy(owner, getFilePath("KusamaEventDecoder"), null);
        LOG.info("KusamaEventDecoder score address = " + score.getAddress());
        LOG.infoExiting();
        return new KusamaEventDecoderScore(score);
    }
}
