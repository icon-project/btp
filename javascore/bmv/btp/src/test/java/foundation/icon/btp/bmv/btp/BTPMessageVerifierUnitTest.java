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

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import scorex.util.Base64;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BTPMessageVerifierUnitTest extends TestBase {
    static final ServiceManager sm = getServiceManager();
    static final Account owner = sm.createAccount();

    static final BTPAddress prev = BTPIntegrationTest.Faker.btpLink();
    static final Account bmcAccount = Account.newScoreAccount(Integer.MAX_VALUE);
    static final BTPAddress bmc = new BTPAddress(BTPIntegrationTest.Faker.btpNetwork(),
            bmcAccount.getAddress().toString());

//        NewVerifierWithFirstBlock: {uid: icon, srcNetworkID: 6274703a2f2f3078312e69636f6e, networkTypeID: 0, firstBu: BlockUpdate{MainHeight:20, Round:0, NextProofContextHash:0dbeede791c199ab61a07713353ae3bbf42d6151135c4fd7c46968ab94d6b05f, NetworkSectionToRoot:[{Dir:0,Value:81b0464a034320a47797cac3988ca6d930957d15f70cb665d2c8a81bfb348f7b}], NetworkID:2, FirstMessageSn:0, IsUpdateProofContext:true, PrevNetworkSectionHash:, MessageCount:3, MessagesRoot:4639491c3e6686de4eac3cd946829eaa31fbb3cbb282144a4b82b606784b2b51, Proof:, NextProofContext:f85af85895004d0f682a02b4196ef5786486f643b548d814a642950068546d6425337e4068544f8a569cea057877d9c6950012fe3a124c6016a93e5a1872bc10caf6ee26530e9500b361c9fb1f8c8448b72b9aa79b7044ca744f922f}}
//        Base64Encoding: -M0UAKANvu3nkcGZq2GgdxM1OuO79C1hURNcT9fEaWirlNawX-PiAKCBsEZKA0MgpHeXysOYjKbZMJV9FfcMtmXSyKgb-zSPewIB-AADoEY5SRw-ZobeTqw82UaCnqox-7PLsoIUSkuCtgZ4SytR-AC4XPha-FiVAE0PaCoCtBlu9XhkhvZDtUjYFKZClQBoVG1kJTN-QGhUT4pWnOoFeHfZxpUAEv46EkxgFqk-WhhyvBDK9u4mUw6VALNhyfsfjIRItyuap5twRMp0T5Iv
//        Relay height:20 alias:icon2 networkTypeID:0 networkID:2 partialMsgsList:[[dog cat] [elephant]]
//        RelayMessage{Messages[{Type:2,Payload:MessageProof{ProofInLeft[], Contents[dog, cat], ProofInRight[{NumOfLeaf:1,Value:6ecf3422e9b5463a79c535bf0db8e1c6f589bbce30ec9ebb3cf4fe947d24cf79}]}}]}
//        Base64Encoding: 8_LxAq_uwMiDZG9ng2NhdOPiAaBuzzQi6bVGOnnFNb8NuOHG9Ym7zjDsnrs89P6UfSTPeQ==
//        RelayMessage{Messages[{Type:2,Payload:MessageProof{ProofInLeft[{NumOfLeaf:2,Value:7ab8c1b1d0f4c2eb47b9c90496e916a07552a4c14b8593f919e07435fdb8bf82}], Contents[elephant], ProofInRight[]}}]}
//        Base64Encoding: 9PPyArDv4-ICoHq4wbHQ9MLrR7nJBJbpFqB1UqTBS4WT-RngdDX9uL-CyYhlbGVwaGFudMA=

    static final String HEX_SRC_NETWORK_ID = "6274703a2f2f3078312e69636f6e";
    static final long networkTypeID = 0;
    static final String BASE64_FIRST_BLOCK_UPDATE = "-M0UAKANvu3nkcGZq2GgdxM1OuO79C1hURNcT9fEaWirlNawX-PiAKCBsEZKA0MgpHeXysOYjKbZMJV9FfcMtmXSyKgb-zSPewIB-AADoEY5SRw-ZobeTqw82UaCnqox-7PLsoIUSkuCtgZ4SytR-AC4XPha-FiVAE0PaCoCtBlu9XhkhvZDtUjYFKZClQBoVG1kJTN-QGhUT4pWnOoFeHfZxpUAEv46EkxgFqk-WhhyvBDK9u4mUw6VALNhyfsfjIRItyuap5twRMp0T5Iv";
    static final List<String> BASE64_FIRST_RELAY_MESSAGES = List.of(
            "8_LxAq_uwMiDZG9ng2NhdOPiAaBuzzQi6bVGOnnFNb8NuOHG9Ym7zjDsnrs89P6UfSTPeQ==",
            "9PPyArDv4-ICoHq4wbHQ9MLrR7nJBJbpFqB1UqTBS4WT-RngdDX9uL-CyYhlbGVwaGFudMA="
    );
    static final List<String[]> FIRST_PARTIAL_MESSAGES = List.of(
            new String[]{"dog", "cat"},
            new String[]{"elephant"}
    );

    static Score score;

    @BeforeAll
    public static void setup() throws Exception {
        score = sm.deploy(owner, BTPMessageVerifier.class,
                StringUtil.hexToBytes(HEX_SRC_NETWORK_ID),
                0,
                bmcAccount.getAddress(),
                Base64.getUrlDecoder().decode(BASE64_FIRST_BLOCK_UPDATE.getBytes()));
    }

    @Test
    public void handleRelayMessage() {
        List<String> base64Msgs = new ArrayList<>(BASE64_FIRST_RELAY_MESSAGES);
        base64Msgs.add("-QIS-QIP-QGqAbkBpvkBox4AoA2-7eeRwZmrYaB3EzU647v0LWFRE1xP18RpaKuU1rBf4-IAoC2W-1Ztb2UrdNR7fgD6vXuxwjBc76hsVFM940YAu8DdAgagnTQwTzvjoAffaYpm7iQTOGbOCR636QzBFX6PwQdbG1cHoALVcj7wLHjikZnNxtVaLWesbsCwmLVFW8PELuMpGYxHuQES-QEP-QEMuEEwJq2w-dvt3ISIByBuIqNDwCjd6csDlK7j2lcJIaK4z02Zo4uUI1SSi89VuzPobHCzlc_BH4-q2iYeVFjd8kqwAbhBI769o0LY1pMj0NOu3MJg56V1fjdHdnQXkh5IzPOijEkQ50mK-vG6tmicpGqEJSeFnUZD4QAFF2RLmrOmhGJ-SgC4QZoUxHXLaHShjZV0K2D3ZGX6rKHdfuxbENslr1tkpSY0ceIp3fVYZMfx5XRx5DNr0L3OwX3pInEforzU8hoKLf0BuEG4gra7C3oohMbVrQVskWgfHrZuwBG_N9LoIX7iiRHRyGfJwf2eo91PbTBEgRST_jXBGR9dO9mJWIiiPTih2geaAPgA-GACuF34W8DRhGJpcmSGbW9ua2V5hGxpb274RuIBoBuwD2guGyarnjbuf5-HSUe8ALWSbGlFGiLcmf6M_SHG4gOgRjlJHD5mht5OrDzZRoKeqjH7s8uyghRKS4K2BnhLK1E=");
        List<String[]> partialMsgsList = new ArrayList<>(FIRST_PARTIAL_MESSAGES);
        partialMsgsList.add(new String[]{"bird", "monkey", "lion"});
        long seq = 0;
        for (int i = 0; i < base64Msgs.size(); i++) {
            String base64Msg = base64Msgs.get(i);
            byte[][] ret = (byte[][]) sm.call(bmcAccount, BigInteger.ZERO, score.getAddress(),
                    "handleRelayMessage",
                    bmc.toString(), prev.toString(), BigInteger.valueOf(seq), base64Msg);
            String[] partialMsgs = partialMsgsList.get(i);
            assertEquals(partialMsgs.length, ret.length);
            for (int j = 0; j < ret.length; j++) {
                String msg = new String(ret[j]);
                assertEquals(partialMsgs[j], msg);
                System.out.printf("seq:%d msg:%s\n", seq, msg);
                seq++;
            }
        }
    }
}