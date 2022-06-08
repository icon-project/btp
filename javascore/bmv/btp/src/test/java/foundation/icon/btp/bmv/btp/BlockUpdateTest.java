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
package foundation.icon.btp.bmv.btp;

import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BlockUpdateTest {
    @Test
    void blockUpdateTest() {
        var nsToRoot1 = new BlockUpdate.NetworkSectionToRoot(BlockUpdate.NetworkSectionToRoot.LEFT, StringUtil.hexToBytes("0x11"));
        var nsToRoot2 = new BlockUpdate.NetworkSectionToRoot(BlockUpdate.NetworkSectionToRoot.RIGHT, StringUtil.hexToBytes("0x12"));
        var nstr = new BlockUpdate.NetworkSectionToRoot[]{nsToRoot1, nsToRoot2};
        var blockUpdate = new BlockUpdate(
                BigInteger.ZERO,
                BigInteger.TEN,
                StringUtil.hexToBytes("0x0a"),
                nstr,
                BigInteger.ONE,
                BigInteger.ONE,
                StringUtil.hexToBytes("0x0b"),
                BigInteger.ZERO,
                null,
                StringUtil.hexToBytes("0xab"),
                StringUtil.hexToBytes("0xab")
        );
        var bytes = blockUpdate.toBytes();
        var fromBytes = BlockUpdate.fromBytes(bytes);
        assertEquals(blockUpdate.getMainHeight(), fromBytes.getMainHeight());
        assertEquals(blockUpdate.getRound(), fromBytes.getRound());
        assertArrayEquals(blockUpdate.getNextProofContextHash(), fromBytes.getNextProofContextHash());
        assertArrayEquals(blockUpdate.getNetworkSectionToRoot(), fromBytes.getNetworkSectionToRoot());
        assertEquals(blockUpdate.getNid(), fromBytes.getNid());
        assertEquals(blockUpdate.getUpdateNumber(), fromBytes.getUpdateNumber());
        assertArrayEquals(blockUpdate.getPrev(), fromBytes.getPrev());
        assertEquals(blockUpdate.getMessageCount(), fromBytes.getMessageCount());
        assertArrayEquals(blockUpdate.getMessageRoot(), fromBytes.getMessageRoot());
        assertArrayEquals(blockUpdate.getProof(), fromBytes.getProof());
        assertArrayEquals(blockUpdate.getNextProofContext(), fromBytes.getNextProofContext());
    }
}
