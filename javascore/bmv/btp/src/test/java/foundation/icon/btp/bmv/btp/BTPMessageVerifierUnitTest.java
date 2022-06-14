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

    static final String HEX_SRC_NETWORK_ID = "6274703a2f2f3078312e69636f6e";
    static final long networkTypeID = 0;
    //NewVerifierWithFirstBlock: {uid: icon, srcNetworkID: 6274703a2f2f3078312e69636f6e, networkTypeID: 0, firstBu: BlockUpdate{MainHeight:20, Round:0, NextProofContextHash:0dbeede791c199ab61a07713353ae3bbf42d6151135c4fd7c46968ab94d6b05f, NetworkSectionToRoot:[{Dir:0,Value:81b0464a034320a47797cac3988ca6d930957d15f70cb665d2c8a81bfb348f7b}], NetworkID:2, FirstMessageSn:0, IsUpdateProofContext:true, PrevNetworkSectionHash:, MessageCount:3, MessagesRoot:4639491c3e6686de4eac3cd946829eaa31fbb3cbb282144a4b82b606784b2b51, Proof:, NextProofContext:f85af85895004d0f682a02b4196ef5786486f643b548d814a642950068546d6425337e4068544f8a569cea057877d9c6950012fe3a124c6016a93e5a1872bc10caf6ee26530e9500b361c9fb1f8c8448b72b9aa79b7044ca744f922f}}
    //NewVerifierWithFirstBlock: {uid: icon, srcNetworkID: 6274703a2f2f3078312e69636f6e, networkTypeID: 0, firstBu: BlockUpdate{MainHeight:20, Round:0, NextProofContextHash:abcba819957542c61d3aea2a815e314d920c264eb8fa27135b33d17e050936c5, NetworkSectionToRoot:[{Dir:0,Value:81b0464a034320a47797cac3988ca6d930957d15f70cb665d2c8a81bfb348f7b}], NetworkID:2, FirstMessageSn:0, IsUpdateProofContext:true, PrevNetworkSectionHash:, MessageCount:3, MessagesRoot:4639491c3e6686de4eac3cd946829eaa31fbb3cbb282144a4b82b606784b2b51, Proof:, NextProofContext:f85af85895000cf2e15e709b5a4768815ba76c1a9e7a830446c695006ec2a3b8a3938d3ae29ca3196af9454e25a5e0b79500b2527c7f246258fc27116549d6b0ad10e3794d379500b6a35b1f2029ba34306787e14b420bc1614d9644}}
    //Relay height:20 alias:icon2 networkTypeID:0 networkID:2 partialMsgsList:[[dog cat] [elephant]]
    static final String BASE64_FIRST_BLOCK_UPDATE = "-M0UAKCry6gZlXVCxh066iqBXjFNkgwmTrj6JxNbM9F-BQk2xePiAKCBsEZKA0MgpHeXysOYjKbZMJV9FfcMtmXSyKgb-zSPewIB-AADoEY5SRw-ZobeTqw82UaCnqox-7PLsoIUSkuCtgZ4SytR-AC4XPha-FiVAAzy4V5wm1pHaIFbp2wannqDBEbGlQBuwqO4o5ONOuKcoxlq-UVOJaXgt5UAslJ8fyRiWPwnEWVJ1rCtEON5TTeVALajWx8gKbo0MGeH4UtCC8FhTZZE";
    //RelayMessage{Messages[{Type:2,Payload:MessageProof{ProofInLeft[], Contents[dog, cat], ProofInRight[{NumOfLeaf:1,Value:6ecf3422e9b5463a79c535bf0db8e1c6f589bbce30ec9ebb3cf4fe947d24cf79}]}}]}
    //RelayMessage{Messages[{Type:2,Payload:MessageProof{ProofInLeft[{NumOfLeaf:2,Value:7ab8c1b1d0f4c2eb47b9c90496e916a07552a4c14b8593f919e07435fdb8bf82}], Contents[elephant], ProofInRight[]}}]}
    static final List<String> BASE64_FIRST_RELAY_MESSAGES = List.of(
            "8_LxAq_uwMiDZG9ng2NhdOPiAaBuzzQi6bVGOnnFNb8NuOHG9Ym7zjDsnrs89P6UfSTPeQ==",
            "9PPyArDv4-ICoHq4wbHQ9MLrR7nJBJbpFqB1UqTBS4WT-RngdDX9uL-CyYhlbGVwaGFudMA="
    );
    static final List<String[]> FIRST_PARTIAL_MESSAGES = List.of(
            new String[]{"dog", "cat"},
            new String[]{"elephant"}
    );
    //Relay height:30 alias:icon2 networkTypeID:0 networkID:2 partialMsgsList:[[bird monkey lion] [tiger dog] [cat elephant]]
    //RelayMessage{Messages[{Type:1,Payload:BlockUpdate{MainHeight:30, Round:0, NextProofContextHash:abcba819957542c61d3aea2a815e314d920c264eb8fa27135b33d17e050936c5, NetworkSectionToRoot:[{Dir:0,Value:2d96fb566d6f652b74d47b7e00fabd7bb1c2305cefa86c54533de34600bbc0dd}], NetworkID:2, FirstMessageSn:3, IsUpdateProofContext:false, PrevNetworkSectionHash:9d34304f3be3a007df698a66ee24133866ce091eb7e90cc1157e8fc1075b1b57, MessageCount:7, MessagesRoot:02d5723ef02c78e29199cdc6d55a2d67ac6ec0b098b5455bc3c42ee329198c47, Proof:f9010ff9010cb841720a52995a73b1440ecd2f31d3b58f5dcc8a1ca074c63c10a35f452d43392e282b4e448ce9988bbb3b5d2b07cc57b488eca932c3e6476aba3715e251cf1daa2700b8417f334c990990742d0b8a493da74be7a2f7a835b8d0b930b06e66908cc750fa4a36bbbc60b5be801c930989f1424f5905351bcad83ac0cb72e384e284f1ad064800b841f7d9491a33ddc0973922e4b90c1a2668d4986185c292ce1c271ba83341e338fb148d062e80723dbd335b36e89e649f2c711871c543edf9374edabcb19b65433101b8413a530389f5b16560d66d248d0ae191db490593a5e39aaa424d8301ae7377abc469433188ef69fcc92ea58773a952fe539c32654ef948a15cb399a038b97f2d5d00, NextProofContext:}},{Type:2,Payload:MessageProof{ProofInLeft[], Contents[bird, monkey, lion], ProofInRight[{NumOfLeaf:1,Value:1bb00f682e1b26ab9e36ee7f9f874947bc00b5926c69451a22dc99fe8cfd21c6}, {NumOfLeaf:3,Value:4639491c3e6686de4eac3cd946829eaa31fbb3cbb282144a4b82b606784b2b51}]}}]}
    static final String SECOND_RELAY_MESSAGE = "-QIS-QIP-QGqAbkBpvkBox4AoKvLqBmVdULGHTrqKoFeMU2SDCZOuPonE1sz0X4FCTbF4-IAoC2W-1Ztb2UrdNR7fgD6vXuxwjBc76hsVFM940YAu8DdAgagnTQwTzvjoAffaYpm7iQTOGbOCR636QzBFX6PwQdbG1cHoALVcj7wLHjikZnNxtVaLWesbsCwmLVFW8PELuMpGYxHuQES-QEP-QEMuEFyClKZWnOxRA7NLzHTtY9dzIocoHTGPBCjX0UtQzkuKCtORIzpmIu7O10rB8xXtIjsqTLD5kdqujcV4lHPHaonALhBfzNMmQmQdC0Likk9p0vnoveoNbjQuTCwbmaQjMdQ-ko2u7xgtb6AHJMJifFCT1kFNRvK2DrAy3LjhOKE8a0GSAC4QffZSRoz3cCXOSLkuQwaJmjUmGGFwpLOHCcbqDNB4zj7FI0GLoByPb0zWzbonmSfLHEYccVD7fk3Ttq8sZtlQzEBuEE6UwOJ9bFlYNZtJI0K4ZHbSQWTpeOaqkJNgwGuc3erxGlDMYjvafzJLqWHc6lS_lOcMmVO-UihXLOZoDi5fy1dAPgA-GACuF34W8DRhGJpcmSGbW9ua2V5hGxpb274RuIBoBuwD2guGyarnjbuf5-HSUe8ALWSbGlFGiLcmf6M_SHG4gOgRjlJHD5mht5OrDzZRoKeqjH7s8uyghRKS4K2BnhLK1E=";
    //RelayMessage{Messages[{Type:2,Payload:MessageProof{ProofInLeft[{NumOfLeaf:2,Value:7382610d5f281c44a68856aa63e1cf0ca392897f6452ddedd4b69ff03a7059a5}, {NumOfLeaf:1,Value:001effe92943afef1178307036e46a606b99bf973f03593ad34e81226efae614}], Contents[tiger, dog], ProofInRight[{NumOfLeaf:1,Value:d616607d3e4ba96a74f323cffc5f20a3c78e7cab8ecbdbb03b13fa8ffc9bf644}, {NumOfLeaf:1,Value:6ecf3422e9b5463a79c535bf0db8e1c6f589bbce30ec9ebb3cf4fe947d24cf79}]}}]}
    static final String SECOND_PARTIAL_RELAY_MESSAGES1 = "-KT4ovigArid-Jv4RuICoHOCYQ1fKBxEpohWqmPhzwyjkol_ZFLd7dS2n_A6cFml4gGgAB7_6SlDr-8ReDBwNuRqYGuZv5c_A1k6006BIm765hTKhXRpZ2Vyg2RvZ_hG4gGg1hZgfT5LqWp08yPP_F8go8eOfKuOy9uwOxP6j_yb9kTiAaBuzzQi6bVGOnnFNb8NuOHG9Ym7zjDsnrs89P6UfSTPeQ==";
    //RelayMessage{Messages[{Type:2,Payload:MessageProof{ProofInLeft[{NumOfLeaf:4,Value:27c3d713e3678c3028e8d93ae7b3a8d623573bce29fa6b78cf01df67c1935c7b}, {NumOfLeaf:1,Value:05cd98fdecc74538182a123f3d91e031833da3e9b0a2558d6652e48bf318a1b2}], Contents[cat, elephant], ProofInRight[]}}]}
    static final String SECOND_PARTIAL_RELAY_MESSAGES2 = "-GD4XvhcArhZ-Ff4RuIEoCfD1xPjZ4wwKOjZOuezqNYjVzvOKfpreM8B32fBk1x74gGgBc2Y_ezHRTgYKhI_PZHgMYM9o-mwolWNZlLki_MYobLNg2NhdIhlbGVwaGFudMA=";
    //Relay height:40 alias:icon2 networkTypeID:0 networkID:2 partialMsgsList:[[]]
    //RelayMessage{Messages[{Type:1,Payload:BlockUpdate{MainHeight:40, Round:0, NextProofContextHash:575d775cc65e27576681100965312b771408b50dee13dc7b57cd5da2ffa353ed, NetworkSectionToRoot:[{Dir:0,Value:fdf48179be2758df70f50a5c58ed870627b03f73180afe5b47ae8fbacb7f9ea3}], NetworkID:2, FirstMessageSn:10, IsUpdateProofContext:true, PrevNetworkSectionHash:13313f5834b85cc2cdedcb067d13eb3ce87bfa0cd883b3a5502c5cbef254d784, MessageCount:0, MessagesRoot:, Proof:f9010ff9010cb84187bbe328ea2bedc28174fac235869e303098f13ef47ab31dbb688254bbcb8bbd68b580ffd89594cdbb4260ea1c0d56ed2398620b1634e85e3210118ad07c2fd200b841ab0dcd072ce7afdb6684947c0d53c78ec7c482aa8ded865f37314c8c361fc6a203e44217549c506007678538ab785eed2e3347574cee74c90bf1897e1764070900b841c86646fbd044456438393bdcf044c20c8be594240491c77cdddddb38b87347a229204b08b81b2f91756ec252679ac507b5da3cc73dd62f6e51fd44b5cfb1012701b84104ce2ec719c2a0dce624514f34062a009df11ad3bf1ed0535436cda86ed8a8fd12af65606ba09da47f437b5e7d7331e3b632aaf0f96a86cc476beac9ebc5020700, NextProofContext:f85af8589500fbb55c86aa6cb242f92aed555e2977b64c992eaa9500770208052c664e04d06241cd0ac3a1ec36e561159500d990781b536dadb257cdf7ad660fc045cf2c60ac9500cd143a5492bfd3ad7258bab01d26eb62ec44c977}}]}
    static final String THIRD_RELAY_MESSAGE = "-QHt-QHq-QHnAbkB4_kB4CgAoFddd1zGXidXZoEQCWUxK3cUCLUN7hPce1fNXaL_o1Pt4-IAoP30gXm-J1jfcPUKXFjthwYnsD9zGAr-W0euj7rLf56jAhWgEzE_WDS4XMLN7csGfRPrPOh7-gzYg7OlUCxcvvJU14QA-AC5ARL5AQ_5AQy4QYe74yjqK-3CgXT6wjWGnjAwmPE-9HqzHbtoglS7y4u9aLWA_9iVlM27QmDqHA1W7SOYYgsWNOheMhARitB8L9IAuEGrDc0HLOev22aElHwNU8eOx8SCqo3thl83MUyMNh_GogPkQhdUnFBgB2eFOKt4Xu0uM0dXTO50yQvxiX4XZAcJALhByGZG-9BERWQ4OTvc8ETCDIvllCQEkcd83d3bOLhzR6IpIEsIuBsvkXVuwlJnmsUHtdo8xz3WL25R_US1z7EBJwG4QQTOLscZwqDc5iRRTzQGKgCd8RrTvx7QU1Q2zahu2Kj9Eq9lYGugnaR_Q3tefXMx47YyqvD5aobMR2vqyevFAgcAuFz4WvhYlQD7tVyGqmyyQvkq7VVeKXe2TJkuqpUAdwIIBSxmTgTQYkHNCsOh7DblYRWVANmQeBtTba2yV833rWYPwEXPLGCslQDNFDpUkr_TrXJYurAdJuti7ETJdw==";
    static final List<String[]> SECOND_PARTIAL_MESSAGES = List.of(
            new String[]{"bird", "monkey", "lion"},
            new String[]{"tiger", "dog"},
            new String[]{"cat", "elephant"}
    );

    static Score score;

    @BeforeAll
    public static void setup() throws Exception {
        score = sm.deploy(owner, BTPMessageVerifier.class,
                StringUtil.hexToBytes(HEX_SRC_NETWORK_ID),
                0,
                bmc.toString(),
                Base64.getUrlDecoder().decode(BASE64_FIRST_BLOCK_UPDATE.getBytes()));
    }

    @Test
    public void handleRelayMessage() {
        List<String> base64Msgs = new ArrayList<>(BASE64_FIRST_RELAY_MESSAGES);
        base64Msgs.add(SECOND_RELAY_MESSAGE);
        base64Msgs.add(SECOND_PARTIAL_RELAY_MESSAGES1);
        base64Msgs.add(SECOND_PARTIAL_RELAY_MESSAGES2);
        base64Msgs.add(THIRD_RELAY_MESSAGE);
        List<String[]> partialMsgsList = new ArrayList<>(FIRST_PARTIAL_MESSAGES);
        partialMsgsList.add(SECOND_PARTIAL_MESSAGES.get(0));
        partialMsgsList.add(SECOND_PARTIAL_MESSAGES.get(1));
        partialMsgsList.add(SECOND_PARTIAL_MESSAGES.get(2));
        partialMsgsList.add(new String[0]);
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