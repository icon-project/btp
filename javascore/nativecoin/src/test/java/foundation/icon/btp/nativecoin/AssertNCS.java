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

import foundation.icon.btp.lib.BTPAddress;

import java.util.Arrays;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssertNCS {
    static Comparator<Asset> assetComparator = (o1, o2) -> {
        int ret = o1.getCoinName().compareTo(o2.getCoinName());
        if (ret == 0) {
            return o1.getAmount().compareTo(o2.getAmount());
        }
        return ret;
    };
    static Comparator<AssetTransferDetail> assetTransferDetailComparator = (o1, o2) -> {
        int ret = assetComparator.compare(o1, o2);
        if (ret == 0) {
            return o1.getFee().compareTo(o2.getFee());
        }
        return ret;
    };

    static void assertEqualsAsset(Asset o1, Asset o2) {
        assertEquals(o1.getCoinName(), o2.getCoinName());
        assertEquals(o1.getAmount(), o2.getAmount());
    }

    static void assertEqualsAssets(Asset[] o1, Asset[] o2) {
        assertEquals(o1.length, o2.length);
        Arrays.sort(o1, assetComparator);
        Arrays.sort(o2, assetComparator);
        for (int i = 0; i < o1.length; i++) {
            assertEqualsAsset(o1[i], o2[i]);
        }
    }

    static void assertEqualsTransferRequest(TransferTransaction o1, TransferRequest o2) {
        assertEquals(o1.getFrom(), o2.getFrom());
        assertEquals(BTPAddress.valueOf(o1.getTo()).account(), o2.getTo());
        assertEqualsAssets(o1.getAssets(), o2.getAssets());
    }

    static void assertEqualsAssetTransferDetail(AssetTransferDetail o1, AssetTransferDetail o2) {
        assertEqualsAsset(o1, o2);
        assertEquals(o1.getFee(), o2.getFee());
    }

    static void assertEqualsAssetTransferDetails(AssetTransferDetail[] o1, AssetTransferDetail[] o2) {
        assertEquals(o1.length, o2.length);
        Arrays.sort(o1, assetTransferDetailComparator);
        Arrays.sort(o2, assetTransferDetailComparator);
        for (int i = 0; i < o1.length; i++) {
            assertEqualsAssetTransferDetail(o1[i], o2[i]);
        }
    }
}
