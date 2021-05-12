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

import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.utils.HexConverter;

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
        BigInteger amount = ICX.multiply(BigInteger.valueOf(50));
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

        validatorPublicKeys.add(HexConverter.hexStringToByteArray("25aabce1a96af5c3f6a4c780b6eeeb7769bf32884f1bad285b5faa464e25c487"));
        validatorPublicKeys.add(HexConverter.hexStringToByteArray("8e111029cd8a3754095d96e9d01629f7ee686198b9e8465d9c90101ed867b958"));
        validatorPublicKeys.add(HexConverter.hexStringToByteArray("01573582dc1bc7474e76cd5a5cbefbcf5475284ced05cc66a45dcb65ea29b09b"));
        validatorPublicKeys.add(HexConverter.hexStringToByteArray("d3e7471c8fd4cdb71e791783098794e2295f992ae6a4a27e0f893071ade31b78"));

        validatorSecretKey.add(HexConverter.hexStringToByteArray("c360fab3025db65e3d967f553ddae434382cfbb9130b6de339fd1f3283f350a025aabce1a96af5c3f6a4c780b6eeeb7769bf32884f1bad285b5faa464e25c487"));
        validatorSecretKey.add(HexConverter.hexStringToByteArray("f5880449f4eb08bfa946a6f5bb679a33f58a7f1bd93c52bb4f2e05dad27e45dc8e111029cd8a3754095d96e9d01629f7ee686198b9e8465d9c90101ed867b958"));
        validatorSecretKey.add(HexConverter.hexStringToByteArray("e8c61ebe8654dfcfc279d28eec8af09d1149bbc9ba26049354e880d479ef98f101573582dc1bc7474e76cd5a5cbefbcf5475284ced05cc66a45dcb65ea29b09b"));
        validatorSecretKey.add(HexConverter.hexStringToByteArray("8f97e18992ed2fb7b7f7e37146e2107cb950556cead04c347ad81c69b62ad51fd3e7471c8fd4cdb71e791783098794e2295f992ae6a4a27e0f893071ade31b78"));
    }

    @Test
    @Order(1)
    public void deployBMVScore() throws Exception {
        List<RlpType> listRlpValidators = new ArrayList<RlpType>(5);
        for (byte[] validator: validatorPublicKeys) {
            listRlpValidators.add(RlpString.create(validator));
        }
        byte[] rlpEncodeValidators = RlpEncoder.encode(new RlpList(listRlpValidators));
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
    @Order(2)
    public void scenario1() throws Exception {
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
     *    call `handleRelayMessage` with caller waller
     * When:
     *    registered bmc address: ownerWallet.getAddress() generated when setup
     * Then:
     *    throw error:
     *    message: "not acceptable bmc"
     *    code: NOT_ACCEPTED_BMC_ADDR_ERROR 39
     */
    @Test
    @Order(3)
    public void scenario2() throws Exception {
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
    @Order(4)
    public void scenario3() throws Exception {
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
     * 
     * Scenario 4: input relay message with invalid base64 format
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
    @Order(5)
    public void scenario4() throws Exception {
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
     * Scenario 5: input relay message with invalid RLP format
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
    @Order(6)
    public void scenario5() throws Exception {
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
     * Scenario 6: input relay message with empty BlockUpdate and BlockProof
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
    @Order(7)
    public void scenario6() throws Exception {
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
     * 
     * Scenario 7: input block update with invalid parent hash with mta last block hash
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
    @Order(8)
    public void scenario7() throws Exception {
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
     * Scenario 8: input block update with invalid parent hash in blocks
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
    public void scenario8() throws Exception {
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
     * Scenario 9: input block with height heigher than current updated height
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
    public void scenario9() throws Exception {
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
     * Scenario 10: input block with height lower than current updated height
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
    public void scenario10() throws Exception {
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
     * Scenario 11: update block without votes of validators
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
    public void scenario11() throws Exception {
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
     * Scenario 12: update block with invalid vote, vote for invalid block hash
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
    public void scenario12() throws Exception {
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
     * Scenario 13: update block with invalid vote, vote for invalid block height
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
    public void scenario13() throws Exception {
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
     * Scenario 14: update block with invalid vote, vote of newer validator set id
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
    public void scenario14() throws Exception {
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
     * Scenario 15: update block with invalid vote, signature invalid
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
    public void scenario15() throws Exception {
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
     * Scenario 16: update block with invalid vote, signature not belong to validators
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
    public void scenario16() throws Exception {
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
     * Scenario 17: update block with duplicate vote
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
    public void scenario17() throws Exception {
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
     * Scenario 18: block vote not enough 2/3
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
    public void scenario18() throws Exception {
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
     * Scenario 19: update block successfully
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
    public void scenario19() throws Exception {
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
     * Scenario 20: update blockProof of block that not exist in MTA
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
    public void inputBlockProofErrorHeightHigher() throws Exception {
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
            blockProof.add(RlpString.create(mtaHeight));
                List<RlpType> witness = new ArrayList<RlpType>(1);
            blockProof.add(new RlpList(witness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_PROOF_HEIGHT_HIGHER + ")"));
    }

    @Test
    @Order(22)
    public void mtaNotAllowedOldWitness() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = lastBlockHash.clone();
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger mtaHeight = bmvScore.mtaHeight();

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            blockProof.add(RlpString.create(updatedBlocks.get(2).getEncodedHeader())); // block 2

            blockProof.add(RlpString.create(mtaHeight.subtract(BigInteger.valueOf(5)))); // less than mta height
                List<RlpType> witness = new ArrayList<RlpType>(1);
                witness.add(RlpString.create(updatedBlocks.get(5).getHash()));
            blockProof.add(new RlpList(witness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS_OLD + ")"));
    }

    @Test
    @Order(23)
    public void mtaInvalidOldWitness() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger mtaHeight = bmvScore.mtaHeight();
        long updatingBlockNumber = bmvScore.mtaHeight().subtract(BigInteger.valueOf(3)).longValue();

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber, stateRoot, round, currentSetId); // fake block updated to block proof
            blockProof.add(RlpString.create(blockUpdate.getEncodedHeader())); // block 2

            blockProof.add(RlpString.create(mtaHeight.subtract(BigInteger.valueOf(1)))); // less than mta height, mta with find hash in cache but not found
                List<RlpType> witness = new ArrayList<RlpType>(1);
                witness.add(RlpString.create(updatedBlocks.get(5).getHash()));
            blockProof.add(new RlpList(witness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    @Test
    @Order(24)
    public void mtaInvalidWitnessWithHeightEqualToAt() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger mtaHeight = bmvScore.mtaHeight();
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().subtract(BigInteger.valueOf(2));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId); // fake block updated to block proof
            blockProof.add(RlpString.create(blockUpdate.getEncodedHeader())); // block 2

            blockProof.add(RlpString.create(bmvScore.mtaHeight())); // greater than current mta height
                List<RlpType> witness = new ArrayList<RlpType>(1);
            blockProof.add(new RlpList(witness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }

    @Test
    @Order(25)
    public void mtaInvalidWitnessWithHeightLessThanAt() throws Exception {
        String prevBmcAddress = "08425D9Df219f93d5763c3e85204cb5B4cE33aAa";
        String prev = "btp://" + destinationNet + "/" + prevBmcAddress;
        BigInteger seq = new BigInteger("111");
        BigInteger round = new BigInteger("123");
        byte[] parentHash = HexConverter.hexStringToByteArray("e65489f4471fe415ce09008134d496995fa3a2bd6a83470a70fd00b684c08d3a");
        byte[] stateRoot = HexConverter.hexStringToByteArray("e488e2726bec22ef07ed0c540d0a6094d41998cd845ebedb773be216d17a44a1");
        BigInteger mtaHeight = bmvScore.mtaHeight();
        BigInteger updatingBlockNumber = bmvScore.mtaHeight().subtract(BigInteger.valueOf(2));

        List<RlpType> relayMessage = new ArrayList<RlpType>(3);
        relayMessage.add(new RlpList()); // empty block update

            List<RlpType> blockProof = new ArrayList<RlpType>(2);
            BlockUpdate blockUpdate = new BlockUpdate(parentHash, updatingBlockNumber.longValue(), stateRoot, round, currentSetId); // fake block updated to block proof
            blockProof.add(RlpString.create(blockUpdate.getEncodedHeader())); // block 2

            blockProof.add(RlpString.create(bmvScore.mtaHeight().add(BigInteger.valueOf(1)))); // greater than current mta height
                List<RlpType> witness = new ArrayList<RlpType>(1);
            blockProof.add(new RlpList(witness));
        relayMessage.add(RlpString.create(RlpEncoder.encode(new RlpList(blockProof)))); // rlp encoded of blockProof
        relayMessage.add(new RlpList()); // empty stateProof

        byte[] rlpEncodeRelayMessage = RlpEncoder.encode(new RlpList(relayMessage));
        String encodedBase64RelayMessage = new String(Base64.getUrlEncoder().encode(rlpEncodeRelayMessage));

        Bytes id = bmvScore.handleRelayMessage(ownerWallet, bmcBTPAddress, prev, seq, encodedBase64RelayMessage);
        TransactionResult txResult = txHandler.getResult(id);
        assertTrue(txResult.getFailure().getMessage().equals("Reverted(" + ErrorCode.INVALID_BLOCK_WITNESS + ")"));
    }
}
