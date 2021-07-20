/*
 * Copyright 2021 ICON Foundation
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

package foundation.icon.btp.nativecoin;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TransferStartEventLog {
    static final String SIGNATURE = "TransferStart(Address,str,int,bytes)";
    private Address from;
    private String to;
    private BigInteger sn;
    private AssetTransferDetail[] assets;

    public TransferStartEventLog(TransactionResult.EventLog el) {
        from = new Address(el.getIndexed().get(1));
        to = el.getData().get(0);
        sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(1));
        assets = toAssetTransferDetailArray(IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(el.getData().get(2)));
    }

    public static AssetTransferDetail[] toAssetTransferDetailArray(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        reader.beginList();
        List<AssetTransferDetail> list = new ArrayList<>();
        while (reader.hasNext()) {
            list.add(reader.read(AssetTransferDetail.class));
        }
        reader.end();
        return list.toArray(new AssetTransferDetail[]{});
    }

    public Address getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public BigInteger getSn() {
        return sn;
    }

    public AssetTransferDetail[] getAssets() {
        return assets;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferStartEventLog{");
        sb.append("from=").append(from);
        sb.append(", to='").append(to).append('\'');
        sb.append(", sn=").append(sn);
        sb.append(", assets=").append(StringUtil.toString(assets));
        sb.append('}');
        return sb.toString();
    }

    public static List<TransferStartEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<TransferStartEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                TransferStartEventLog.SIGNATURE,
                address,
                TransferStartEventLog::new,
                filter);
    }
}
