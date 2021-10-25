package foundation.icon.btp.test;

import foundation.icon.btp.bmv.BTPMessageVerifier;
import foundation.icon.btp.bmv.lib.HexConverter;
import foundation.icon.btp.bmv.lib.TypeDecoder;
import foundation.icon.btp.bmv.lib.mta.MerkleTreeAccumulator;
import foundation.icon.btp.bmv.types.*;
import foundation.icon.btp.bmv.types.BlockHeader;
import foundation.icon.btp.bmv.types.Votes;
import foundation.icon.ee.io.DataWriter;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.common.*;
import foundation.icon.test.score.Score;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import scorex.util.Base64;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import static foundation.icon.test.common.Env.LOG;
import static org.junit.jupiter.api.Assertions.assertTrue;

    @Tag(Constants.TAG_JAVA_SCORE)
    public class Test extends TestBase {
        final static String RLPn = "RLPn";

        private static Score test;
        private static IconService iconService;
        private static TransactionHandler txHandler;
        private static SecureRandom secureRandom;
        private static KeyWallet[] wallets;
        private static KeyWallet ownerWallet, caller;

        @BeforeAll
        static void init() throws Exception {
            Env.Node node = Env.nodes[0];
            Env.Channel channel = node.channels[0];
            Env.Chain chain = channel.chain;
            HttpProvider provider = new HttpProvider(channel.getAPIUrl(Env.testApiVer));
            iconService = new IconService(provider);

            System.out.println("iconService => " + channel.getAPIUrl(Env.testApiVer));
            txHandler = new TransactionHandler(iconService, chain);
            secureRandom = new SecureRandom();

            // init wallets
            wallets = new KeyWallet[3];
            DataWriter writer = foundation.icon.test.common.Codec.rlp.newWriter();
            writer.writeListHeader(wallets.length);
            BigInteger amount = ICX.multiply(BigInteger.valueOf(10000));
            for (int i = 0; i < wallets.length; i++) {
                wallets[i] = KeyWallet.create();
                txHandler.transfer(wallets[i].getAddress(), amount);
                writer.write(wallets[i].getAddress().getBody());
            }

            writer.writeFooter();

            for (KeyWallet wallet : wallets) {
                ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
            }
            ownerWallet = wallets[0];
            caller = wallets[1];

            test = deployTest(txHandler, ownerWallet);
            //test = deployTest(txHandler, ownerWallet);

        }

        @AfterAll
        static void shutdown() throws Exception {
            for (KeyWallet wallet : wallets) {
                txHandler.refundAll(wallet);
            }
        }


        public static Score deployTest(TransactionHandler txHandler, Wallet owner)
                throws ResultTimeoutException, TransactionFailureException, IOException {
            LOG.infoEntering("deploy", "Test");
            String encodedValidators="";
            RpcObject args = new RpcObject.Builder()
                    .put("base64", new RpcValue(encodedValidators.getBytes()))
                    .build();

            Score score = txHandler.deploy(owner, new Class[]{
//                            foundation.icon.btp.bmv.Test.class,
                            HexConverter.class,
                            scorex.util.Arrays.class,
                            Base64.class},
                    args);

            LOG.info("Deployed BMV address " + score.getAddress());
            LOG.infoExiting();
            return score;
        }

        /**
         * Scenario 1: Receiving address is an invalid address - fail
         */
        @Order(1)
        @org.junit.jupiter.api.Test
        public void scenario1() throws IOException, ResultTimeoutException {

            //header bytes sample value from verify.js poc:108 "headerEncoded" var
            byte[] headerBytes= Hex.decode("f901f7a0762577e92f95731a13473cc53e02fdd689963d993bb5e154b284fa4803e89142a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a0997a1add7987e3d4e4498fa04fb343411676b44e4f229c8764ba8007d515a22fa0ce8bc9f9a3bb622d7eb3c2506f6e00a9f0d01d2ae19f2c02b5f39bdb7cb1c717a0d5fb9fafd6b0c3d46d6c9e08d9947a70e95400dd0bf21467ea3d3f17ce77651bb90100010000020000000000000002000800000000000000000000008000800000000000000000800000000000000000000200000000000000000000000000002000000000000000000000000000080000000000000080000000000000000000000000200000000000000040000000840000000020000000800000000000100000000000000000100000000000000000000008000000000000000000000008001000000200000000080000040040000000000000000000000000001000000000000000002000020000000000000000000000000000000000000800000040000000000000100000000000000000010000000000000000000000000000000000000000008038836691b78305177c8460c09b7b80a00000000000000000000000000000000000000000000000000000000000000000880000000000000000");

            //witness got from poc verify.js:47 "witness" of transactionProof
            byte[] witness = Hex.decode("f9024b378504a817c8008347e7c4948cd1d5d16caf488efc057e4fc3add7c11b01d9b080b901e4e995e3de00000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000014000000000000000000000000053f1aaac3db0557bd6413da6ade72e53d45b77ab00000000000000000000000000000000000000000000000000000000000001a00000000000000000000000000000000000000000000000000000000000000064000000000000000000000000000000000000000000000000000000000000000362736300000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008546f6b656e425348000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002a307835343332364232616436413741663733453066384538613434373845313343323643423832393439000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003455448000000000000000000000000000000000000000000000000000000000025a01d1d49798a10c7abb230fcbaedb48867a2e3f690c0671d8841451fb4d7e8c10aa0528cf1e4ea354ab2d01dde65c724a22d3807d6b9b77fad41700ad53a7d4914e2");

            //receipts root hash = d5fb9fafd6b0c3d46d6c9e08d9947a70e95400dd0bf21467ea3d3f17ce77651b
            // from  keccak(rlp.encode(resp.receiptProof[0]))
            // rp= mptproofs = needed to prove the Receipt in MPT = rlp.encode(encodedProof).toString('hex') where encodedeProof = rlp.encode(resp.receiptProof)

            byte[] rp = Hex.decode("f905a5b905a2f9059f822080b90599f90596018305177cb9010001000002000000000000000200080000000000000000000000800080000000000000000080000000000000000000020000000000000000000000000000200000000000000000000000000008000000000000008000000000000000000000000020000000000000004000000084000000002000000080000000000010000000000000000010000000000000000000000800000000000000000000000800100000020000000008000004004000000000000000000000000000100000000000000000200002000000000000000000000000000000000000080000004000000000000010000000000000000001000000000000000000000000000000000000000000f9048bf89b94b27345f8e20bf8cdd839c837b792b5452c282c22f863a08c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925a00000000000000000000000009c0604273c25c268bad67935553d82437387a397a00000000000000000000000009c0604273c25c268bad67935553d82437387a397a00000000000000000000000000000000000000000000000000000000000000064f89b94b27345f8e20bf8cdd839c837b792b5452c282c22f863a0ddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3efa00000000000000000000000009c0604273c25c268bad67935553d82437387a397a000000000000000000000000053f1aaac3db0557bd6413da6ade72e53d45b77aba00000000000000000000000000000000000000000000000000000000000000064f89b94b27345f8e20bf8cdd839c837b792b5452c282c22f863a08c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925a00000000000000000000000009c0604273c25c268bad67935553d82437387a397a00000000000000000000000009c0604273c25c268bad67935553d82437387a397a00000000000000000000000000000000000000000000000000000000000000000f899948cd1d5d16caf488efc057e4fc3add7c11b01d9b0e1a0aa0f21ab61398ccea95cd4e139e0d3c2cf1438e8034b7e8e9701b80bc39d2f56b86000000000000000000000000000000000000000000000000000000000000000380000000000000000000000000000000000000000000000000000000000000000762577e92f95731a13473cc53e02fdd689963d993bb5e154b284fa4803e89142f9013b948cd1d5d16caf488efc057e4fc3add7c11b01d9b0f842a037be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1ba083d57b2915dae13afb3bdeac7357363ed5e6545fb90a89f09722f0f95f9efa91b8e0000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000000000000000000000000000000400000000000000000000000000000000000000000000000000000000000000066f864b83d6274703a2f2f6273632f3078386364316435643136636166343838656663303537653466633361646437633131623031643962300000000000000000008362736388546f6b656e4253480096d50293d200905472616e7366657220537563636573730000000000000000000000000000000000000000000000000000f8d9949c0604273c25c268bad67935553d82437387a397e1a0356868e4a05430bccb6aa9c954e410ab0792c5a5baa7b973b03e1d4c03fa1366b8a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000105472616e73666572205375636365737300000000000000000000000000000000");

            // Mocking relay message from actual data
            //relay message

            DataWriter relayMsgWriter = foundation.icon.test.common.Codec.rlp.newWriter();
            //ByteArrayObjectWriter relayMsgWriter = Context.newByteArrayObjectWriter(RLPn);
            relayMsgWriter.writeListHeader(3);

            //blockUpdates
            relayMsgWriter.writeListHeader(1);

            DataWriter blockUpdateWriter = foundation.icon.test.common.Codec.rlp.newWriter();
            blockUpdateWriter.writeListHeader(3);
            blockUpdateWriter.writeNullity( true);
            blockUpdateWriter.writeNullity(true);
            blockUpdateWriter.writeNullity(true);
            blockUpdateWriter.writeFooter();
            relayMsgWriter.write(blockUpdateWriter.toByteArray());
            relayMsgWriter.writeFooter();

            //blockProof
            DataWriter blockProofWrtr =  foundation.icon.test.common.Codec.rlp.newWriter();
            blockProofWrtr.writeListHeader(2);
            blockProofWrtr.writeNullity(true); //block header
            blockProofWrtr.writeNullity(true); // block witness
            blockProofWrtr.writeFooter();
            relayMsgWriter.write(blockProofWrtr.toByteArray());

            //receiptProof
            relayMsgWriter.writeListHeader(1);
            DataWriter receiptProofWtr = foundation.icon.test.common.Codec.rlp.newWriter();
            receiptProofWtr.writeListHeader(4);
            receiptProofWtr.write(0);
            receiptProofWtr.write(rp); // receipt proof
            receiptProofWtr.writeNullity(true);
            receiptProofWtr.writeNullity(true);
            receiptProofWtr.writeFooter();
            relayMsgWriter.write(receiptProofWtr.toByteArray());
            relayMsgWriter.writeFooter();
            relayMsgWriter.writeFooter();
            byte[] _msg = Base64.getUrlEncoder().encode(relayMsgWriter.toByteArray());

            RpcObject args = new RpcObject.Builder()
                    .put("msg", new RpcValue(_msg))
                    .build();

            TransactionResult txResult = test.invokeAndWaitResult(wallets[0], "loopTest", args);
            assertTrue(txResult.getFailure() != null);


        }
}
