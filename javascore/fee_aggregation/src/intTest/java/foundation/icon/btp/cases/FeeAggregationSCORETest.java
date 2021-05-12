package foundation.icon.btp.cases;

import foundation.icon.btp.*;
import foundation.icon.btp.score.FeeAggregationScore;
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
    private static FeeAggregationScore feeAggregationScore;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        IconService iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL(3), 3));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        wallets = new KeyWallet[5];
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

        // Deploy FAS
        feeAggregationScore = FeeAggregationScore.mustDeploy(txHandler, ownerWallet);

        // prepare
        LOG.infoEntering("transfer token", "10 " + SampleTokenScore.NAME + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer10TokenToFAS = tokenScore.transfer(ownerWallet, feeAggregationScore.getAddress(), ICX.multiply(BigInteger.TEN));
        tokenScore.ensureTransfer(resultOfTransfer10TokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), ICX.multiply(BigInteger.TEN), null);
        LOG.infoExiting();

        LOG.infoEntering("call", "register() - register " + SampleTokenScore.NAME);
        feeAggregationScore.ensureRegisterSuccess(ownerWallet, SampleTokenScore.NAME, tokenScore.getAddress());
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
        LOG.infoEntering("call", "register() - register " + tokenNameB);
        feeAggregationScore.ensureRegisterSuccess(ownerWallet, "tokenNameB", tokenScore.getAddress());
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
     * Scenario 7: if User submits a bid value not 10% higher than the current highest bid of the auction to the Fee Aggregation system
     *
     * Given:
     *      - The current highest bid is 150 ICX for 10 SampleToken by UserB
     *      - UserC has 200 ICX
     * When:
     *      - UserC send the request to bid for 10 SampleToken by 160 ICX to the Fee Aggregation system
     * Then:
     *      - Transaction Revert
     */
    @Test
    void scenario7Test() {
        KeyWallet userC = wallets[3];

        // UserC bid 160 ICX for 10 SampleToken
        LOG.infoEntering("transact", "bid() - UserC deposit 90 ICX to bid 10 " + SampleTokenScore.NAME);
        assertThrows(TransactionFailureException.class, () -> feeAggregationScore.ensureBidSuccess(txHandler, userC, SampleTokenScore.NAME, ICX.multiply(BigInteger.valueOf(160))));
        LOG.infoExiting("Transaction Revert");
    }
}
