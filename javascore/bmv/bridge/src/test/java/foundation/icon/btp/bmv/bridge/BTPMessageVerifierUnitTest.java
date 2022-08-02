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

package foundation.icon.btp.bmv.bridge;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BTPMessageVerifierUnitTest extends TestBase {
    static final ServiceManager sm = getServiceManager();
    static final Account owner = sm.createAccount();

    static final BTPAddress prev = BTPIntegrationTest.Faker.btpLink();
    static final Account bmcAccount = Account.newScoreAccount(Integer.MAX_VALUE);
    static final BTPAddress bmc = new BTPAddress(BTPIntegrationTest.Faker.btpNetwork(),
            bmcAccount.getAddress().toString());

    static Score score;

    @BeforeAll
    public static void setup() throws Exception {
        score = sm.deploy(owner, BTPMessageVerifier.class,
                bmcAccount.getAddress(),
                prev.net(),
                BigInteger.valueOf(0));
    }

    @Test
    public void handleRelayMessage() {
        BigInteger seq = BigInteger.ZERO;
        String msg = "testMessage";
        EventDataBTPMessage ed = new EventDataBTPMessage(bmc.toString(), seq.add(BigInteger.ONE), msg.getBytes());
        BigInteger height = BigInteger.ONE;
        ReceiptProof rp = new ReceiptProof(0, List.of(ed), height);
        RelayMessage rm = new RelayMessage(new ArrayList<>(List.of(rp)));

        byte[][] ret = (byte[][]) sm.call(bmcAccount, BigInteger.ZERO, score.getAddress(),
                "handleRelayMessage",
                bmc.toString(), prev.toString(), seq, toBytes(rm));
        assertEquals(rm.getReceiptProofs().length, ret.length);
        assertArrayEquals(msg.getBytes(), ret[0]);
        Map<String, Object> map = (Map<String, Object>) score.call("getStatus");
        assertEquals(height, map.get("height"));
    }

    static byte[] toBytes(List<EventDataBTPMessage> events) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(events.size());
        for (EventDataBTPMessage event : events) {
            writer.beginList(3);
            writer.write(event.getNext_bmc());
            writer.write(event.getSeq());
            writer.write(event.getMsg());
            writer.end();
        }
        writer.end();
        return writer.toByteArray();
    }

    static byte[] toBytes(ReceiptProof rp) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(3);
        writer.write(rp.getIndex());
        writer.write(toBytes(rp.getEvents()));
        writer.write(rp.getHeight());
        writer.end();
        return writer.toByteArray();
    }

    static byte[] toBytes(RelayMessage rm) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(1);
        ReceiptProof[] rps = rm.getReceiptProofs();
        writer.beginList(rps.length);
        for(ReceiptProof rp : rps) {
            writer.write(toBytes(rp));
        }
        writer.end();
        writer.end();
        return writer.toByteArray();
    }

    static void assertEventDataBTPMessageEquals(EventDataBTPMessage o1, EventDataBTPMessage o2) {
        assertEquals(o1.getNext_bmc(), o2.getNext_bmc());
        assertEquals(o1.getSeq(), o2.getSeq());
        assertArrayEquals(o1.getMsg(), o2.getMsg());
    }

    static void assertReceiptProofEquals(ReceiptProof o1, ReceiptProof o2) {
        assertEquals(o1.getIndex(), o2.getIndex());
        assertEquals(o1.getEvents().size(), o2.getEvents().size());
        for (int i = 0; i < o1.getEvents().size(); i++) {
            assertEventDataBTPMessageEquals(o1.getEvents().get(i), o2.getEvents().get(i));
        }
        assertEquals(o1.getHeight(), o2.getHeight());
    }

    static void assertRelayMessageEquals(RelayMessage o1, RelayMessage o2) {
        assertEquals(o1.getReceiptProofs().length, o2.getReceiptProofs().length);
        for (int i = 0; i < o1.getReceiptProofs().length; i++) {
            assertReceiptProofEquals(o1.getReceiptProofs()[i], o2.getReceiptProofs()[i]);
        }
    }

    @Test
    public void encodeRelayMessage() {
        String next_bmc = bmc.toString();
        BigInteger seq = BigInteger.ZERO;
        byte[] msg = "testMessage".getBytes();
        EventDataBTPMessage ed = new EventDataBTPMessage(next_bmc, seq, msg);
        assertEquals(next_bmc, ed.getNext_bmc());
        assertEquals(seq, ed.getSeq());
        assertArrayEquals(msg, ed.getMsg());

        int index = 0;
        List<EventDataBTPMessage> events = List.of(ed);
        BigInteger height = BigInteger.ONE;
        ReceiptProof rp = new ReceiptProof(index, events, height);
        assertEquals(index, rp.getIndex());
        assertEquals(events, rp.getEvents());
        assertEquals(height, rp.getHeight());
        ReceiptProof rp2 = ReceiptProof.fromBytes(toBytes(rp));
        assertReceiptProofEquals(rp, rp2);

        ArrayList<ReceiptProof> rps = new ArrayList<>(List.of(rp));
        RelayMessage rm = new RelayMessage(rps);
        assertArrayEquals(rps.toArray(), rm.getReceiptProofs());
        RelayMessage rm2 = RelayMessage.fromBytes(toBytes(rm));
        assertRelayMessageEquals(rm, rm2);
    }

    @Test
    public void decodeTest() {
        String hex = "de942eca89299fdfea25b07978017221c41cadf19b03883078332e69636f6e";
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", StringUtil.hexToBytes(hex));
        reader.beginList();
        //eth address
        byte[] bmcAddresss = reader.readByteArray();
        String netAddr = reader.readString();
        reader.end();
        System.out.println("bmcAddresss:"+StringUtil.bytesToHex(bmcAddresss));
        System.out.println("netAddr:"+netAddr);

        //validatorset
        //https://github.com/tendermint/tendermint/blob/master/proto/tendermint/types/validator.proto
        //*tmtypes.ValidatorSet
        hex = "4d1be64f0e9a466c2e66a53433928192783e29f8fa21beb2133499b5ef770f60000000e8d4a5100099308aa365c40554bc89982af505d85da95251445d5dd4a9bb37dd2584fd92d3000000e8d4a5100001776920ff0b0f38d78cf95c033c21adf7045785114e392a7544179652e0a612000000e8d4a51000";
        byte[] bs =StringUtil.hexToBytes(hex);
        System.out.println(bs.length);
        System.out.printf("<"+StringUtil.bytesToHex(new byte[]{bs[0]}));
        for (int i=1;i<bs.length;i++) {
            System.out.printf(" "+StringUtil.bytesToHex(new byte[]{bs[i]}));
        }
        System.out.println(">");
    }
}
