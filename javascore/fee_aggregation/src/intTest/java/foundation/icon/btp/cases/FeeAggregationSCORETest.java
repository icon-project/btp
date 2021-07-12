package foundation.icon.btp.cases;

import foundation.icon.btp.*;
import foundation.icon.btp.score.FeeAggregationScore;
import foundation.icon.btp.score.SampleMultiTokenScore;
import foundation.icon.btp.score.SampleTokenScore;
import foundation.icon.icx.IconService;
import foundation.icon.icx.KeyWallet;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.http.HttpProvider;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static foundation.icon.btp.Env.LOG;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigInteger;

public class FeeAggregationSCORETest extends TestBase {
    private static TransactionHandler txHandler;
    private static KeyWallet[] wallets;
    private static KeyWallet ownerWallet;
    private static SampleTokenScore tokenScore;
    private static SampleMultiTokenScore irc31TokenScore;
    private static FeeAggregationScore feeAggregationScore;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        IconService iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL()));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        wallets = new KeyWallet[6];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(200));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
        ownerWallet = wallets[0];

        // Deploy token SCORE
        BigInteger decimals = BigInteger.valueOf(18);
        BigInteger initialSupply = BigInteger.valueOf(1000);
        tokenScore = SampleTokenScore.mustDeploy(txHandler, ownerWallet,
                decimals, initialSupply);

        // Deploy multi-token SCORE && mint token
        irc31TokenScore = SampleMultiTokenScore.mustDeploy(txHandler, ownerWallet);
        irc31TokenScore.mintToken(ownerWallet);

        // Deploy FAS
        feeAggregationScore = FeeAggregationScore.mustDeploy(txHandler, ownerWallet);

        // prepare
        LOG.infoEntering("transfer token", "10 " + SampleTokenScore.NAME + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer10TokenToFAS = tokenScore.transfer(ownerWallet, feeAggregationScore.getAddress(), ICX.multiply(BigInteger.TEN));
        tokenScore.ensureTransfer(resultOfTransfer10TokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), ICX.multiply(BigInteger.TEN), null);
        LOG.infoExiting();

        LOG.infoEntering("transfer multi-token", "100000 " + SampleMultiTokenScore.ID + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer100000MultiTokenToFAS = irc31TokenScore.transfer(ownerWallet, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, BigInteger.valueOf(100000), null);
        irc31TokenScore.ensureTransfer(resultOfTransfer100000MultiTokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, BigInteger.valueOf(100000), null);
        LOG.infoExiting();

        LOG.infoEntering("call", "registerIRC2() - register " + SampleTokenScore.NAME);
        feeAggregationScore.ensureRegisterIRC2Success(ownerWallet, SampleTokenScore.NAME, tokenScore.getAddress());
        LOG.infoExiting();

        LOG.infoEntering("call", "registerIRC31() - register IRC31 Token " + SampleMultiTokenScore.NAME);
        feeAggregationScore.ensureRegisterIRC31Success(ownerWallet, SampleMultiTokenScore.NAME, irc31TokenScore.getAddress(), SampleMultiTokenScore.ID);
        LOG.infoExiting();
    }

    @AfterAll
    static void shutdown() throws Exception {
        for (KeyWallet wallet : wallets) {
            txHandler.refundAll(wallet);
        }
    }

    /**
     * Scenario 1: if User submits a bid for SampleToken for the first time
     *
     * Given:
     *      - There are 10 SampleToken in the Fee Aggregation system
     *      - UserA has 200 ICX
     * When:
     *      - UserA send the request to bid for 10 SampleToken by 100 ICX to the Fee Aggregation system
     * Then:
     *      - UserA deposit 100 ICX to the Fee Aggregation system for 10 SampleToken
     *      - Start an auction for 10 SampleToken
     */
    @Test
    void scenario1Test() throws Exception {
        KeyWallet userA = wallets[1];

        // UserA bid 100 ICX for 10 SampleToken
        LOG.infoEntering("transact", "bid() - UserA deposit 100 ICX to bid 10 " + SampleTokenScore.NAME);
        feeAggregationScore.ensureBidSuccess(txHandler, userA, SampleTokenScore.NAME, ICX.multiply(BigInteger.valueOf(100)));
        // UserA remain less than (200 - 100) ICX
        BigInteger balanceA = txHandler.getBalance(userA.getAddress());
        assertNotEquals(balanceA.compareTo(ICX.multiply(BigInteger.valueOf(100))), 1);
        feeAggregationScore.ensureAuctionInfo(userA, SampleTokenScore.NAME, ICX.multiply(BigInteger.valueOf(100)), userA.getAddress());
        LOG.infoExiting();
    }

    /**
     * Scenario 2: if User submits a bid for SampleToken which being auctioned
     *
     * Given:
     *      - 10 SampleToken have been bid by UserA with 100 ICX
     *      - UserB has 200 ICX
     * When:
     *      - UserB sends the request to bid for 10 SampleToken by 150 ICX to the Fee Aggregation system
     * Then:
     *      - UserB deposit 150 ICX to the Fee Aggregation system for 10 SampleToken
     *      - Fee Aggregation System refund 100 ICX to UserA
     */
    @Test
    void scenario2Test() throws Exception {
        KeyWallet userA = wallets[1];
        KeyWallet userB = wallets[2];
        BigInteger balanceA = txHandler.getBalance(userA.getAddress());

        // UserB bid 150 ICX for 10 SampleToken
        LOG.infoEntering("transact", "bid() - UserB deposit 150 ICX to bid 10 " + SampleTokenScore.NAME);
        feeAggregationScore.ensureBidSuccess(txHandler, userB, SampleTokenScore.NAME, ICX.multiply(BigInteger.valueOf(150)));
        feeAggregationScore.ensureAuctionInfo(userB, SampleTokenScore.NAME, ICX.multiply(BigInteger.valueOf(150)), userB.getAddress());
        // UserB remain less than (200 - 150) ICX
        BigInteger balanceB = txHandler.getBalance(userB.getAddress());
        assertNotEquals(balanceB.compareTo(ICX.multiply(BigInteger.valueOf(50))), 1);

        // UserA balance +100 ICX
        BigInteger balanceARefunded = txHandler.getBalance(userA.getAddress());
        assertEquals(balanceARefunded, balanceA.add(ICX.multiply(BigInteger.valueOf(100))));
        LOG.infoExiting();
    }

    /**
     * Scenario 3: if User submits a bid for SampleTokenA which Fee Aggregation system does not register
     *
     * Given:
     *      - Fee Aggregation System does not register SampleTokenA
     *      - UserC has 100 ICX
     * When:
     *      - UserC sends the request to bid for 10 SampleToken by 100 ICX to the Fee Aggregation system
     * Then:
     *      - Transaction Revert
     */
    @Test
    void scenario3Test() {
        KeyWallet userC = wallets[3];
        String tokenNameNotRegisted = "SampleTokenA";

        // UserC bid 100 ICX for 10 SampleToken
        LOG.infoEntering("transact", "bid() - UserC deposit 100 ICX to bid 10 " + tokenNameNotRegisted);
        assertThrows(TransactionFailureException.class, () -> feeAggregationScore.ensureBidSuccess(txHandler, userC, tokenNameNotRegisted, ICX.multiply(BigInteger.valueOf(100))));
        LOG.infoExiting("Transaction Revert");
    }

    /**
     * Scenario 4: if User submits a bid for amount SampleTokenB equal 0 in the Fee Aggregation system
     *
     * Given:
     *      - SampleTokenB balance is 0 in Fee Aggregation System
     *      - UserC has 100 ICX
     * When:
     *      - UserC sends the request to bid for 10 SampleTokenB by 100 ICX to the Fee Aggregation system
     * Then:
     *      - Transaction Revert
     */
    @Test
    void scenario4Test() throws Exception {
        KeyWallet userC = wallets[3];
        String tokenNameB = "SampleTokenB";
        LOG.infoEntering("call", "registerIRC2() - register " + tokenNameB);
        feeAggregationScore.ensureRegisterIRC2Success(ownerWallet, "tokenNameB", tokenScore.getAddress());
        LOG.infoExiting();

        // UserC bid 100 ICX for 0 SampleTokenB
        LOG.infoEntering("transact", "bid() - UserC deposit 100 ICX to bid 0 " + tokenNameB);
        assertThrows(TransactionFailureException.class, () -> feeAggregationScore.ensureBidSuccess(txHandler, userC, tokenNameB, ICX.multiply(BigInteger.valueOf(100))));
        LOG.infoExiting("Transaction Revert");
    }

    /**
     * Scenario 5: if User submits a bid value less than 100 ICX to the Fee Aggregation system
     *
     * Given:
     *      - There are 10 SampleToken in the Fee Aggregation system
     *      - UserC has 200 ICX
     * When:
     *      - UserC send the request to bid for 10 SampleToken by 90 ICX to the Fee Aggregation system
     * Then:
     *      - Transaction Revert
     */
    @Test
    void scenario5Test() {
        KeyWallet userC = wallets[3];

        // UserC bid 90 ICX for 10 SampleToken
        LOG.infoEntering("transact", "bid() - UserC deposit 90 ICX to bid 10 " + SampleTokenScore.NAME);
        assertThrows(TransactionFailureException.class, () -> feeAggregationScore.ensureBidSuccess(txHandler, userC, SampleTokenScore.NAME, ICX.multiply(BigInteger.valueOf(90))));
        LOG.infoExiting("Transaction Revert");
    }

    /**
     * Scenario 6: if User submits a bid for the auction ended to the Fee Aggregation system
     *
     * Given:
     *      - There are 100000 MyIRC31SampleToken in the Fee Aggregation system and ended
     *      - Winner of the ended auction is UserC
     *      - TokenName MyIRC31SampleToken available balance is 150000
     *      - UserD has 200 ICX
     * When:
     *      - UserD send the request to bid for TokenName MyIRC31SampleToken by 100 ICX to the Fee Aggregation system
     * Then:
     *      - Transfer 100000 TokenName MyIRC31SampleToken to winner UserC
     *      - Start a new auction for 50000 (150000 - 100000) TokenName MyIRC31SampleToken
     *      - UserD bid for new auction
     */
    @Test
    void scenario6Test() throws Exception {
        KeyWallet userC = wallets[3];
        KeyWallet userD = wallets[4];

        feeAggregationScore.ensureSetDurationSuccess(ownerWallet, BigInteger.valueOf(1000));

        // UserC bid 100 ICX for 100000 MyIRC31SampleToken
        LOG.infoEntering("transact", "bid() - UserC deposit 100 ICX to bid 100000 " + SampleMultiTokenScore.NAME);
        feeAggregationScore.ensureBidSuccess(txHandler, userC, SampleMultiTokenScore.NAME, ICX.multiply(BigInteger.valueOf(100)));

        LOG.infoEntering("transfer more multi-token", "50000 " + SampleMultiTokenScore.ID + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer50000MultiTokenToFAS = irc31TokenScore.transfer(ownerWallet, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, BigInteger.valueOf(50000), null);
        irc31TokenScore.ensureTransfer(resultOfTransfer50000MultiTokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, BigInteger.valueOf(50000), null);
        LOG.infoExiting();

        feeAggregationScore.ensureSetDurationSuccess(ownerWallet, BigInteger.valueOf(1000*3600*12));

        // UserD bid 150 ICX for this below ended auction
        LOG.infoEntering("transact", "bid() - UserD deposit 150 ICX to bid 50000 " + SampleMultiTokenScore.NAME + " and start new auction");
        feeAggregationScore.ensureBidSuccess(txHandler, userD, SampleMultiTokenScore.NAME, ICX.multiply(BigInteger.valueOf(150)));

        // UserC remain less than (200 - 100) ICX
        BigInteger balanceICXOfC = txHandler.getBalance(userC.getAddress());
        assertNotEquals(balanceICXOfC.compareTo(ICX.multiply(BigInteger.valueOf(100))), 1);

        // UserC receive 100000 MyIRC31SampleToken
        BigInteger balanceTokenOfC = irc31TokenScore.balanceOf(userC.getAddress(), SampleMultiTokenScore.ID);
        assertNotEquals(balanceTokenOfC.compareTo(BigInteger.valueOf(100000)), 1);

        // UserD remain less than (200 - 150) ICX
        BigInteger balanceICXOfD = txHandler.getBalance(userD.getAddress());
        assertNotEquals(balanceICXOfD.compareTo(ICX.multiply(BigInteger.valueOf(50))), 1);
        feeAggregationScore.ensureAuctionInfo(userD, SampleMultiTokenScore.NAME, ICX.multiply(BigInteger.valueOf(150)), userD.getAddress());
    }

    /**
     * Scenario 7: if User submits a bid value not 10% higher than the current highest bid of the auction to the Fee Aggregation system
     *
     * Given:
     *      - The current highest bid is 150 ICX for 50000 MyIRC31SampleToken by UserD
     *      - UserE has 200 ICX
     * When:
     *      - UserE send the request to bid for 50000 MyIRC31SampleToken by 160 ICX to the Fee Aggregation system
     * Then:
     *      - Transaction Revert
     */
    @Test
    void scenario7Test() {
        KeyWallet userE = wallets[5];

        // UserE bid 160 ICX for 50000 MyIRC31SampleToken
        LOG.infoEntering("transact", "bid() - UserE deposit 160 ICX to bid 50000 " + SampleMultiTokenScore.NAME);
        assertThrows(TransactionFailureException.class, () -> feeAggregationScore.ensureBidSuccess(txHandler, userE, SampleMultiTokenScore.NAME, ICX.multiply(BigInteger.valueOf(160))));
        LOG.infoExiting("Transaction Revert");
    }
}
