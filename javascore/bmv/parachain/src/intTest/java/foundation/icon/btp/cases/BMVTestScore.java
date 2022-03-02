/*
 * Copyright 2020 ICONLOOP Inc.
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

package foundation.icon.test.cases;

import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.Bytes;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcError;
import foundation.icon.icx.data.TransactionResult;

import foundation.icon.test.Env;
import foundation.icon.test.TestBase;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import scorex.util.ArrayList;
import scorex.util.Base64;

import score.util.Crypto;

import static foundation.icon.test.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.TestMethodOrder;

import okhttp3.OkHttpClient;
import org.web3j.rlp.RlpEncoder;
import org.web3j.rlp.RlpString;
import org.web3j.rlp.RlpList;
import org.web3j.rlp.RlpType;

@TestMethodOrder(OrderAnnotation.class)
public class BMVTestScore extends TestBase {
    private static final boolean DEBUG = true;
    private static Address ZERO_ADDRESS;
    private static TransactionHandler txHandler;
    private static KeyWallet[] wallets;
    private static KeyWallet ownerWallet, caller;
    private static List<byte[]> validatorPublicKeys;
    private static List<byte[]> validatorSecretKey;
    private static BMVScore bmvScore;
    private static KusamaEventDecoderScore kusamaEventDecoderScore;
    private static MoonriverEventDecoderScore moonriverEventDecoderScore;
    private static String bmc;
    private static String destinationNet = "0x9876.edge";
    private static String sourceNet = "0x1234.icon";
    private static boolean mtaIsAllowNewerWitness = true;
    private static byte[] paraLastBlockHash = HexConverter.hexStringToByteArray("08557f0ca4d319f559310f5d62b38643d0a0555b04efe3e1589f869d052ff9f2");
    private static byte[] relayLastBlockHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
    private static String bmcBTPAddress;
    private static BigInteger currentSetId = BigInteger.valueOf(123);
    private static BigInteger paraChainId = BigInteger.valueOf(1001);
    private static long paraMtaOffset = 10;
    private static long relayMtaOffset = 100;
    private static int mtaRootSize = 10;
    private static int mtaCacheSize = 3;
    private static List<RelayBlockUpdate> relayUpdatedBlocks = new ArrayList<RelayBlockUpdate>(10);
    private static List<ParaBlockUpdate> paraUpdatedBlocks = new ArrayList<ParaBlockUpdate>(10);

    private byte[] getConcatenationHash(byte[] item1, byte[] item2) {
        byte[] concatenation = new byte[item1.length + item2.length];
        System.arraycopy(item1, 0, concatenation, 0, item1.length);
        System.arraycopy(item2, 0, concatenation, item1.length, item2.length);  
        return Crypto.sha3_256(concatenation);
    }

    @BeforeAll
    static void setup() throws Exception {
        ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
        validatorPublicKeys = new ArrayList<byte[]>(5);
        validatorSecretKey = new ArrayList<byte[]>(5);

        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        IconService iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL(), 3));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        wallets = new KeyWallet[2];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(200));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
        ownerWallet = wallets[0];
        caller = wallets[1];

        bmc = ownerWallet.getAddress().toString();
        bmcBTPAddress = "btp://" + sourceNet + "/" + ownerWallet.getAddress().toString();

        // initialize validators key
        validatorPublicKeys.add(HexConverter.hexStringToByteArray("25aabce1a96af5c3f6a4c780b6eeeb7769bf32884f1bad285b5faa464e25c487"));
        validatorPublicKeys.add(HexConverter.hexStringToByteArray("8e111029cd8a3754095d96e9d01629f7ee686198b9e8465d9c90101ed867b958"));
        validatorPublicKeys.add(HexConverter.hexStringToByteArray("01573582dc1bc7474e76cd5a5cbefbcf5475284ced05cc66a45dcb65ea29b09b"));
        validatorPublicKeys.add(HexConverter.hexStringToByteArray("d3e7471c8fd4cdb71e791783098794e2295f992ae6a4a27e0f893071ade31b78"));

        validatorSecretKey.add(HexConverter.hexStringToByteArray("c360fab3025db65e3d967f553ddae434382cfbb9130b6de339fd1f3283f350a025aabce1a96af5c3f6a4c780b6eeeb7769bf32884f1bad285b5faa464e25c487"));
        validatorSecretKey.add(HexConverter.hexStringToByteArray("f5880449f4eb08bfa946a6f5bb679a33f58a7f1bd93c52bb4f2e05dad27e45dc8e111029cd8a3754095d96e9d01629f7ee686198b9e8465d9c90101ed867b958"));
        validatorSecretKey.add(HexConverter.hexStringToByteArray("e8c61ebe8654dfcfc279d28eec8af09d1149bbc9ba26049354e880d479ef98f101573582dc1bc7474e76cd5a5cbefbcf5475284ced05cc66a45dcb65ea29b09b"));
        validatorSecretKey.add(HexConverter.hexStringToByteArray("8f97e18992ed2fb7b7f7e37146e2107cb950556cead04c347ad81c69b62ad51fd3e7471c8fd4cdb71e791783098794e2295f992ae6a4a27e0f893071ade31b78"));

        List<RlpType> listRlpValidators = new ArrayList<RlpType>(5);
        for (byte[] validator: validatorPublicKeys) {
            listRlpValidators.add(RlpString.create(validator));
        }
        // RLP encode validators public key
        byte[] rlpEncodeValidators = RlpEncoder.encode(new RlpList(listRlpValidators));
        // base 64 encoded validator public key
        String encodedValidators = new String(Base64.getUrlEncoder().encode(rlpEncodeValidators));
        kusamaEventDecoderScore = KusamaEventDecoderScore.mustDeploy(
            txHandler,
            ownerWallet
        );
        moonriverEventDecoderScore = MoonriverEventDecoderScore.mustDeploy(
            txHandler,
            ownerWallet
        );
        bmvScore = BMVScore.mustDeploy(
            txHandler,
            ownerWallet,
            bmc,
            destinationNet,
            encodedValidators,
            relayMtaOffset,
            paraMtaOffset,
            mtaRootSize,
            mtaCacheSize,
            mtaIsAllowNewerWitness,
            relayLastBlockHash,
            paraLastBlockHash,
            kusamaEventDecoderScore.getAddress(),
            moonriverEventDecoderScore.getAddress(),
            currentSetId,
            paraChainId
        );

        // check contract initialized successfully
        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        assertTrue(relayMtaHeight.equals(BigInteger.valueOf(relayMtaOffset)));

        BigInteger paraMtaHeight = bmvScore.paraMtaHeight(); 
        assertTrue(paraMtaHeight.equals(BigInteger.valueOf(paraMtaOffset)));

        List<byte[]> relayMtaRoot = bmvScore.relayMtaRoot();
        assertEquals(relayMtaRoot.size(), mtaRootSize);

        List<byte[]> paraMtaRoot = bmvScore.relayMtaRoot();
        assertEquals(paraMtaRoot.size(), mtaRootSize);

        byte[] relayMtaLastBlockHash = bmvScore.relayMtaLastBlockHash();
        assertArrayEquals(relayMtaLastBlockHash, relayLastBlockHash);

        byte[] paraMtaLastBlockHash = bmvScore.paraMtaLastBlockHash();
        assertArrayEquals(paraMtaLastBlockHash, paraLastBlockHash);

        BigInteger relayMtaOffsetResult = bmvScore.relayMtaOffset();
        assertTrue(relayMtaOffsetResult.equals(BigInteger.valueOf(relayMtaOffset)));

        BigInteger paraMtaOffsetResult = bmvScore.paraMtaOffset();
        assertTrue(paraMtaOffsetResult.equals(BigInteger.valueOf(paraMtaOffset)));

        List<byte[]> relayMtaCaches = bmvScore.relayMtaCaches();
        assertEquals(relayMtaCaches.size(), 0);

        List<byte[]> paraMtaCaches = bmvScore.paraMtaCaches();
        assertEquals(paraMtaCaches.size(), 0);

        Address bmcAddress = bmvScore.bmc();
        assertTrue(bmcAddress.toString().equals(bmc));

        Address relayEventDecoderAddress = bmvScore.relayEventDecoder();
        assertTrue(relayEventDecoderAddress.equals(kusamaEventDecoderScore.getAddress()));

        Address paraEventDecoderAddress = bmvScore.paraEventDecoder();
        assertTrue(paraEventDecoderAddress.equals(moonriverEventDecoderScore.getAddress()));

        String netAddressResult = bmvScore.netAddress();
        assertTrue(netAddressResult.equals(destinationNet));

        BigInteger lastHeightResult = bmvScore.lastHeight();
        assertTrue(lastHeightResult.equals(BigInteger.valueOf(paraMtaOffset)));

        List<byte[]> validators = bmvScore.validators();
        assertEquals(validators.size(), validatorPublicKeys.size());
    }

    /**
     * ---------------------------------------------------------------------------------------------
     *                            RECEIVING A RELAY MESSAGE FROM BMC
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * 
     * Scenario 1: previous bmc is not belong to network that BMV handle
     * Given:
     *    prev: btp://0xffff.eos/0x12345
     * When:
     *    network that BMV handle: 0x9876.edge
     * Then:
     *    throw error:
     *    message: "not acceptable from"
     *    code: NOT_ACCEPTED_FROM_NETWORK_ERROR 38
     */
    @Test
    @Order(1)
    public void receivingScenario1() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String invalidNetAddress = "0xffff.eos"; // valid is 0x1234.icon
        String prev = "btp://" + invalidNetAddress + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        String relayMessageEncoded = "0x1234";

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, relayMessageEncoded);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.NOT_ACCEPTED_FROM_NETWORK_ERROR + ")"));
    }

    /**
     * 
     * Scenario 2: transaction caller is not bmc
     * Given:
     *    call `handleRelayMessage` with caller "caller"
     * When:
     *    registered bmc address: ownerWallet.getAddress() generated when setup
     * Then:
     *    throw error:
     *    message: "not acceptable bmc"
     *    code: NOT_ACCEPTED_BMC_ADDR_ERROR 39
     */
    @Test
    @Order(2)
    public void receivingScenario2() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        String relayMessageEncoded = "0x1234";

        Bytes id = bmvScore.handleRelayMessage(caller, bmcBTPAddress, prev, seq, relayMessageEncoded);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.NOT_ACCEPTED_BMC_ADDR_ERROR + ")"));
    }

    /**
     * 
     * Scenario 3: bmc is invalid
     * Given:
     *    bmc: btp://0x1234.icon/caller.getAddress()
     * When:
     *    registered bmc address: ownerWallet.getAddress() generated when setup
     * Then:
     *    throw error:
     *    message: "not acceptable bmc"
     *    code: NOT_ACCEPTED_BMC_ADDR_ERROR 39
     */
    @Test
    @Order(3)
    public void receivingScenario3() throws Exception {
        String invalidBmcBTPAddress = "btp://" + sourceNet + "/" + caller.getAddress().toString(); // valid is owner address
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        String relayMessageEncoded = "0x1234";

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, invalidBmcBTPAddress, prev, seq, relayMessageEncoded);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.NOT_ACCEPTED_BMC_ADDR_ERROR + ")"));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     *                            EXTRACTING THE MESSAGE FROM BMC
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * 
     * Scenario 1: input relay message with invalid base64 format
     * Given:
     *    msg: invalid base64 format
     * When:
     *
     * Then:
     *    throw error:
     *    message: "decode base64 msg error: "
     *    code: DECODE_ERROR 37
     */
    @Test
    @Order(4)
    public void extractingScenario1() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        String relayMessageEncoded = "abcedgef=="; // invalid base64 formart of relay message

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, relayMessageEncoded);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.DECODE_ERROR + ")"));
    }

    /**
     * 
     * Scenario 2: input relay message with invalid RLP format
     * Given:
     *    msg: invalid RLP encoded format
     * When:
     *
     * Then:
     *    throw error:
     *    message: "RelayMessage RLP decode error: "
     *    code: DECODE_ERROR 37
     */
    @Test
    @Order(5)
    public void extractingScenario2() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");

        List<RlpType> blockUpdate = new ArrayList<RlpType>(3);
        blockUpdate.add(RlpString.create("abc")); // invalid block encode

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdate)); // invalid block update
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.DECODE_ERROR + ")"));
    }

    /**
     * 
     * Scenario 3: input relay message with empty BlockUpdate and BlockProof
     * Given:
     *    msg: empty BlockUpdate and BlockProof
     * When:
     *
     * Then:
     *    throw error:
     *    message: "invalid RelayMessage not exists BlockUpdate or BlockProof"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(6)
    public void extractingScenario3() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update
        relayMessage.add(RlpString.create("")); // empty block proof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     *                            VERIFYING THE MESSAGE FROM BMC
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * 
     * Scenario 1: input block update without relay chain data and para block
     * Given:
     *    msg:
     *      blockUpdates: [empty]
     * When:
     * 
     * Then:
     *    throw error:
     *    message: "Missing relay chain data"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(7)
    public void verifyingScenario1() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] parentHash = HexConverter.hexStringToByteArray("46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"); // different with lastBlockHash
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(null);
        updatingBlockNumber += 1;
        parentHash = paraBlockUpdate.getHash();
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 2: input relay chain block with invalid parent hash with relay mta last block hash
     * Given:
     *    msg:
     *      relay block 101:
     *         parentHash: "46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"
     * When:
     *    relay mta height: 100
     *    last block hash: "681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249"
     * Then:
     *    throw error:
     *    message: "parent relay block hash does not match, parent: 46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55 current: 681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(8)
    public void verifyingScenario2() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"); // different with lastBlockHash
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatingBlockNumber += 1;
            relayParentHash = relayBlockUpdate.getHash();
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encode()));
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 3: input relay chain block with invalid parent hash in blocks
     * Given:
     *    update relay block 101, 102, 103:
     *      relay block 102 parent hash: "46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"
     * When:
     *    relay block 102 parent hash should be: block11.getHash()
     * Then:
     *    throw error:
     *    message: "parent relay block hash does not match, parent: 46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55 current: block11.getHash()"
     *    code: BMV_ERROR 25
     * 
     */
    @Test
    @Order(9)
    public void verifyingScenario3() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249"); // different with lastBlockHash
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatingBlockNumber += 1;
            relayParentHash = HexConverter.hexStringToByteArray("46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"); // correct is blockUpdate.getHash()
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encode()));
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 4: input relay block with height higher than current updated relay block height
     * Given:
     *    update relay block 109, 110, 111
     * When:
     *    last updated relay block height of BMV: 100
     * Then:
     *    throw error:
     *    message: "invalid relay blockUpdate height: 109; expected: 101"
     *    code: INVALID_BLOCK_UPDATE_HEIGHT_HIGHER 33
     */
    @Test
    @Order(10)
    public void verifyingScenario4() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 109; // should be 101

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatingBlockNumber += 1;
            relayParentHash = relayBlockUpdate.getHash();
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encode()));
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 5: input relay block with height lower than current updated relay block height
     * Given:
     *    update relay block 91, 92, 93
     * When:
     *    last updated block height of BMV: 100
     * Then:
     *    throw error:
     *    message: "invalid relay blockUpdate height: 91; expected: 101"
     *    code: INVALID_BLOCK_UPDATE_HEIGHT_LOWER 34
     */
    @Test
    @Order(11)
    public void verifyingScenario5() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 91; // should be 101

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatingBlockNumber += 1;
            relayParentHash = relayBlockUpdate.getHash();
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encode()));
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_LOWER + ")"));
    }

    /**
     * 
     * Scenario 6: update relay block without votes of validators
     * Given:
     *    update relay block 101, 102, 103
     *    relay block 103 has empty votes
     * When:
     *
     * Then:
     *    throw error:
     *    message: "not exists votes"
     *    code: INVALID_BLOCK_UPDATE 29
     */
    @Test
    @Order(12)
    public void verifyingScenario6() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatingBlockNumber += 1;
            relayParentHash = relayBlockUpdate.getHash();
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_UPDATE + ")"));
    }

    /**
     * 
     * Scenario 7: update relay block with invalid vote, vote for invalid block hash
     * Given:
     *    update relay block 101, 102, 103
     *    validator sign for:
     *       relay block hash: "a5f2868483655605709dcbda6ce0fd21cd0386ae2f99445e052d6b1f1ae6db5b"
     * When:
     *    validator should be sign for:
     *       relay block hash:  block103.getHash()
     * Then:
     *    throw error:
     *    message: "validator signature invalid block hash"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(13)
    public void verifyingScenario7() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatingBlockNumber += 1;
            relayParentHash = relayBlockUpdate.getHash();
            if (i == 2) {
                byte[] invalidVoteRelayBlockHash = HexConverter.hexStringToByteArray("a5f2868483655605709dcbda6ce0fd21cd0386ae2f99445e052d6b1f1ae6db5b");
                byte[] voteMessage = RelayBlockUpdate.voteMessage(invalidVoteRelayBlockHash, relayUpdatingBlockNumber, round, setId); // should not be invalidVoteRelayBlockHash but relayBlockUpdate.getHash()
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage))); // last block of list, update block votes
            } else {
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 8: update relay block with invalid vote, vote for invalid block height
     * Given:
     *    update relay block 101, 102, 103
     *    validator sign for:
     *       block height 102
     * When:
     *    validator should be sign for:
     *       block height: 103
     * Then:
     *    throw error:
     *    message: "validator signature invalid block height"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(14)
    public void verifyingScenario8() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatingBlockNumber += 1;
            relayParentHash = relayBlockUpdate.getHash();
            if (i == 2) {
                long invalidVoteBlockHeight = 12;
                byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), invalidVoteBlockHeight, round, setId); // should not be invalidVoteRelayBlockHash but relayBlockUpdate.getHash()
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage))); // last block of list, update block votes
            } else {
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 9: update relay block with invalid vote, vote of newer validator set id
     * Given:
     *    update block 101, 102, 103
     *    validator vote for set Id: 124
     * When:
     *    current validator set id stored in BMV: 123
     * Then:
     *    throw error:
     *    message: "verify signature for invalid validator set id""
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(15)
    public void verifyingScenario9() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("124");
        BigInteger setId = new BigInteger("92");  // invalid setId
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            if (i == 2) {
                byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber, round, setId); // should not be invalidVoteRelayBlockHash but relayBlockUpdate.getHash()
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));  // last block of list, update block votes
            } else {
                relayUpdatingBlockNumber += 1;
                relayParentHash = relayBlockUpdate.getHash();
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 10: update relay block with invalid vote, signature invalid
     * Given:
     *    update relay block 101, 102, 103
     *    validator1 has invalid signature
     * When:
     *
     * Then:
     *    throw error:
     *    message: "invalid signature"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(16)
    public void verifyingScenario10() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            if (i == 2) {
                byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber, round, setId);
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round.add(BigInteger.valueOf(1)), setId); // invalid vote message
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage))); // last block of list, update block votes
            } else {
                relayUpdatingBlockNumber += 1;
                relayParentHash = relayBlockUpdate.getHash();
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 11: update relay block with invalid vote, signature not belong to validators
     * Given:
     *    update relay block 101, 102, 103
     *    block 103 is signed by key not belong to validators
     * When:
     *
     * Then:
     *    throw error:
     *    message: "one of signature is not belong to validator"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(17)
    public void verifyingScenario11() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        byte[] notValidatorPubicKey = HexConverter.hexStringToByteArray("793d5108b8d128792958a139660102d538c24e9fe70bd396e5c95fe451cdc222");
        byte[] notValidatorSecrectKey = HexConverter.hexStringToByteArray("b9fbcdfe9e6289fadef00225651dc6a4ae3fee160d6f86cce71150b3db691481793d5108b8d128792958a139660102d538c24e9fe70bd396e5c95fe451cdc222");

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            if (i == 2) {
                byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber, round, setId);
                relayBlockUpdate.vote(notValidatorPubicKey, notValidatorSecrectKey, round, setId); // sign by key not belong to validator
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
            } else {
                relayUpdatingBlockNumber += 1;
                relayParentHash = relayBlockUpdate.getHash();
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 12: update relay block with duplicate vote
     * Given:
     *    update block 101, 102, 103
     *    two votes for block 103 are the same
     * When:
     *
     * Then:
     *    throw error:
     *    message: "duplicated signature"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(18)
    public void verifyingScenario12() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249"); // different with lastBlockHash
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            if (i == 2) {
                byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber, round, setId);
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId); // two same signatures
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId); // two same signatures
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
            } else {
                relayUpdatingBlockNumber += 1;
                relayParentHash = relayBlockUpdate.getHash();
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 13: relay block vote not enough 2/3
     * Given:
     *    update block 101, 102, 103
     *    number of vote for block 103: 2
     * When:
     *    number of validators: 4
     *    number of vote to finalize block should be: 3
     * Then:
     *    throw error:
     *    message: "require signature +2/3"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(19)
    public void verifyingScenario13() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            if (i == 2) {
                byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber, round, setId);
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId); // validator 1,2 sign block 103
                relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, setId);
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
            } else {
                relayUpdatingBlockNumber += 1;
                relayParentHash = relayBlockUpdate.getHash();
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 14: update relay block successfully
     * Given:
     *    update relay block 101, 102, 103, 104, 105, 106, 107
     *    number of vote for block 107: 3
     * When:
     *    number of validators: 4
     *    current updated block height: 100
     * Then:
     *    update relay block to relay relay MTA caches
     *    update root of relay MTA
     *    update relay MTA last block hash
     */
    @Test
    @Order(20)
    public void verifyingScenario14() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("681031a8a8da2d3ec9d8da3c8b3c6e4884a3654769d90f09f8e91f2d14cdd249");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long relayUpdatingBlockNumber = 101;

        List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 7; i++) {
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, setId);
            relayUpdatedBlocks.add(relayBlockUpdate);
            relayParentHash = relayBlockUpdate.getHash();
            if (i == 6) {
                byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber, round, setId);
                relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);// validator 1, 2, 3 sign block 103
                relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, setId);
                relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, setId);
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
            } else {
                relayUpdatingBlockNumber += 1;
                relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList(relayBlockUpdates));
        relayChainData.add(RlpString.create("")); // block proof
        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        assertTrue(relayMtaHeight.equals(BigInteger.valueOf(relayUpdatingBlockNumber)));

        List<byte[]> relayMtaRoot = bmvScore.relayMtaRoot();
        assertEquals(relayMtaRoot.size(), mtaRootSize);
        assertArrayEquals(relayMtaRoot.get(0), relayParentHash);

        byte[] relayMtaLastBlockHash = bmvScore.relayMtaLastBlockHash();
        assertArrayEquals(relayMtaLastBlockHash, relayParentHash);

        BigInteger relayMtaOffsetResult = bmvScore.relayMtaOffset();
        assertTrue(relayMtaOffsetResult.equals(BigInteger.valueOf(relayMtaOffset)));

        List<byte[]> relayMtaCaches = bmvScore.relayMtaCaches();
        assertEquals(relayMtaCaches.size(), 3);
        assertArrayEquals(relayMtaCaches.get(2), relayParentHash);

        BigInteger lastHeightResult = bmvScore.lastHeight();
        assertTrue(lastHeightResult.equals(BigInteger.valueOf(paraMtaOffset)));
    }

    /**
     * 
     * Scenario 15: update relay blockProof of block that not exist in MTA
     * Given:
     *    relay blockProof of block 108;
     * When:
     *    current mta height: 107;
     * Then:
     *    throw error:
     *       message: "given block height is newer 108; expected: 103"
     *       code: INVALID_BLOCK_PROOF_HEIGHT_HIGHER, 35
     */
    @Test
    @Order(21)
    public void verifyingScenario15() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = relayLastBlockHash.clone();
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        long relayUpdatingBlockNumber = 108;

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList());  // empty block update

        List<RlpType> relayBlockProof = new ArrayList<RlpType>(2);
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, currentSetId); // new block that not exist in MTA
            relayBlockProof.add(RlpString.create(relayBlockUpdate.getEncodedHeader())); // block header
                List<RlpType> relayBlockWitness = new ArrayList<RlpType>(2);
                relayBlockWitness.add(RlpString.create(relayMtaHeight));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                relayBlockWitness.add(new RlpList(witness));
            relayBlockProof.add(new RlpList(relayBlockWitness));
        relayChainData.add(RlpString.create(RlpEncoder.encode(new RlpList(relayBlockProof)))); // rlp encoded of blockProof

        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_PROOF_HEIGHT_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 16: update relay blockProof with invalid old witness
     * Given:
     *    relay blockProof of block 104;
     *    relay mta height of client: 105;
     * When:
     *    current mta height of BMV: 107;
     *    mta cache size: 3 (stored block hash 107, 106, 105)
     * Then:
     *    throw error:
     *       message: "not allowed old witness"
     *       code: INVALID_BLOCK_WITNESS_OLD, 36
     */
    @Test
    @Order(22)
    public void verifyingScenario16() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = relayLastBlockHash.clone();
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList());  // empty block update

            List<RlpType> relayBlockProof = new ArrayList<RlpType>(2);
            relayBlockProof.add(RlpString.create(relayUpdatedBlocks.get(3).getEncodedHeader())); // block 104
                List<RlpType> relayBlockWitness = new ArrayList<RlpType>(2);
                relayBlockWitness.add(RlpString.create(BigInteger.valueOf(105)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(relayUpdatedBlocks.get(5).getHash())); // block 105 hash
                relayBlockWitness.add(new RlpList(witness));
            relayBlockProof.add(new RlpList(relayBlockWitness));
        relayChainData.add(RlpString.create(RlpEncoder.encode(new RlpList(relayBlockProof)))); // rlp encoded of blockProof

        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS_OLD + ")"));
    }

    /**
     * 
     * Scenario 17: update relay blockProof with invalid block header
     * Given:
     *    relay blockProof of block 105 with incorrect data;
     *    relay mta height of client: 106;
     * When:
     *    current relay mta height of BMV: 107;
     *    client mta height less than BMV mta height
     *    mta cache size: 3 (stored block hash 107, 106, 105)
     * Then:
     *    throw error:
     *       message: "invalid old witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(23)
    public void verifyingScenario17() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        long relayUpdatingBlockNumber = 105;

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList());  // empty block update

            List<RlpType> relayBlockProof = new ArrayList<RlpType>(2);
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, currentSetId); // fake block 15 updated to block proof
            relayBlockProof.add(RlpString.create(relayBlockUpdate.getEncodedHeader())); // block 105
            List<RlpType> relayBlockWitness = new ArrayList<RlpType>(2);
                relayBlockWitness.add(RlpString.create(BigInteger.valueOf(106)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                relayBlockWitness.add(new RlpList(witness));
                witness.add(RlpString.create(relayUpdatedBlocks.get(5).getHash())); // block 105 hash
            relayBlockProof.add(new RlpList(relayBlockWitness));
        relayChainData.add(RlpString.create(RlpEncoder.encode(new RlpList(relayBlockProof)))); // rlp encoded of blockProof

        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 18: update relay blockProof with invalid block witness
     * Given:
     *    relay blockProof of block 105 that has invalid witness
     *    mta height of client: 107;
     * When:
     *    current relay mta height of BMV: 107;
     * Then:
     *    throw error:
     *       message: "invalid witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(24)
    public void verifyingScenario18() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        long relayUpdatingBlockNumber = 105;

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList());  // empty block update

            List<RlpType> relayBlockProof = new ArrayList<RlpType>(2);
            relayBlockProof.add(RlpString.create(relayUpdatedBlocks.get(4).getEncodedHeader())); // block 105
                List<RlpType> relayBlockWitness = new ArrayList<RlpType>(2);
                relayBlockWitness.add(RlpString.create(bmvScore.relayMtaHeight()));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(HexConverter.hexStringToByteArray("9af0b4af1fc9a9d75dbb7e2b72e6560ae2afef30f91625b562adb9659931cc9a"))); // fake witness
                    witness.add(RlpString.create(HexConverter.hexStringToByteArray("679adaf23ccf7fbc51b3c59588357f0c2ec3cacba0595274c8ec5c44354ab8bc")));
                relayBlockWitness.add(new RlpList(witness));
            relayBlockProof.add(new RlpList(relayBlockWitness));

        relayChainData.add(RlpString.create(RlpEncoder.encode(new RlpList(relayBlockProof)))); // rlp encoded of blockProof

        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 19: update relay blockProof with invalid block witness when client MTA height greater than bmv MTA height
     * Given:
     *    blockProof of block 107 that has invalid witness
     *    mta height of client: 108;
     * When:
     *    current mta height of BMV: 107;
     * Then:
     *    throw error:
     *       message: "invalid witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(25)
    public void verifyingScenario19() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        long relayUpdatingBlockNumber = 107;

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList());  // empty block update

            List<RlpType> relayBlockProof = new ArrayList<RlpType>(2);
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber, relayStateRoot, round, currentSetId); // fake block 107 updated to block proof
            relayBlockProof.add(RlpString.create(relayBlockUpdate.getEncodedHeader()));
            List<RlpType> relayBlockWitness = new ArrayList<RlpType>(2);
                relayBlockWitness.add(RlpString.create(BigInteger.valueOf(108)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                relayBlockWitness.add(new RlpList(witness));
            relayBlockProof.add(new RlpList(relayBlockWitness));

        relayChainData.add(RlpString.create(RlpEncoder.encode(new RlpList(relayBlockProof)))); // rlp encoded of blockProof

        relayChainData.add(new RlpList()); // empty stateProof

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 20: invalid relay state proof
     * Given:
     *    invalid state proof of relay block, hash of proof missmatch with stateRoot in header
     *    state proof hash: "e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1"
     * When:
     *    state proof hash: "e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1"
     *    hash of proof not equal to stateRoot
     * Then:
     *    throw error:
     *       message: "invalid MPT proof"
     *       code: INVALID_MPT
     */
    @Test
    @Order(26)
    public void verifyingScenario20() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        BigInteger setId = new BigInteger("123");
        byte[] relayParentHash = relayUpdatedBlocks.get(6).getHash();
        byte[] relayStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 108
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, setId);
            relayParentHash = relayBlockUpdate.getHash();
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, setId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, setId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, setId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
                List<RlpType> relayProofs = new ArrayList<RlpType>(1);
                // invalid proofs
                relayProofs.add(RlpString.create(HexConverter.hexStringToByteArray("80810480d7349ba81a8606735be62a4df34f5ca785ffff19b39b6cd32886a4da9d706c59545e8d434d6125b40443fe11fd292d13a4100300000080985a2c32d72399aa5d04da503ead1ff1a15007afe9b119dec3aa53528d9948c9")));
            relayStateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))); // storage key
            relayStateProofs.add(new RlpList(relayProofs));
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_MPT + ")"));
    }

    /**
     * 
     * Scenario 21: update new authorities list of relay chain
     * Given:
     *    stateProof with new authorities event of relay chain
     *    event contains list of 4 new validators
     * When:
     *    current validators set ID: 123
     * Then:
     *    update new validators list to db
     *    increase validator setID: 124
     */
    @Test
    @Order(27)
    public void verifyingScenario21() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        byte[] relayParentHash = relayUpdatedBlocks.get(6).getHash();

        List<byte[]> newValidatorPublicKeys = new ArrayList<byte[]>(4); // new list of validators
        List<byte[]> newValidatorSecretKey = new ArrayList<byte[]>(4);
        newValidatorPublicKeys.add(0, HexConverter.hexStringToByteArray("2f27b7a3f5e8a73ac8e905ff237de902723e969bcc424ddbc2b718d948fc82e8"));
        newValidatorPublicKeys.add(1, HexConverter.hexStringToByteArray("0541be4a4cfda7c722a417cd38807fa5ba9500cf3a8f5440576bf8764f3281c6"));
        newValidatorPublicKeys.add(2, HexConverter.hexStringToByteArray("20852d791571334b261838ab84ee38aac4845e81faac2c677e108a3553b65641"));
        newValidatorPublicKeys.add(3, HexConverter.hexStringToByteArray("ef3a8a0271488f2fbdc787f3285aa42c4c073c2d73f9a3ee288a07471512a6e9"));

        newValidatorSecretKey.add(0, HexConverter.hexStringToByteArray("23fae2b43f110c42a1d32b2c8678d7b7f71eb7873edd0feab952efbc99d1f9d02f27b7a3f5e8a73ac8e905ff237de902723e969bcc424ddbc2b718d948fc82e8"));
        newValidatorSecretKey.add(1, HexConverter.hexStringToByteArray("2b502a205ab7ff2b56a0b225fb6b58c3d78a57a8e03c80938b32e7fbc2c564cc0541be4a4cfda7c722a417cd38807fa5ba9500cf3a8f5440576bf8764f3281c6"));
        newValidatorSecretKey.add(2, HexConverter.hexStringToByteArray("27b9c71dfee22bacee68a3c73c4aac3a7c95b72dbe1876095fcc2d4f96ce5bac20852d791571334b261838ab84ee38aac4845e81faac2c677e108a3553b65641"));
        newValidatorSecretKey.add(3, HexConverter.hexStringToByteArray("b68efa447ebc6ec9f31a5d24c42e4cba41ae7bcc3e7a3f64918648d9e2bf9244ef3a8a0271488f2fbdc787f3285aa42c4c073c2d73f9a3ee288a07471512a6e9"));

        NewAuthoritiesProof newAuthoritiesProof = new NewAuthoritiesProof(newValidatorPublicKeys);

        byte[] relayStateRoot = newAuthoritiesProof.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 108
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(newAuthoritiesProof.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(newAuthoritiesProof.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        List<byte[]> storedValidators = bmvScore.validators();
        assertEquals(storedValidators.size(), newValidatorPublicKeys.size());
        assertTrue(bmvScore.setId().equals(BigInteger.valueOf(124)));
        assertArrayEquals(storedValidators.get(0), newValidatorPublicKeys.get(0));
        assertArrayEquals(storedValidators.get(1), newValidatorPublicKeys.get(1));
        assertArrayEquals(storedValidators.get(2), newValidatorPublicKeys.get(2));
        assertArrayEquals(storedValidators.get(3), newValidatorPublicKeys.get(3));

        validatorPublicKeys = newValidatorPublicKeys;
        validatorSecretKey = newValidatorSecretKey;
        currentSetId = bmvScore.setId();
    }

    /**
     * 
     * Scenario 22: ignore authorities list of relay chain that already updated
     * Given:
     *    stateProof with new authorities event of relay chain that already updated
     * When:
     *    current validators set ID: 124
     * Then:
     *    ignore that event
     *    no change to validator setID: 124
     */
    @Test
    @Order(28)
    public void verifyingScenario22() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("92");
        byte[] relayParentHash = relayUpdatedBlocks.get(6).getHash();
        byte[] block106Hash = relayUpdatedBlocks.get(5).getHash();
        byte[] block105Hash = relayUpdatedBlocks.get(4).getHash();
        byte[] block104Hash = relayUpdatedBlocks.get(3).getHash();
        byte[] block103Hash = relayUpdatedBlocks.get(2).getHash();
        byte[] block102Hash = relayUpdatedBlocks.get(1).getHash();
        byte[] block101Hash = relayUpdatedBlocks.get(0).getHash();

        byte[] witness1 = this.getConcatenationHash(block105Hash, block106Hash);
        byte[] witness20 = this.getConcatenationHash(block101Hash, block102Hash);
        byte[] witness21 = this.getConcatenationHash(block103Hash, block104Hash);
        byte[] witness2 = this.getConcatenationHash(witness20, witness21);

        List<byte[]> newValidatorPublicKeys = new ArrayList<byte[]>(4); // new list of validators
        List<byte[]> newValidatorSecretKey = new ArrayList<byte[]>(4);
        newValidatorPublicKeys.add(0, HexConverter.hexStringToByteArray("2f27b7a3f5e8a73ac8e905ff237de902723e969bcc424ddbc2b718d948fc82e8"));
        newValidatorPublicKeys.add(1, HexConverter.hexStringToByteArray("0541be4a4cfda7c722a417cd38807fa5ba9500cf3a8f5440576bf8764f3281c6"));
        newValidatorPublicKeys.add(2, HexConverter.hexStringToByteArray("20852d791571334b261838ab84ee38aac4845e81faac2c677e108a3553b65641"));
        newValidatorPublicKeys.add(3, HexConverter.hexStringToByteArray("ef3a8a0271488f2fbdc787f3285aa42c4c073c2d73f9a3ee288a07471512a6e9"));

        newValidatorSecretKey.add(0, HexConverter.hexStringToByteArray("23fae2b43f110c42a1d32b2c8678d7b7f71eb7873edd0feab952efbc99d1f9d02f27b7a3f5e8a73ac8e905ff237de902723e969bcc424ddbc2b718d948fc82e8"));
        newValidatorSecretKey.add(1, HexConverter.hexStringToByteArray("2b502a205ab7ff2b56a0b225fb6b58c3d78a57a8e03c80938b32e7fbc2c564cc0541be4a4cfda7c722a417cd38807fa5ba9500cf3a8f5440576bf8764f3281c6"));
        newValidatorSecretKey.add(2, HexConverter.hexStringToByteArray("27b9c71dfee22bacee68a3c73c4aac3a7c95b72dbe1876095fcc2d4f96ce5bac20852d791571334b261838ab84ee38aac4845e81faac2c677e108a3553b65641"));
        newValidatorSecretKey.add(3, HexConverter.hexStringToByteArray("b68efa447ebc6ec9f31a5d24c42e4cba41ae7bcc3e7a3f64918648d9e2bf9244ef3a8a0271488f2fbdc787f3285aa42c4c073c2d73f9a3ee288a07471512a6e9"));

        NewAuthoritiesProof newAuthoritiesProof = new NewAuthoritiesProof(newValidatorPublicKeys);

        byte[] relayStateRoot = newAuthoritiesProof.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight();

        List<RlpType> relayChainData = new ArrayList<RlpType>(3);
        relayChainData.add(new RlpList());  // empty block update

            List<RlpType> relayBlockProof = new ArrayList<RlpType>(2);
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayBlockProof.add(RlpString.create(relayBlockUpdate.getEncodedHeader()));
            List<RlpType> relayBlockWitness = new ArrayList<RlpType>(2);
                relayBlockWitness.add(RlpString.create(relayUpdatingBlockNumber));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(relayUpdatedBlocks.get(6).getHash()));
                    witness.add(RlpString.create(witness1));
                    witness.add(RlpString.create(witness2));
                relayBlockWitness.add(new RlpList(witness));
            relayBlockProof.add(new RlpList(relayBlockWitness));

        relayChainData.add(RlpString.create(RlpEncoder.encode(new RlpList(relayBlockProof)))); // rlp encoded of blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(newAuthoritiesProof.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(newAuthoritiesProof.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(RlpEncoder.encode(new RlpList(relayChainData)));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        List<byte[]> storedValidators = bmvScore.validators();
        assertEquals(storedValidators.size(), newValidatorPublicKeys.size());
        assertTrue(bmvScore.setId().equals(BigInteger.valueOf(124)));
        // check that validator list is not changed
        assertArrayEquals(storedValidators.get(0), newValidatorPublicKeys.get(0));
        assertArrayEquals(storedValidators.get(1), newValidatorPublicKeys.get(1));
        assertArrayEquals(storedValidators.get(2), newValidatorPublicKeys.get(2));
        assertArrayEquals(storedValidators.get(3), newValidatorPublicKeys.get(3));

        validatorPublicKeys = newValidatorPublicKeys;
        validatorSecretKey = newValidatorSecretKey;
        assertEquals(bmvScore.setId(), currentSetId); // check that setId is not changed
    }

    /**
     * 
     * Scenario 23: input para block update with invalid parent hash with para mta last block hash
     * Given:
     *    msg:
     *      para block 11:
     *         parentHash: "46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"
     * When:
     *    para mta height: 10
     *    para last block hash: "08557f0ca4d319f559310f5d62b38643d0a0555b04efe3e1589f869d052ff9f2"
     * Then:
     *    throw error:
     *    message: "parent para block hash does not match, parent: 46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55 current: 08557f0ca4d319f559310f5d62b38643d0a0555b04efe3e1589f869d052ff9f2"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(29)
    public void verifyingScenario23() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");

        byte[] paraParentHash = HexConverter.hexStringToByteArray("46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"); // different with lastBlockHash
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 11;

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<3; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);
            paraUpdatingBlockNumber +=1;
            paraParentHash = paraBlockUpdate.getHash();
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 24: input para block update with invalid parent hash in blocks
     * Given:
     *    update block 11, 12, 13
     *    block 12 parent hash: "e85929fd964218f6e876ab93087681ee6865d8ce86407329ea45ddedd90522dd"
     * When:
     *    block 12 parent hash should be: block11.getHash()
     * Then:
     *    throw error:
     *    message: "parent para block hash does not match, parent: e85929fd964218f6e876ab93087681ee6865d8ce86407329ea45ddedd90522dd current: block11 hash"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(30)
    public void verifyingScenario24() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = paraLastBlockHash.clone();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 11;

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<3; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);
            paraUpdatingBlockNumber +=1;
            paraParentHash = HexConverter.hexStringToByteArray("e85929fd964218f6e876ab93087681ee6865d8ce86407329ea45ddedd90522dd"); // should be paraBlockUpdate.getHash()
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 25: input para block with height higher than current updated height
     * Given:
     *    update para block 19, 20, 21
     * When:
     *    last para updated block height of BMV: 10
     * Then:
     *    throw error:
     *    message: "invalid para blockUpdate height: 19; expected: 11"
     *    code: INVALID_BLOCK_UPDATE_HEIGHT_HIGHER 33
     */
    @Test
    @Order(31)
    public void verifyingScenario25() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = paraLastBlockHash.clone();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 19; // should be 11

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<3; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);
            paraUpdatingBlockNumber +=1;
            paraParentHash = paraBlockUpdate.getHash();
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 26: input para block with height lower than current updated height
     * Given:
     *    para update block 7, 8, 9
     * When:
     *    last updated para block height of BMV: 10
     * Then:
     *    throw error:
     *    message: "invalid para blockUpdate height: 7; expected: 11"
     *    code: INVALID_BLOCK_UPDATE_HEIGHT_LOWER 34
     */
    @Test
    @Order(32)
    public void verifyingScenario26() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = paraLastBlockHash.clone();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 7; // should be 11

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<3; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);
            paraUpdatingBlockNumber +=1;
            paraParentHash = paraBlockUpdate.getHash();
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_LOWER + ")"));
    }

    /**
     * 
     * Scenario 27: input para block without relay chain data to prove that block finalized
     * Given:
     *    para update block 11, 12, 13
     *    block update 13 does not have relay chain data
     * When:
     *    last updated para block height of BMV: 10
     * Then:
     *    throw error:
     *    message: "Missing relay chain data"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(33)
    public void verifyingScenario27() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = paraLastBlockHash.clone();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 11;

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<3; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);
            paraUpdatingBlockNumber +=1;
            paraParentHash = paraBlockUpdate.getHash();
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode())); // no relay chain data
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 28: can not find parachain block data in relay chain data
     * Given:
     *    para update block 11, 12, 13
     *    block update 13 contain relay chain data
     *    relay chain data does not contain parachain block data
     * When:
     *    last updated para block height of BMV: 10
     * Then:
     *    throw error:
     *    message: "can not find parachain data in relay chain data"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(34)
    public void verifyingScenario28() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = paraLastBlockHash.clone();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 11;

        byte[] ramdomBlockHeader = HexConverter.hexStringToByteArray("9e1f7a1c522ab43821f2d09e1552bb0666422c4e3c7c70b71637b1fb0d226b5b"); // just ramdom block hash, BMV will ignore because it not belong to current paraChainId
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(ramdomBlockHeader, paraChainId.add(BigInteger.valueOf(1))); // different paraChainId

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 109
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<3; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot, RlpEncoder.encode(new RlpList(relayChainData)));
            paraUpdatingBlockNumber +=1;
            paraParentHash = paraBlockUpdate.getHash();
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 29: block hash of parachain not match with data included to relay chain
     * Given:
     *    para update block 11, 12, 13
     *    block update 13 contain relay chain data
     *    relay chain data included block hash "9e1f7a1c522ab43821f2d09e1552bb0666422c4e3c7c70b71637b1fb0d226b5b"
     * When:
     *    last updated para block height of BMV: 10
     *    block 13 hash not match "9e1f7a1c522ab43821f2d09e1552bb0666422c4e3c7c70b71637b1fb0d226b5b"
     * Then:
     *    throw error:
     *    message: "block hash does not match with relay chain, para block hash: block13.hash()  relay inclusion: 9e1f7a1c522ab43821f2d09e1552bb0666422c4e3c7c70b71637b1fb0d226b5b"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(35)
    public void verifyingScenario29() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = paraLastBlockHash.clone();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 11;

        byte[] includedParaBlockHeader = HexConverter.hexStringToByteArray("9e1f7a1c522ab43821f2d09e1552bb0666422c4e3c7c70b71637b1fb0d226b5c"); // not match with block13.hash()
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId);

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 109
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<3; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot, RlpEncoder.encode(new RlpList(relayChainData)));
            paraUpdatingBlockNumber +=1;
            paraParentHash = paraBlockUpdate.getHash();
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.BMV_ERROR + ")"));
    }

    /**
     * 
     * Scenario 30: update relay and para chain block successfully
     * Given:
     *    para update block 11, 12, 13, 14, 15, 16, 17
     *    block update 17 contains relay chain data
     *    relay chain data included block hash block17.hash()
     *    update relay block 109
     * When:
     *    last updated para block height of BMV: 10
     *    last updated relay block height of BMV: 109
     * Then:
     *    update relay MTA
     *    update para MTA
     */
    @Test
    @Order(36)
    public void verifyingScenario30() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = paraLastBlockHash.clone();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 11;

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i<6; i++) {
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);
            paraUpdatedBlocks.add(paraBlockUpdate);
            paraUpdatingBlockNumber +=1;
            paraParentHash = paraBlockUpdate.getHash();
            paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));
        }

        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);

        byte[] includedParaBlockHeader = paraBlockUpdate.getHash();
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId); // event that include block of parachain

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 109
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        paraParentHash = paraBlockUpdate.getHash();
        paraBlockUpdate.setRelayChainData(RlpEncoder.encode(new RlpList(relayChainData)));
        paraUpdatedBlocks.add(paraBlockUpdate);
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        BigInteger relayMtaHeight = bmvScore.relayMtaHeight();
        assertTrue(relayMtaHeight.equals(relayUpdatingBlockNumber));

        List<byte[]> relayMtaRoot = bmvScore.relayMtaRoot();
        assertEquals(relayMtaRoot.size(), mtaRootSize);
        assertArrayEquals(relayMtaRoot.get(0), relayParentHash);

        byte[] relayMtaLastBlockHash = bmvScore.relayMtaLastBlockHash();
        assertArrayEquals(relayMtaLastBlockHash, relayParentHash);

        BigInteger relayMtaOffsetResult = bmvScore.relayMtaOffset();
        assertTrue(relayMtaOffsetResult.equals(BigInteger.valueOf(relayMtaOffset)));

        List<byte[]> relayMtaCaches = bmvScore.relayMtaCaches();
        assertEquals(relayMtaCaches.size(), 3);
        assertArrayEquals(relayMtaCaches.get(2), relayParentHash);

        BigInteger lastHeightResult = bmvScore.lastHeight();
        assertTrue(lastHeightResult.equals(BigInteger.valueOf(paraMtaOffset)));

        BigInteger paraMtaHeight = bmvScore.paraMtaHeight();
        assertTrue(paraMtaHeight.equals(BigInteger.valueOf(paraUpdatingBlockNumber)));

        List<byte[]> paraMtaRoot = bmvScore.paraMtaRoot();
        assertEquals(paraMtaRoot.size(), mtaRootSize);
        assertArrayEquals(paraMtaRoot.get(0), paraParentHash);

        byte[] paraMtaLastBlockHash = bmvScore.paraMtaLastBlockHash();
        assertArrayEquals(paraMtaLastBlockHash, paraParentHash);

        BigInteger paraMtaOffsetResult = bmvScore.paraMtaOffset();
        assertTrue(paraMtaOffsetResult.equals(BigInteger.valueOf(paraMtaOffset)));

        List<byte[]> paraMtaCaches = bmvScore.paraMtaCaches();
        assertEquals(paraMtaCaches.size(), 3);
        assertArrayEquals(paraMtaCaches.get(2), paraParentHash);
    }

    /**
     * 
     * Scenario 31: update para blockProof of block that not exist in MTA
     * Given:
     *    para blockProof of block 18;
     * When:
     *    current para mta height: 17;
     * Then:
     *    throw error:
     *       message: "given block height is newer 18; expected: 17"
     *       code: INVALID_BLOCK_PROOF_HEIGHT_HIGHER, 35
     */
    @Test
    @Order(37)
    public void verifyingScenario31() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        byte[] paraParentHash = paraUpdatedBlocks.get(6).getHash(); // block 17 hash
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger paraMtaHeight = bmvScore.paraMtaHeight();
        long paraUpdatingBlockNumber = 18;

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> paraBlockProof = new ArrayList<RlpType>(2);
            ParaBlockUpdate blockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot); // new block that not exist in MTA
            paraBlockProof.add(RlpString.create(blockUpdate.getEncodedHeader())); // block header
                List<RlpType> paraBlockWitness = new ArrayList<RlpType>(2); // empty witness
                paraBlockWitness.add(RlpString.create(paraUpdatingBlockNumber));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                paraBlockWitness.add(new RlpList(witness));
            paraBlockProof.add(new RlpList(paraBlockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(paraBlockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_PROOF_HEIGHT_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 32: update para blockProof with invalid old witness
     * Given:
     *    para blockProof of block 14;
     *    para mta height of client: 15;
     * When:
     *    current para mta height of BMV: 17;
     *    para mta cache size: 3 (stored block hash 17, 16, 15)
     * Then:
     *    throw error:
     *       message: "not allowed old witness"
     *       code: INVALID_BLOCK_WITNESS_OLD, 36
     */
    @Test
    @Order(38)
    public void verifyingScenario32() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> paraBlockProof = new ArrayList<RlpType>(2);
            paraBlockProof.add(RlpString.create(paraUpdatedBlocks.get(3).getEncodedHeader())); // block 14
                List<RlpType> paraBlockWitness = new ArrayList<RlpType>(2); // empty witness
                paraBlockWitness.add(RlpString.create(BigInteger.valueOf(15)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(paraUpdatedBlocks.get(5).getHash()));
                paraBlockWitness.add(new RlpList(witness));
            paraBlockProof.add(new RlpList(paraBlockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(paraBlockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS_OLD + ")"));
    }

    /**
     * 
     * Scenario 33: update para blockProof with invalid block header when client MTA height less than BMV MTA height
     * Given:
     *    para blockProof of block 15 with incorrect data;
     *    para mta height of client: 16;
     * When:
     *    current para mta height of BMV: 17;
     *    para mta cache size: 3 (stored block hash 17, 16, 15)
     * Then:
     *    throw error:
     *       message: "invalid old witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(39)
    public void verifyingScenario33() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        byte[] paraParentHash = HexConverter.hexStringToByteArray("4eb5e84732c04e792a48a9c4d3b637039cff2299d8a9ebfaebfa54b437ea697c");
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 15;

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> paraBlockProof = new ArrayList<RlpType>(2);
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot); // fake block 15 updated to block proof
            paraBlockProof.add(RlpString.create(paraBlockUpdate.getEncodedHeader())); // block header
                List<RlpType> paraBlockWitness = new ArrayList<RlpType>(2); // empty witness
                paraBlockWitness.add(RlpString.create(BigInteger.valueOf(16)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                paraBlockWitness.add(new RlpList(witness));
            paraBlockProof.add(new RlpList(paraBlockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(paraBlockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 34: update para blockProof with invalid block witness
     * Given:
     *    para blockProof of block 15 that has invalid witness
     *    para mta height of client: 17;
     * When:
     *    para current mta height of BMV: 17;
     * Then:
     *    throw error:
     *       message: "invalid witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(40)
    public void verifyingScenario34() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> paraBlockProof = new ArrayList<RlpType>(2);
            paraBlockProof.add(RlpString.create(paraUpdatedBlocks.get(4).getEncodedHeader())); // block 15, correct data
                List<RlpType> paraBlockWitness = new ArrayList<RlpType>(2); // empty witness
                paraBlockWitness.add(RlpString.create(BigInteger.valueOf(17)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                paraBlockWitness.add(new RlpList(witness));
            paraBlockProof.add(new RlpList(paraBlockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(paraBlockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 35: update para blockProof with invalid block witness when client MTA height greater than bmv MTA height
     * Given:
     *    para blockProof of block 17 that has invalid witness
     *    para mta height of client: 18;
     * When:
     *    para current mta height of BMV: 17;
     * Then:
     *    throw error:
     *       message: "invalid witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(41)
    public void verifyingScenario35() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        byte[] paraParentHash = HexConverter.hexStringToByteArray("4eb5e84732c04e792a48a9c4d3b637039cff2299d8a9ebfaebfa54b437ea697c");
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = 17;

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> paraBlockProof = new ArrayList<RlpType>(2);
            ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot); // fake block 15 updated to block proof
            paraBlockProof.add(RlpString.create(paraBlockUpdate.getEncodedHeader())); // block 17, incorrect data
            List<RlpType> paraBlockWitness = new ArrayList<RlpType>(2); // empty witness
                paraBlockWitness.add(RlpString.create(bmvScore.paraMtaHeight().add(BigInteger.valueOf(1))));
                List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                paraBlockWitness.add(new RlpList(witness));
            paraBlockProof.add(new RlpList(paraBlockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(paraBlockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 36: invalid para state proof
     * Given:
     *    invalid para state proof, hash of proof missmatch with stateRoot in header
     *    state proof hash: "",
     *    state root in header: "e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1"
     * When:
     *
     * Then:
     *    throw error:
     *       message: "invalid MPT proof"
     *       code: INVALID_MPT
     */
    @Test
    @Order(42)
    public void verifyingScenario36() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = bmvScore.paraMtaLastBlockHash();
        byte[] paraStateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long paraUpdatingBlockNumber = bmvScore.paraMtaHeight().longValue() + 1;

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);

        byte[] includedParaBlockHeader = paraBlockUpdate.getHash();
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId);

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 110
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            // validators sign block to be finalized
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        paraBlockUpdate.setRelayChainData(RlpEncoder.encode(new RlpList(relayChainData)));
        paraUpdatedBlocks.add(paraBlockUpdate);
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // empty block proof of para chain

            List<RlpType> stateProofs = new ArrayList<RlpType>(2);
                List<RlpType> proofs = new ArrayList<RlpType>(1);
                // invalid para state proof
                proofs.add(RlpString.create(HexConverter.hexStringToByteArray("80810480d7349ba81a8606735be62a4df34f5ca785ffff19b39b6cd32886a4da9d706c59545e8d434d6125b40443fe11fd292d13a4100300000080985a2c32d72399aa5d04da503ead1ff1a15007afe9b119dec3aa53528d9948c9")));
            stateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))); // storage key
            stateProofs.add(new RlpList(proofs));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(stateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_MPT + ")"));
    }

    /**
     * ---------------------------------------------------------------------------------------------
     *                            RESPOND THE MESSAGE FROM BMC
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * 
     * Scenario 1: btp message sequence higher than expected
     * Given:
     *    btp message sequence: 120
     *
     * When:
     *    bmc message sequence: 111
     * Then:
     *    throw error:
     *      message: "invalid sequence: 120; expected 112"
     *      code: INVALID_SEQUENCE_HIGHER, 32
     */
    @Test
    @Order(43)
    public void respondScenario1() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = bmvScore.paraMtaLastBlockHash();
        long paraUpdatingBlockNumber = bmvScore.paraMtaHeight().longValue() + 1;

        String btpNextAddress = bmcBTPAddress;
        BigInteger invalidMessageSequence = BigInteger.valueOf(120); // should be 112
        byte[] btpMessage = "testencodedmessage".getBytes();
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, invalidMessageSequence, btpMessage);
        byte[] paraStateRoot = btpEvent.getRoot();

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);

        byte[] includedParaBlockHeader = paraBlockUpdate.getHash();
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId);

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 110
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            // validators sign block to be finalized
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        paraBlockUpdate.setRelayChainData(RlpEncoder.encode(new RlpList(relayChainData)));
        paraUpdatedBlocks.add(paraBlockUpdate);
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // empty block proof of para chain

            List<RlpType> paraStateProofs = new ArrayList<RlpType>(2);
            paraStateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))); // storage key
            paraStateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(paraStateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_SEQUENCE_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 2: btp message sequence lower than expected
     * Given:
     *    btp message sequence: 105
     *
     * When:
     *    bmc message sequence: 111
     * Then:
     *    throw error:
     *      message: "invalid sequence: 105; expected 112"
     *      code: INVALID_SEQUENCE, 28
     */
    @Test
    @Order(44)
    public void respondScenario2() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = bmvScore.paraMtaLastBlockHash();
        long paraUpdatingBlockNumber = bmvScore.paraMtaHeight().longValue() + 1;

        String btpNextAddress = bmcBTPAddress;
        BigInteger invalidMessageSequence = BigInteger.valueOf(105); // should be 112
        byte[] btpMessage = "testencodedmessage".getBytes();
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, invalidMessageSequence, btpMessage);
        byte[] paraStateRoot = btpEvent.getRoot();

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);

        byte[] includedParaBlockHeader = paraBlockUpdate.getHash();
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId);

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 110
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            // validators sign block to be finalized
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        paraBlockUpdate.setRelayChainData(RlpEncoder.encode(new RlpList(relayChainData)));
        paraUpdatedBlocks.add(paraBlockUpdate);
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // empty block proof of para chain

            List<RlpType> paraStateProofs = new ArrayList<RlpType>(2);
            paraStateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))); // storage key
            paraStateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(paraStateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_SEQUENCE + ")"));
    }

    /**
     * 
     * Scenario 3: update btp message with block update successfully
     * Given:
     *    update block: 18
     *    valid relay chain data with enough signatures
     *    valid state proof of relay chain and para chain
     *    btp message sequence: 112
     *    btp message contain in block 18
     * When:
     *    bmc message sequence: 111
     *    current block height: 17
     * Then:
     *    return btp message to BMC
     */
    @Test
    @Order(45)
    public void respondScenario3() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = bmvScore.paraMtaLastBlockHash();
        long paraUpdatingBlockNumber = bmvScore.paraMtaHeight().longValue() + 1;

        String btpNextAddress = bmcBTPAddress;
        BigInteger nextMessageSequence = BigInteger.valueOf(112); // current message seq is 111
        byte[] btpMessage = "testencodedmessage".getBytes(); // don't need to create about this, only ramdom data for test, BMV with return this to BMC
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, nextMessageSequence, btpMessage);
        byte[] paraStateRoot = btpEvent.getRoot();

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);

        byte[] includedParaBlockHeader = paraBlockUpdate.getHash();
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId);

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 110
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            // validators sign block to be finalized
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        paraBlockUpdate.setRelayChainData(RlpEncoder.encode(new RlpList(relayChainData)));
        paraUpdatedBlocks.add(paraBlockUpdate);
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // empty block proof of para chain

            List<RlpType> paraStateProofs = new ArrayList<RlpType>(2);
            paraStateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))); // storage key
            paraStateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(paraStateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        assertTrue(bmvScore.paraMtaHeight().equals(BigInteger.valueOf(paraUpdatingBlockNumber)));
        assertTrue(bmvScore.relayMtaHeight().equals(relayUpdatingBlockNumber));

        assertTrue(bmvScore.lastHeight().equals(BigInteger.valueOf(paraUpdatingBlockNumber)));
    }

    /**
     * 
     * Scenario 4: contract log event not BMC contract of src chain
     * Given:
     *    update block: 19
     *    valid relay chain data with enough signatures
     *    valid state proof of relay chain and para chain
     *    btp message sequence: 113
     *    btp message contain in block 19
     *    event message extract from RelayMessage of contract cx1234567
     * When:
     *    BTP address of _prev: btp://0x9876.edge/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
     * Then:
     *    ignore that event log
     */
    @Test
    @Order(46)
    public void respondScenario4() throws Exception {
        String notHandlePrevBmcAddress = "ea1c4c2767b6ef9618f6338e700d2570b23f1231"; // bmc address of src chain that not match with prev input of BMC
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = bmvScore.paraMtaLastBlockHash();
        long paraUpdatingBlockNumber = bmvScore.paraMtaHeight().longValue() + 1;
        BigInteger lastHeightBeforeUpdated = bmvScore.lastHeight(); // last height of para chain has btp message before updated

        String btpNextAddress = bmcBTPAddress;
        BigInteger nextMessageSequence = BigInteger.valueOf(112); // current message seq is 111
        byte[] btpMessage = "testencodedmessage".getBytes(); // don't need to create about this, only ramdom data for test, BMV with return this to BMC
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(notHandlePrevBmcAddress), btpNextAddress, nextMessageSequence, btpMessage);
        byte[] paraStateRoot = btpEvent.getRoot();

        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(1);
        ParaBlockUpdate paraBlockUpdate = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);

        byte[] includedParaBlockHeader = paraBlockUpdate.getHash();
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId);

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 110
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            // validators sign block to be finalized
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        paraBlockUpdate.setRelayChainData(RlpEncoder.encode(new RlpList(relayChainData)));
        paraUpdatedBlocks.add(paraBlockUpdate);
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));
        relayMessage.add(RlpString.create("")); // empty block proof of para chain

            List<RlpType> paraStateProofs = new ArrayList<RlpType>(2);
            paraStateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))); // storage key
            paraStateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(paraStateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        assertTrue(bmvScore.paraMtaHeight().equals(BigInteger.valueOf(paraUpdatingBlockNumber)));
        assertTrue(bmvScore.relayMtaHeight().equals(relayUpdatingBlockNumber));

        assertTrue(bmvScore.lastHeight().equals(lastHeightBeforeUpdated)); // last height should not be change, because no btp message was updated
    }

    /**
     * 
     * Scenario 5: update btp message with block proof successfully
     * Given:
     *    update block: 20, 21
     *    btp message sequence: 114
     *    btp message contain in block 120
     *    block proof for block 20
     *    para chain data update block 111
     *    candidate included event in relay chain prove that block 20 included
     * When:
     *    current bmc message sequence: 113
     *    current block height of para: 19
     *    current block height of relay: 110
     * Then:
     *    return btp message to BMC
     */
    @Test
    @Order(47)
    public void respondScenario5() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("112");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] paraParentHash = bmvScore.paraMtaLastBlockHash();
        long paraUpdatingBlockNumber = bmvScore.paraMtaHeight().longValue() + 1;

        String btpNextAddress = bmcBTPAddress;
        BigInteger nextMessageSequence = BigInteger.valueOf(113); // current message seq is 112
        byte[] btpMessage = "testencodedmessageblock21".getBytes(); // don't need to create about this, only ramdom data for test, BMV with return this to BMC
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, nextMessageSequence, btpMessage);
        byte[] paraStateRoot = btpEvent.getRoot();

        // update block 19 and 20 of para chain
        ParaBlockUpdate paraBlockUpdate19 = new ParaBlockUpdate(paraParentHash, paraUpdatingBlockNumber, paraStateRoot);
        paraUpdatingBlockNumber += 1;
        ParaBlockUpdate paraBlockUpdate20 = new ParaBlockUpdate(paraBlockUpdate19.getHash(), paraUpdatingBlockNumber, HexConverter.hexStringToByteArray("ce639555326f1a51548e7aa18db08e3dc0584b05d81285c6776453b78345e745"));

        // relay chain included block 20 of para chain
        byte[] includedParaBlockHeader = paraBlockUpdate20.getHash();
        CandidateIncludedEvent candidateIncludedEvent = new CandidateIncludedEvent(includedParaBlockHeader, paraChainId);

        byte[] relayParentHash = bmvScore.relayMtaLastBlockHash();
        byte[] relayStateRoot = candidateIncludedEvent.getRoot();
        BigInteger relayUpdatingBlockNumber = bmvScore.relayMtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayChainData = new ArrayList<RlpType>(1);
            List<RlpType> relayBlockUpdates = new ArrayList<RlpType>(1);
            // new block 110
            RelayBlockUpdate relayBlockUpdate = new RelayBlockUpdate(relayParentHash, relayUpdatingBlockNumber.longValue(), relayStateRoot, round, currentSetId);
            relayParentHash = relayBlockUpdate.getHash();
            relayUpdatedBlocks.add(relayBlockUpdate);
            // validators sign block to be finalized
            byte[] voteMessage = RelayBlockUpdate.voteMessage(relayBlockUpdate.getHash(), relayUpdatingBlockNumber.longValue(), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
            relayBlockUpdate.vote(validatorPublicKeys.get(2), validatorSecretKey.get(2), round, currentSetId);
            relayBlockUpdates.add(RlpString.create(relayBlockUpdate.encodeWithVoteMessage(voteMessage)));
        relayChainData.add(new RlpList(relayBlockUpdates));

        relayChainData.add(RlpString.create("")); // empty blockProof

            List<RlpType> relayStateProofs = new ArrayList<RlpType>(2);
            relayStateProofs.add(RlpString.create(candidateIncludedEvent.getEventKey())); // storage key
            relayStateProofs.add(new RlpList(candidateIncludedEvent.getProof())); // MPT proof of event storage
        relayChainData.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(relayStateProofs)))));

        paraBlockUpdate20.setRelayChainData(RlpEncoder.encode(new RlpList(relayChainData)));
        paraUpdatedBlocks.add(paraBlockUpdate19);
        paraUpdatedBlocks.add(paraBlockUpdate20);
        List<RlpType> paraBlockUpdates = new ArrayList<RlpType>(2);
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate19.encode()));
        paraBlockUpdates.add(RlpString.create(paraBlockUpdate20.encode()));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(paraBlockUpdates));

            List<RlpType> paraBlockProof = new ArrayList<RlpType>(2);
            paraBlockProof.add(RlpString.create(paraBlockUpdate19.getEncodedHeader())); // block 19

            List<RlpType> paraBlockWitness = new ArrayList<RlpType>(2); // empty witness
                paraBlockWitness.add(RlpString.create(BigInteger.valueOf(20)));
                List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                paraBlockWitness.add(new RlpList(witness));
                witness.add(RlpString.create(paraBlockUpdate20.getHash())); // block 20 hash
            paraBlockProof.add(new RlpList(paraBlockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(paraBlockProof)))); // rlp encoded of blockProof

            List<RlpType> paraStateProofs = new ArrayList<RlpType>(2);
            paraStateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7"))); // storage key
            paraStateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(paraStateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        assertTrue(bmvScore.paraMtaHeight().equals(BigInteger.valueOf(paraUpdatingBlockNumber)));
        assertTrue(bmvScore.relayMtaHeight().equals(relayUpdatingBlockNumber));
    }
}
