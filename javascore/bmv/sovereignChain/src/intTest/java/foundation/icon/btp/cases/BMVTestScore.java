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
import foundation.icon.test.score.BMVScore;
import foundation.icon.test.score.EventDecoderScore;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.SecureRandom;
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
    private static SecureRandom secureRandom;
    private static KeyWallet[] wallets;
    private static KeyWallet ownerWallet, caller;
    private static List<byte[]> validatorPublicKeys;
    private static List<byte[]> validatorSecretKey;
    private static BMVScore bmvScore;
    private static EventDecoderScore eventDecoderScore;
    private static String bmc;
    private static String destinationNet = "0x9876.edge";
    private static String sourceNet = "0x1234.icon";
    private static boolean mtaIsAllowNewerWitness = true;
    private static byte[] lastBlockHash = HexConverter.hexStringToByteArray("08557f0ca4d319f559310f5d62b38643d0a0555b04efe3e1589f869d052ff9f2");
    private static String bmcBTPAddress;
    private static BigInteger currentSetId = BigInteger.valueOf(123);
    private static long mtaOffset = 10;
    private static int mtaRootSize = 10;
    private static int mtaCacheSize = 3;
    private static List<BlockUpdate> updatedBlocks = new ArrayList<BlockUpdate>(10);

    @BeforeAll
    static void setup() throws Exception {
        ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");
        validatorPublicKeys = new ArrayList<byte[]>(5);
        validatorSecretKey = new ArrayList<byte[]>(5);

        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        IconService iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL(), 3));
        txHandler = new TransactionHandler(iconService, chain);
        secureRandom = new SecureRandom();

        // init wallets
        wallets = new KeyWallet[2];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(100));
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
        eventDecoderScore = EventDecoderScore.mustDeploy(
            txHandler,
            ownerWallet
        );
        bmvScore = BMVScore.mustDeploy(
            txHandler,
            ownerWallet,
            bmc,
            destinationNet,
            encodedValidators,
            mtaOffset,
            mtaRootSize,
            mtaCacheSize,
            mtaIsAllowNewerWitness,
            lastBlockHash,
            eventDecoderScore.getAddress(),
            currentSetId
        );

        // check contract initialized successfully
        BigInteger mtaHeight = bmvScore.mtaHeight();
        assertTrue(mtaHeight.equals(BigInteger.valueOf(mtaOffset)));

        List<byte[]> mtaRoot = bmvScore.mtaRoot();
        assertEquals(mtaRoot.size(), mtaRootSize);

        byte[] mtaLastBlockHash = bmvScore.mtaLastBlockHash();
        assertArrayEquals(mtaLastBlockHash, lastBlockHash);

        BigInteger mtaOffsetResult = bmvScore.mtaOffset();
        assertTrue(mtaOffsetResult.equals(BigInteger.valueOf(mtaOffset)));

        List<byte[]> mtaCaches = bmvScore.mtaCaches();
        assertEquals(mtaCaches.size(), 0);

        Address bmcAddress = bmvScore.bmc();
        assertTrue(bmcAddress.toString().equals(bmc));

        Address eventDecoderAddress = bmvScore.eventDecoder();
        assertTrue(eventDecoderAddress.equals(eventDecoderScore.getAddress()));

        String netAddressResult = bmvScore.netAddress();
        assertTrue(netAddressResult.equals(destinationNet));

        BigInteger lastHeightResult = bmvScore.mtaOffset();
        assertTrue(lastHeightResult.equals(BigInteger.valueOf(mtaOffset)));

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
     * Scenario 1: input block update with invalid parent hash with mta last block hash
     * Given:
     *    msg:
     *      block 11:
     *         parentHash: "46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"
     * When:
     *    mta height: 10
     *    last block hash: "08557f0ca4d319f559310f5d62b38643d0a0555b04efe3e1589f869d052ff9f2"
     * Then:
     *    throw error:
     *    message: "parent block hash does not match, parent: 46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55 current: 08557f0ca4d319f559310f5d62b38643d0a0555b04efe3e1589f869d052ff9f2"
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

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            updatingBlockNumber += 1;
            parentHash = blockUpdate.getHash();
            blockUpdates.add(RlpString.create(blockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 2: input block update with invalid parent hash in blocks
     * Given:
     *    update block 11, 12, 13
     *    block 12 parent hash: "46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"
     * When:
     *    block 12 parent hash should be: block11.getHash()
     * Then:
     *    throw error:
     *    message: "parent block hash does not match, parent: 46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55 current: block11 hash"
     *    code: BMV_ERROR 25
     */
    @Test
    @Order(9)
    public void verifyingScenario2() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            updatingBlockNumber += 1;
            parentHash = HexConverter.hexStringToByteArray("46212af3be91c9eb81e0864c0158332428d4df405980a5d3042c1ba934cbaa55"); // correct is blockUpdate.getHash()
            blockUpdates.add(RlpString.create(blockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 3: input block with height higher than current updated height
     * Given:
     *    update block 19, 20, 21
     * When:
     *    last updated block height of BMV: 10
     * Then:
     *    throw error:
     *    message: "invalid blockUpdate height: 19; expected: 11"
     *    code: INVALID_BLOCK_UPDATE_HEIGHT_HIGHER 33
     */
    @Test
    @Order(10)
    public void verifyingScenario3() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 19;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            updatingBlockNumber += 1;
            parentHash = blockUpdate.getHash();
            blockUpdates.add(RlpString.create(blockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_UPDATE_HEIGHT_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 4: input block with height lower than current updated height
     * Given:
     *    update block 7, 8, 9
     * When:
     *    last updated block height of BMV: 10
     * Then:
     *    throw error:
     *    message: "invalid blockUpdate height: 7; expected: 11"
     *    code: INVALID_BLOCK_UPDATE_HEIGHT_LOWER 34
     */
    @Test
    @Order(11)
    public void verifyingScenario4() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 7;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            updatingBlockNumber += 1;
            parentHash = blockUpdate.getHash();
            blockUpdates.add(RlpString.create(blockUpdate.encode()));
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 5: update block without votes of validators
     * Given:
     *    update block 11, 12, 13
     *    block 13 has empty votes
     * When:
     *
     * Then:
     *    throw error:
     *    message: "not exists votes"
     *    code: INVALID_BLOCK_UPDATE 29
     */
    @Test
    @Order(12)
    public void verifyingScenario5() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            updatingBlockNumber += 1;
            parentHash = blockUpdate.getHash();
            blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 6: update block with invalid vote, vote for invalid block hash
     * Given:
     *    update block 11, 12, 13
     *    validator sign for:
     *       block hash: "a5f2868483655605709dcbda6ce0fd21cd0386ae2f99445e052d6b1f1ae6db5b"
     * When:
     *    validator should be sign for:
     *       block hash:  block13.getHash()
     * Then:
     *    throw error:
     *    message: "validator signature invalid block hash"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(13)
    public void verifyingScenario6() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = bmvScore.mtaHeight().add(BigInteger.valueOf(1)).longValue();

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            updatingBlockNumber += 1;
            parentHash = blockUpdate.getHash();
            if (i == 2) {
                byte[] invalidVoteBlockHash = HexConverter.hexStringToByteArray("a5f2868483655605709dcbda6ce0fd21cd0386ae2f99445e052d6b1f1ae6db5b");
                byte[] voteMessage = BlockUpdate.voteMessage(invalidVoteBlockHash, updatingBlockNumber, round, setId); // should not be invalidVoteBlockHash but blockUpdate.getHash()
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage))); // update block without votes
            } else {
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 7: update block with invalid vote, vote for invalid block height
     * Given:
     *    update block 11, 12, 13
     *    validator sign for:
     *       block height 12
     * When:
     *    validator should be sign for:
     *       block height: 13
     * Then:
     *    throw error:
     *    message: "validator signature invalid block height"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(14)
    public void verifyingScenario7() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("92");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            updatingBlockNumber += 1;
            parentHash = blockUpdate.getHash();
            if (i == 2) {
                long invalidVoteBlockHeight = 12;
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), invalidVoteBlockHeight, round, setId); // should not be invalidVoteBlockHeight but updatingBlockNumber
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));
            } else {
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 8: update block with invalid vote, vote of newer validator set id
     * Given:
     *    update block 11, 12, 13
     *    vote of validator set Id: 124
     * When:
     *    current validator set id stored in BMV: 123
     * Then:
     *    throw error:
     *    message: "verify signature for invalid validator set id""
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(15)
    public void verifyingScenario8() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        BigInteger setId = new BigInteger("124"); // invalid setId
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, setId);
            if (i == 2) {
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber, round, setId); // should not be updatingBlockNumber + 1 but updatingBlockNumber
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, setId);
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage))); // update block without votes
            } else {
                updatingBlockNumber += 1;
                parentHash = blockUpdate.getHash();
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 9: update block with invalid vote, signature invalid
     * Given:
     *    update block 11, 12, 13
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
    public void verifyingScenario9() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId);
            if (i == 2) {
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber, round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round.add(BigInteger.valueOf(1)), currentSetId); // validator 1 sign with incorrect message
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage))); // update block without votes
            } else {
                updatingBlockNumber += 1;
                parentHash = blockUpdate.getHash();
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 10: update block with invalid vote, signature not belong to validators
     * Given:
     *    update block 11, 12, 13
     *    block 13 is signed by key not belong to validators
     * When:
     *
     * Then:
     *    throw error:
     *    message: "one of signature is not belong to validator"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(17)
    public void verifyingScenario10() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        byte[] notValidatorPubicKey = HexConverter.hexStringToByteArray("793d5108b8d128792958a139660102d538c24e9fe70bd396e5c95fe451cdc222");
        byte[] notValidatorSecrectKey = HexConverter.hexStringToByteArray("b9fbcdfe9e6289fadef00225651dc6a4ae3fee160d6f86cce71150b3db691481793d5108b8d128792958a139660102d538c24e9fe70bd396e5c95fe451cdc222");

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId);
            if (i == 2) {
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber, round, currentSetId);
                blockUpdate.vote(notValidatorPubicKey, notValidatorSecrectKey, round, currentSetId); // sign key not belong to validator
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));
            } else {
                updatingBlockNumber += 1;
                parentHash = blockUpdate.getHash();
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 11: update block with duplicate vote
     * Given:
     *    update block 11, 12, 13
     *    two votes for block 13 are the same
     * When:
     *
     * Then:
     *    throw error:
     *    message: "duplicated signature"
     *    code: INVALID_VOTES 27
     */
    @Test
    @Order(18)
    public void verifyingScenario11() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId);
            if (i == 2) {
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber, round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // two same signatures
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // two same signatures
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage))); // update block without votes
            } else {
                updatingBlockNumber += 1;
                parentHash = blockUpdate.getHash();
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
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
     * Scenario 12: block vote not enough 2/3
     * Given:
     *    update block 11, 12, 13
     *    number of vote for block 13: 2
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
    public void verifyingScenario12() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 3; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId);
            if (i == 2) {
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber, round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // just two vote, must be 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage))); // update block without votes
            } else {
                updatingBlockNumber += 1;
                parentHash = blockUpdate.getHash();
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_VOTES + ")"));
    }

    /**
     * 
     * Scenario 13: update block successfully
     * Given:
     *    update block 11, 12, 13, 14, 15, 16, 17
     *    number of vote for block 17: 3
     * When:
     *    number of validators: 4
     *    current updated block height: 10
     * Then:
     *    update block to MTA caches
     *    update root of mta
     *    update MTA last block hash
     */
    @Test
    @Order(20)
    public void verifyingScenario13() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        long updatingBlockNumber = 11;

        List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
        for (int i = 0; i < 7; i++) {
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId);
            updatedBlocks.add(blockUpdate);
            parentHash = blockUpdate.getHash();
            if (i == 6) {
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber, round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // 3 votes, enough 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(3), validatorSecretKey.get(3), round, currentSetId);
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));
            } else {
                updatingBlockNumber += 1;
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithoutVote())); // update block without votes
            }
        }

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // block proof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        BigInteger mtaHeight = bmvScore.mtaHeight();
        assertTrue(mtaHeight.equals(BigInteger.valueOf(updatingBlockNumber)));

        List<byte[]> mtaRoot = bmvScore.mtaRoot();
        assertEquals(mtaRoot.size(), mtaRootSize);
        assertArrayEquals(mtaRoot.get(0), parentHash);

        byte[] mtaLastBlockHash = bmvScore.mtaLastBlockHash();
        assertArrayEquals(mtaLastBlockHash, parentHash);

        BigInteger mtaOffsetResult = bmvScore.mtaOffset();
        assertTrue(mtaOffsetResult.equals(BigInteger.valueOf(mtaOffset)));

        List<byte[]> mtaCaches = bmvScore.mtaCaches();
        assertEquals(mtaCaches.size(), 3);
        assertArrayEquals(mtaCaches.get(2), parentHash);

        BigInteger lastHeightResult = bmvScore.mtaOffset();
        assertTrue(lastHeightResult.equals(BigInteger.valueOf(mtaOffset)));
    }

    /**
     * 
     * Scenario 14: update blockProof of block that not exist in MTA
     * Given:
     *    blockProof of block 18;
     * When:
     *    current mta height: 17;
     * Then:
     *    throw error:
     *       message: "given block height is newer 18; expected: 17"
     *       code: INVALID_BLOCK_PROOF_HEIGHT_HIGHER, 35
     */
    @Test
    @Order(21)
    public void verifyingScenario14() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger mtaHeight = bmvScore.mtaHeight();
        long updatingBlockNumber = 18;

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId); // new block that not exist in MTA
            blockProof.add(RlpString.create(blockUpdate.getEncodedHeader())); // block header

                List<RlpType> blockWitness = new ArrayList<RlpType>(2);
                blockWitness.add(RlpString.create(mtaHeight));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                blockWitness.add(new RlpList(witness));
            blockProof.add(new RlpList(blockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_PROOF_HEIGHT_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 15: update blockProof with invalid old witness
     * Given:
     *    blockProof of block 14;
     *    mta height of client: 15;
     * When:
     *    current mta height of BMV: 17;
     *    mta cache size: 3 (stored block hash 17, 16, 15)
     * Then:
     *    throw error:
     *       message: "not allowed old witness"
     *       code: INVALID_BLOCK_WITNESS_OLD, 36
     */
    @Test
    @Order(22)
    public void verifyingScenario15() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            blockProof.add(RlpString.create(updatedBlocks.get(3).getEncodedHeader())); // block 14

                List<RlpType> blockWitness = new ArrayList<RlpType>(2);
                blockWitness.add(RlpString.create(BigInteger.valueOf(15)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(updatedBlocks.get(5).getHash()));
                blockWitness.add(new RlpList(witness));
            blockProof.add(new RlpList(blockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS_OLD + ")"));
    }

    /**
     * 
     * Scenario 16: update blockProof with invalid block header
     * Given:
     *    blockProof of block 15 with incorrect data;
     *    mta height of client: 16;
     * When:
     *    current mta height of BMV: 17;
     *    mta cache size: 3 (stored block hash 17, 16, 15)
     * Then:
     *    throw error:
     *       message: "invalid old witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(23)
    public void verifyingScenario16() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger mtaHeight = bmvScore.mtaHeight();
        long updatingBlockNumber = 15;

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId); // fake block 15 updated to block proof
            blockProof.add(RlpString.create(blockUpdate.getEncodedHeader()));

                List<RlpType> blockWitness = new ArrayList<RlpType>(2);
                blockWitness.add(RlpString.create(BigInteger.valueOf(16)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(updatedBlocks.get(5).getHash()));
                blockWitness.add(new RlpList(witness));
            blockProof.add(new RlpList(blockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 17: update blockProof with invalid block witness
     * Given:
     *    blockProof of block 15 that has invalid witness
     *    mta height of client: 17;
     * When:
     *    current mta height of BMV: 17;
     * Then:
     *    throw error:
     *       message: "invalid witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(24)
    public void verifyingScenario17() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger mtaHeight = bmvScore.mtaHeight();
        BigInteger updatingBlockNumber = BigInteger.valueOf(15);

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId); // fake block 15 updated to block proof
            blockProof.add(RlpString.create(blockUpdate.getEncodedHeader()));

                List<RlpType> blockWitness = new ArrayList<RlpType>(2);
                blockWitness.add(RlpString.create(BigInteger.valueOf(17)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(HexConverter.hexStringToByteArray("9af0b4af1fc9a9d75dbb7e2b72e6560ae2afef30f91625b562adb9659931cc9a"))); // fake witness
                    witness.add(RlpString.create(HexConverter.hexStringToByteArray("679adaf23ccf7fbc51b3c59588357f0c2ec3cacba0595274c8ec5c44354ab8bc")));
                blockWitness.add(new RlpList(witness));
            blockProof.add(new RlpList(blockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 18: update blockProof with invalid block witness when client MTA height greater than bmv MTA height
     * Given:
     *    blockProof of block 17 that has invalid witness
     *    mta height of client: 18;
     * When:
     *    current mta height of BMV: 17;
     * Then:
     *    throw error:
     *       message: "invalid witness"
     *       code: INVALID_BLOCK_WITNESS, 31
     */
    @Test
    @Order(25)
    public void verifyingScenario18() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger updatingBlockNumber = BigInteger.valueOf(17);

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId); // fake block updated to block proof
            blockProof.add(RlpString.create(blockUpdate.getEncodedHeader()));

            List<RlpType> blockWitness = new ArrayList<RlpType>(2);
                blockWitness.add(RlpString.create(BigInteger.valueOf(18)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                blockWitness.add(new RlpList(witness));
            blockProof.add(new RlpList(blockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    /**
     * 
     * Scenario 19: invalid state proof
     * Given:
     *    invalid state proof, hash of proof missmatch with stateRoot in header
     *    state proof hash: ""
     * When:
     *    state root
     * Then:
     *    throw error:
     *       message: "invalid MPT proof"
     *       code: INVALID_MPT
     */
    @Test
    @Order(26)
    public void verifyingScenario19() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = updatedBlocks.get(6).getHash();

        byte[] stateRoot = HexConverter.hexStringToByteArray("05099e0dbdde814d9bb86abc0915301fb833336890ad40761c44350846663157");
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
            List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
                BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId);
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber.longValue(), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // 3 votes, enough 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(3), validatorSecretKey.get(3), round, currentSetId);
            blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));

        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // rlp encoded of blockProof

            List<RlpType> stateProofs = new ArrayList<RlpType>(2);
                List<RlpType> proofs = new ArrayList<RlpType>(1);
                // invalid proofs
                proofs.add(RlpString.create(HexConverter.hexStringToByteArray("80810480d7349ba81a8606735be62a4df34f5ca785ffff19b39b6cd32886a4da9d706c59545e8d434d6125b40443fe11fd292d13a4100300000080985a2c32d72399aa5d04da503ead1ff1a15007afe9b119dec3aa53528d9948c9")));
            stateProofs.add(RlpString.create(HexConverter.hexStringToByteArray("26aa394eea5630e07c48ae0c9558cef780d41e5e16056765bc8461851072c9d7")));
            stateProofs.add(new RlpList(proofs));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(stateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_MPT + ")"));
    }

    /**
     * 
     * Scenario 20: update new authorities list
     * Given:
     *    stateProof with new authrities event
     *    event contains list of 4 new validators
     * When:
     *    current validators set ID: 123
     * Then:
     *    update new validators list to db
     *    increase validator setID: 124
     */
    @Test
    @Order(27)
    public void verifyingScenario20() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = updatedBlocks.get(6).getHash();

        List<byte[]> newValidatorPublicKeys = new ArrayList<byte[]>(5);
        List<byte[]> newValidatorSecretKey = new ArrayList<byte[]>(5);
        newValidatorPublicKeys.add(0, HexConverter.hexStringToByteArray("2f27b7a3f5e8a73ac8e905ff237de902723e969bcc424ddbc2b718d948fc82e8"));
        newValidatorPublicKeys.add(1, HexConverter.hexStringToByteArray("0541be4a4cfda7c722a417cd38807fa5ba9500cf3a8f5440576bf8764f3281c6"));
        newValidatorPublicKeys.add(2, HexConverter.hexStringToByteArray("20852d791571334b261838ab84ee38aac4845e81faac2c677e108a3553b65641"));
        newValidatorPublicKeys.add(3, HexConverter.hexStringToByteArray("ef3a8a0271488f2fbdc787f3285aa42c4c073c2d73f9a3ee288a07471512a6e9"));

        newValidatorSecretKey.add(0, HexConverter.hexStringToByteArray("23fae2b43f110c42a1d32b2c8678d7b7f71eb7873edd0feab952efbc99d1f9d02f27b7a3f5e8a73ac8e905ff237de902723e969bcc424ddbc2b718d948fc82e8"));
        newValidatorSecretKey.add(1, HexConverter.hexStringToByteArray("2b502a205ab7ff2b56a0b225fb6b58c3d78a57a8e03c80938b32e7fbc2c564cc0541be4a4cfda7c722a417cd38807fa5ba9500cf3a8f5440576bf8764f3281c6"));
        newValidatorSecretKey.add(2, HexConverter.hexStringToByteArray("27b9c71dfee22bacee68a3c73c4aac3a7c95b72dbe1876095fcc2d4f96ce5bac20852d791571334b261838ab84ee38aac4845e81faac2c677e108a3553b65641"));
        newValidatorSecretKey.add(3, HexConverter.hexStringToByteArray("b68efa447ebc6ec9f31a5d24c42e4cba41ae7bcc3e7a3f64918648d9e2bf9244ef3a8a0271488f2fbdc787f3285aa42c4c073c2d73f9a3ee288a07471512a6e9"));

        NewAuthoritiesProof newAuthoritiesProof = new NewAuthoritiesProof(newValidatorPublicKeys);

        byte[] stateRoot = newAuthoritiesProof.getRoot();
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
            List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
                BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId);
                updatedBlocks.add(blockUpdate);
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber.longValue(), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // 3 votes, enough 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(3), validatorSecretKey.get(3), round, currentSetId);
            blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));

        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // rlp encoded of blockProof

            List<RlpType> stateProofs = new ArrayList<RlpType>(2);
            stateProofs.add(RlpString.create(newAuthoritiesProof.getEventKey()));
            stateProofs.add(new RlpList(newAuthoritiesProof.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(stateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);

        List<byte[]> storedValidators = bmvScore.validators();
        assertEquals(storedValidators.size(), newValidatorPublicKeys.size());
        assertArrayEquals(storedValidators.get(0), newValidatorPublicKeys.get(0));
        assertArrayEquals(storedValidators.get(1), newValidatorPublicKeys.get(1));
        assertArrayEquals(storedValidators.get(2), newValidatorPublicKeys.get(2));
        assertArrayEquals(storedValidators.get(3), newValidatorPublicKeys.get(3));
        assertTrue(bmvScore.setId().equals(BigInteger.valueOf(124)));

        validatorPublicKeys = newValidatorPublicKeys;
        validatorSecretKey = newValidatorSecretKey;
        currentSetId = bmvScore.setId();
    }

    /**
     * ---------------------------------------------------------------------------------------------
     *                            RESPOND THE MESSAGE FROM BMC
     * ---------------------------------------------------------------------------------------------
     */

    /**
     * 
     * Scenario 2: btp message sequence higher than expected
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
    @Order(28)
    public void respondScenario2() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = bmvScore.mtaLastBlockHash();

        List<byte[]> newValidatorPublicKeys = new ArrayList<byte[]>(5);
        List<byte[]> newValidatorSecretKey = new ArrayList<byte[]>(5);

        String btpNextAddress = bmcBTPAddress;
        BigInteger invalidMessageSequence = BigInteger.valueOf(120);
        byte[] btpMessage = "testencodedmessage".getBytes();
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, invalidMessageSequence, btpMessage);

        byte[] stateRoot = btpEvent.getRoot();
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
            List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
                BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId);
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber.longValue(), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // 3 votes, enough 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(3), validatorSecretKey.get(3), round, currentSetId);
            blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));

        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // rlp encoded of blockProof

            List<RlpType> stateProofs = new ArrayList<RlpType>(2);
            stateProofs.add(RlpString.create(btpEvent.getEventKey()));
            stateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(stateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_SEQUENCE_HIGHER + ")"));
    }

    /**
     * 
     * Scenario 3: btp message sequence lower than expected
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
    @Order(29)
    public void respondScenario3() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = bmvScore.mtaLastBlockHash();

        List<byte[]> newValidatorPublicKeys = new ArrayList<byte[]>(5);
        List<byte[]> newValidatorSecretKey = new ArrayList<byte[]>(5);

        String btpNextAddress = bmcBTPAddress;
        BigInteger invalidMessageSequence = BigInteger.valueOf(105);
        byte[] btpMessage = "testencodedmessage".getBytes();
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, invalidMessageSequence, btpMessage);

        byte[] stateRoot = btpEvent.getRoot();
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
            List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
                BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId);
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber.longValue(), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // 3 votes, enough 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(3), validatorSecretKey.get(3), round, currentSetId);
            blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));

        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // rlp encoded of blockProof

            List<RlpType> stateProofs = new ArrayList<RlpType>(2);
            stateProofs.add(RlpString.create(btpEvent.getEventKey()));
            stateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(stateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_SEQUENCE + ")"));
    }

    /**
     * 
     * Scenario 4: update btp message with block update successfully
     * Given:
     *    update block: 19
     *    btp message sequence: 112
     *    btp message contain in block 19
     * When:
     *    bmc message sequence: 111
     *    current block height: 18
     * Then:
     *    return btp message to BMC
     */
    @Test
    @Order(30)
    public void respondScenario4() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = bmvScore.mtaLastBlockHash();

        List<byte[]> newValidatorPublicKeys = new ArrayList<byte[]>(5);
        List<byte[]> newValidatorSecretKey = new ArrayList<byte[]>(5);

        String btpNextAddress = bmcBTPAddress;
        BigInteger invalidMessageSequence = BigInteger.valueOf(112);
        byte[] btpMessage = "testencodedmessagea".getBytes();
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, invalidMessageSequence, btpMessage);

        byte[] stateRoot = btpEvent.getRoot();
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
            List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
                BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId);
                updatedBlocks.add(blockUpdate);
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber.longValue(), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // 3 votes, enough 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(3), validatorSecretKey.get(3), round, currentSetId);
            blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));

        relayMessage.add(new RlpList(blockUpdates));
        relayMessage.add(RlpString.create("")); // rlp encoded of blockProof

            List<RlpType> stateProofs = new ArrayList<RlpType>(2);
            stateProofs.add(RlpString.create(btpEvent.getEventKey()));
            stateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(stateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);
    }

    /**
     * 
     * Scenario 5: update btp message with block proof successfully
     * Given:
     *    update block: 20, 21
     *    btp message sequence: 113
     *    btp message contain in block 20
     *    block proof for block 20
     * When:
     *    bmc message sequence: 112
     *    current block height: 19
     * Then:
     *    return btp message to BMC
     */
    @Test
    @Order(31)
    public void respondScenario5() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = bmvScore.mtaLastBlockHash();

        List<byte[]> newValidatorPublicKeys = new ArrayList<byte[]>(5);
        List<byte[]> newValidatorSecretKey = new ArrayList<byte[]>(5);

        String btpNextAddress = bmcBTPAddress;
        BigInteger invalidMessageSequence = BigInteger.valueOf(112);
        byte[] btpMessage = "testencodedmessagesecond".getBytes();
        BTPEvent btpEvent = new BTPEvent(prev, HexConverter.hexStringToByteArray(prevBmcAddress), btpNextAddress, invalidMessageSequence, btpMessage);

        byte[] stateRoot = btpEvent.getRoot();
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().add(BigInteger.valueOf(1));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
            List<RlpType> blockUpdates = new ArrayList<RlpType>(3);
            for (int i = 0; i< 2; i++) {
                BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId);
                byte[] voteMessage = BlockUpdate.voteMessage(blockUpdate.getHash(), updatingBlockNumber.longValue(), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(0), validatorSecretKey.get(0), round, currentSetId); // 3 votes, enough 2/3 * 4 = 3 votes
                blockUpdate.vote(validatorPublicKeys.get(1), validatorSecretKey.get(1), round, currentSetId);
                blockUpdate.vote(validatorPublicKeys.get(3), validatorSecretKey.get(3), round, currentSetId);
                blockUpdates.add(RlpString.create(blockUpdate.encodeWithVoteMessage(voteMessage)));
                updatedBlocks.add(blockUpdate);
                updatingBlockNumber = updatingBlockNumber.add(BigInteger.valueOf(1));
                parentHash = blockUpdate.getHash();
                stateRoot = HexConverter.hexStringToByteArray("11ea2d1ee7efe3fdabe1e07a2f6af89996dfd16ba8070b6b9b980d6c1bb34502");
            }

        relayMessage.add(new RlpList(blockUpdates));
            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            blockProof.add(RlpString.create(updatedBlocks.get(9).getEncodedHeader())); // block 20

                List<RlpType> blockWitness = new ArrayList<RlpType>(2);
                blockWitness.add(RlpString.create(BigInteger.valueOf(21)));
                    List<RlpType> witness = new ArrayList<RlpType>(1); // empty witness
                    witness.add(RlpString.create(updatedBlocks.get(8).getHash())); // block 19 hash
                blockWitness.add(new RlpList(witness));
            blockProof.add(new RlpList(blockWitness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof

            List<RlpType> stateProofs = new ArrayList<RlpType>(2);
            stateProofs.add(RlpString.create(btpEvent.getEventKey()));
            stateProofs.add(new RlpList(btpEvent.getProof()));
        relayMessage.add(new RlpList(RlpString.create(RlpEncoder.encode(new RlpList(stateProofs))))); // stateProof with event data

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertSuccess(txResult);
    }
}
