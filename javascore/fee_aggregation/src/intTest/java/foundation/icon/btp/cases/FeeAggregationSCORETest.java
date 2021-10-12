package foundation.icon.btp.cases;

import foundation.icon.btp.*;
import foundation.icon.btp.score.BandProtocolScore;
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
import java.util.concurrent.TimeUnit;
import java.util.Date;

public class FeeAggregationSCORETest extends TestBase {
    private static TransactionHandler txHandler;
    private static KeyWallet[] wallets;
    private static KeyWallet ownerWallet;
    private static SampleTokenScore sampleTokenScore;
    private static SampleMultiTokenScore irc31TokenScore;
    private static FeeAggregationScore feeAggregationScore;
    private static BandProtocolScore bandProtocolScore;
    private static IconService iconService;

    @BeforeAll
    static void setup() throws Exception {
        Env.Chain chain = Env.getDefaultChain();
        OkHttpClient ohc = new OkHttpClient.Builder().build();
        iconService = new IconService(new HttpProvider(ohc, chain.getEndpointURL()));
        txHandler = new TransactionHandler(iconService, chain);

        // init wallets
        wallets = new KeyWallet[6];
        BigInteger amount = ICX.multiply(BigInteger.valueOf(300));
        for (int i = 0; i < wallets.length; i++) {
            wallets[i] = KeyWallet.create();
            txHandler.transfer(wallets[i].getAddress(), amount);
        }
        for (KeyWallet wallet : wallets) {
            ensureIcxBalance(txHandler, wallet.getAddress(), BigInteger.ZERO, amount);
        }
        ownerWallet = wallets[0];

        // Deploy token SCORE
        BigInteger sampleTokenDecimals = BigInteger.valueOf(18);
        BigInteger initialSupply = BigInteger.valueOf(1000);
        sampleTokenScore = SampleTokenScore.mustDeploy(txHandler, ownerWallet, "SampleToken",
        sampleTokenDecimals, initialSupply);

        // Deploy multi-token SCORE && mint token
        BigInteger irc31Decimals = BigInteger.valueOf(10);
        irc31TokenScore = SampleMultiTokenScore.mustDeploy(txHandler, ownerWallet, irc31Decimals);
        irc31TokenScore.mintToken(ownerWallet);

        bandProtocolScore = BandProtocolScore.mustDeploy(txHandler, ownerWallet);

        // Deploy FAS
        feeAggregationScore = FeeAggregationScore.mustDeploy(txHandler, ownerWallet, bandProtocolScore.getAddress());

        // prepare
        LOG.infoEntering("transfer token", "10 " + sampleTokenScore.name() + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer10TokenToFAS = sampleTokenScore.transfer(ownerWallet, feeAggregationScore.getAddress(), ICX.multiply(BigInteger.TEN));
        sampleTokenScore.ensureTransfer(resultOfTransfer10TokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), ICX.multiply(BigInteger.TEN), null);
        LOG.infoExiting();

        LOG.infoEntering("transfer multi-token", "100 " + SampleMultiTokenScore.ID + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer100000MultiTokenToFAS = irc31TokenScore.transfer(ownerWallet, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, irc31TokenScore.unit().multiply(BigInteger.valueOf(100)), null);
        irc31TokenScore.ensureTransfer(resultOfTransfer100000MultiTokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, irc31TokenScore.unit().multiply(BigInteger.valueOf(100)), null);
        LOG.infoExiting();

        LOG.infoEntering("call", "registerIRC2() - register " + sampleTokenScore.name());
        feeAggregationScore.ensureRegisterIRC2Success(ownerWallet, sampleTokenScore.name(), sampleTokenScore.getAddress());
        LOG.infoExiting();

        LOG.infoEntering("call", "registerIRC31() - register IRC31 Token " + SampleMultiTokenScore.NAME);
        feeAggregationScore.ensureRegisterIRC31Success(ownerWallet, SampleMultiTokenScore.NAME, irc31TokenScore.getAddress(), SampleMultiTokenScore.ID, irc31Decimals);
        LOG.infoExiting();

        LOG.infoEntering("transact", "setDuration() - set duration to 30s " + SampleMultiTokenScore.NAME);
        feeAggregationScore.ensureSetDurationSuccess(ownerWallet, BigInteger.valueOf(1000*1000*30)); // 100s
        LOG.infoExiting();
        
        LOG.infoEntering("transact", "relay() - set rate of " + sampleTokenScore.name() + " to 100 ICX");
        bandProtocolScore.relay(ownerWallet, "[\"" + sampleTokenScore.name() + "\",\"ICX\",\"USDC\"]",  "[100000000000000, 1000000000000,1000000000]", "[1633508543,1633508543,1633508543]", "[5684902,5684904,5684902]");
        LOG.infoExiting();
    }

    @AfterAll
    static void shutdown() throws Exception {
        for (KeyWallet wallet : wallets) {
            txHandler.refundAll(wallet);
        }
    }

    /**
     * Scenario 1: if User submits a deposit for SampleToken for the first time
     *
     * Given:
     *      - There are 10 SampleToken in the Fee Aggregation system
     *      - UserA has 200 ICX
     * When:
     *      - UserA send the request to deposit for 10 SampleToken by 100 ICX to the Fee Aggregation system
     * Then:
     *      - UserA deposit 100 ICX to the Fee Aggregation system for 10 SampleToken
     *      - Start an syndication for 10 SampleToken
     */
    @Test
    void scenario1Test() throws Exception {
        KeyWallet userA = wallets[1];
        BigInteger depositAmount = ICX.multiply(BigInteger.valueOf(100));
        BigInteger tokenAmount = ICX.multiply(BigInteger.TEN);

        // UserA bid 100 ICX for 10 SampleToken
        LOG.infoEntering("transact", "deposit() - UserA deposit 100 ICX to bid 10 " + sampleTokenScore.name());
        feeAggregationScore.ensureDepositIcxSuccess(txHandler, userA, sampleTokenScore.name(), depositAmount);
        // UserA remain less than (200 - 100) ICX because of transaction fee
        BigInteger balanceA = txHandler.getBalance(userA.getAddress());
        assertTrue(balanceA.compareTo(ICX.multiply(BigInteger.valueOf(200))) < 0);
        feeAggregationScore.ensureSyndicationInfo(userA, sampleTokenScore.name(), tokenAmount, depositAmount);
        feeAggregationScore.ensureDepositAmount(sampleTokenScore.name(), depositAmount, userA.getAddress());
        LOG.infoExiting();
    }

    /**
     * Scenario 2: if User submits a deposit for SampleToken which syndication is active
     *
     * Given:
     *      - 10 SampleToken have been deposited by UserA with 100 ICX
     *      - UserB has 200 ICX
     * When:
     *      - UserB sends the request to deposit for 10 SampleToken by 150 ICX to the Fee Aggregation system
     * Then:
     *      - Syndication now has two shareholders
     *      - UserA deposit 100 ICX
     *      - UserB deposit 150 ICX
     *      - totalDeposited 250 ICX
     */
    @Test
    void scenario2Test() throws Exception {
        KeyWallet userB = wallets[2];
        BigInteger tokenAmount = ICX.multiply(BigInteger.TEN);
        BigInteger depositAmount = ICX.multiply(BigInteger.valueOf(150));

        // UserB bid 150 ICX for 10 SampleToken
        LOG.infoEntering("transact", "deposit() - UserB deposit 150 ICX to bid 10 " + sampleTokenScore.name());
        feeAggregationScore.ensureDepositIcxSuccess(txHandler, userB, sampleTokenScore.name(), depositAmount);
        feeAggregationScore.ensureSyndicationInfo(userB, sampleTokenScore.name(), tokenAmount, ICX.multiply(BigInteger.valueOf(250)));
        // UserB remain less than (300 - 150) ICX
        BigInteger balanceB = txHandler.getBalance(userB.getAddress());
        assertNotEquals(balanceB.compareTo(ICX.multiply(BigInteger.valueOf(150))), 1);
        feeAggregationScore.ensureDepositAmount(sampleTokenScore.name(), depositAmount, userB.getAddress());
        LOG.infoExiting();
    }

    /**
     * Scenario 3: if User submits a additional deposit for SampleToken
     *
     * Given:
     *      - There are 10 SampleToken in the Fee Aggregation system
     *      - UserA has deposited 100 ICX
     * When:
     *      - UserA send the request to deposit additional 60 ICX for 10 SampleToken to the Fee Aggregation system
     * Then:
     *      - UserA total deposit 200 ICX to the Fee Aggregation system for 10 SampleToken
     */
    @Test
    void scenario3Test() throws Exception {
        KeyWallet userA = wallets[1];
        KeyWallet userB = wallets[2];
        BigInteger depositAmount = ICX.multiply(BigInteger.valueOf(60));
        BigInteger tokenAmount = ICX.multiply(BigInteger.TEN);
        BigInteger currentUserADeposited = feeAggregationScore.getDepositAmount(sampleTokenScore.name(), userA.getAddress());
        BigInteger currentUserBDeposited = feeAggregationScore.getDepositAmount(sampleTokenScore.name(), userB.getAddress());

        // UserA bid 100 ICX for 10 SampleToken
        LOG.infoEntering("transact", "deposit() - UserA deposit 100 ICX to bid 10 " + sampleTokenScore.name());
        feeAggregationScore.ensureDepositIcxSuccess(txHandler, userA, sampleTokenScore.name(), depositAmount);
        // UserA remain less than (200 - 60) ICX because of transaction fee
        BigInteger balanceA = txHandler.getBalance(userA.getAddress());
        assertTrue(balanceA.compareTo(ICX.multiply(BigInteger.valueOf(140))) < 0);
        feeAggregationScore.ensureSyndicationInfo(userA, sampleTokenScore.name(), tokenAmount, depositAmount.add(currentUserADeposited).add(currentUserBDeposited));
        feeAggregationScore.ensureDepositAmount(sampleTokenScore.name(), depositAmount.add(currentUserADeposited), userA.getAddress());
        LOG.infoExiting();
    }

    /**
     * Scenario 4: User deposit for syndication that has been ended
     *
     * Given:
     *      - Current SampleToken amount of contract 30
     * When:
     *      - Current syndication has 10 SampleToken and two share holders
     *      - UserA has deposited 160 ICX
     *      - UserB has deposited 150 ICX
     *      - Current SampleToken ICX price = 100.000.000.000.000/1.000.000.000.000 = 1/100
     *      - SampleToken/ICX price with discount 10% = 1/90
     *      - Total price of SampleToken in this syndication 100*90 = 9000 ICX
     *      - UserC send the request to deposit 100 ICX to the Fee Aggregation system to SampleToken
     * Then:
     *      - Finished current syndication
     *      - Add share of SampleToken to UserA and UserB in state
     *      - UserA has 160/90 = 1.7777777777778 SampleToken
     *      - UserB has 150/110 = 1.666666666667 SampleToken
     *      - Create new syndication for SampleToken with token amount is (30 - 10)
     */
    @Test
    void scenario4Test() throws Exception {
        KeyWallet userA = wallets[1];
        KeyWallet userB = wallets[2];
        KeyWallet userC = wallets[3];
        BigInteger depositAmount = ICX.multiply(BigInteger.valueOf(100));
        BigInteger currentUserADeposited = feeAggregationScore.getDepositAmount(sampleTokenScore.name(), userA.getAddress());
        BigInteger currentUserBDeposited = feeAggregationScore.getDepositAmount(sampleTokenScore.name(), userB.getAddress());
        BigInteger currentSampleTokenIcxRate = bandProtocolScore.getReferenceData(sampleTokenScore.name(), "ICX");
        BigInteger currentSampleTokenIcxRateWithDiscount = currentSampleTokenIcxRate.divide(BigInteger.valueOf(100)).multiply(BigInteger.valueOf(90));

        BigInteger currentSyndicationEndTime = feeAggregationScore.getEndTime(sampleTokenScore.name());

        // prepare
        LOG.infoEntering("transfer token", "20 " + sampleTokenScore.name() + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer10TokenToFAS = sampleTokenScore.transfer(ownerWallet, feeAggregationScore.getAddress(), ICX.multiply(BigInteger.valueOf(20)));
        sampleTokenScore.ensureTransfer(resultOfTransfer10TokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), ICX.multiply(BigInteger.valueOf(20)), null);
        LOG.infoExiting();

        BigInteger userASampleTokenShare = currentUserADeposited.multiply(ICX).divide(currentSampleTokenIcxRateWithDiscount);
        BigInteger userBSampleTokenShare = currentUserBDeposited.multiply(ICX).divide(currentSampleTokenIcxRateWithDiscount);

        // wait until current syndication end
        while(currentSyndicationEndTime.longValue() > (new Date().getTime()*1000 - 3000000))
            TimeUnit.SECONDS.sleep(1);


        // UserA bid 100 ICX for 10 SampleToken
        LOG.infoEntering("transact", "deposit() - UserA deposit 100 ICX to bid 10 " + sampleTokenScore.name());
        feeAggregationScore.ensureDepositIcxSuccess(txHandler, userC, sampleTokenScore.name(), depositAmount);
        // UserA remain less than (300 - 100) ICX because of transaction fee
        BigInteger balanceC = txHandler.getBalance(userC.getAddress());
        assertTrue(balanceC.compareTo(ICX.multiply(BigInteger.valueOf(200))) < 0);
        feeAggregationScore.ensureSyndicationInfo(userC, sampleTokenScore.name(), ICX.multiply(BigInteger.valueOf(20)), depositAmount);
        feeAggregationScore.ensureDepositAmount(sampleTokenScore.name(), depositAmount, userC.getAddress());
        LOG.infoExiting();

        feeAggregationScore.ensureTokenBalance(sampleTokenScore.name(), userASampleTokenShare, userA.getAddress());
        feeAggregationScore.ensureTokenBalance(sampleTokenScore.name(), userBSampleTokenShare, userB.getAddress());
    }

    /**
     * Scenario 5: Distribute token for by percent of token that deposited if total deposited greater than token value
     *
     * Given:
     *      - Current SecondSampleToken amount of contract 100
     *      - ICX SecondSampleToken rate 1/10 == 10/100
     *      - SecondSampleToken/ICX price with discount 10% = 9/100
     *      - Total price of SecondSampleToken in this syndication 100*(9/100) = 9 ICX
     * When:
     *      - UserA has deposited 20 ICX
     *      - UserB has deposited 50 ICX
     *      - Total deposited 70 ICX
     *      - Transfer additional 200 SecondSampleToken to Fee Aggregation
     *      - UserC send the request to deposit 10 ICX to the Fee Aggregation system to SecondSampleToken
     * Then:
     *      - Finished current syndication of SecondSampleToken
     *      - Add share of SampleToken to UserA and UserB in state
     *      - UserA has (20/70)*100 = 28.5714285714 SampleMultiToken
     *      - UserB has (50/70)*100 = 71.4285714285 SampleMultiToken
     *      - Update ICX remaining for UserA and UserB
     *      - UserA remain (20 - 28.5714285700*(9/100)) = 17.42857143 ICX
     *      - UserB remain (50 - 71.4285714300*(9/100)) = 43.57142857 ICX
     *      - Create new syndication for SecondSampleToken with token amount is 200
     */
    @Test
    void scenario5Test() throws Exception {
        LOG.infoEntering("transact", "setDuration() - set duration to 10s ");
        feeAggregationScore.ensureSetDurationSuccess(ownerWallet, BigInteger.valueOf(1000*1000*10)); // 10s
        LOG.infoExiting();
        
        LOG.infoEntering("transact", "relay() - set rate of " + SampleMultiTokenScore.NAME + " to 1/100 ICX");
        bandProtocolScore.relay(ownerWallet, "[\"" + SampleMultiTokenScore.NAME + "\",\"ICX\",\"USDC\"]",  "[10000000000, 100000000000, 1000000000]", "[1633508543, 1633508543, 1633508543]", "[5684902, 5684904, 5684902]");
        LOG.infoExiting();

        KeyWallet userA = wallets[1];
        KeyWallet userB = wallets[2];
        KeyWallet userC = wallets[3];
        BigInteger userADeposited = ICX.multiply(BigInteger.valueOf(20));
        BigInteger userBDeposited = ICX.multiply(BigInteger.valueOf(50));
        BigInteger totalDeposited = userADeposited.add(userBDeposited);
        BigInteger tokenAmount = irc31TokenScore.unit().multiply(BigInteger.valueOf(100));
        BigInteger currentSampleMultiTokenScoreIcxRate = bandProtocolScore.getReferenceData(SampleMultiTokenScore.NAME, "ICX");

        // UserA bid 20 ICX for 100 SecondSampleToken
        LOG.infoEntering("transact", "deposit() - UserA deposit 20 ICX for 100 " + SampleMultiTokenScore.NAME);
        feeAggregationScore.ensureDepositIcxSuccess(txHandler, userA, SampleMultiTokenScore.NAME, userADeposited);
        feeAggregationScore.ensureSyndicationInfo(userA, SampleMultiTokenScore.NAME, tokenAmount, userADeposited);
        feeAggregationScore.ensureDepositAmount(SampleMultiTokenScore.NAME, userADeposited, userA.getAddress());

        // UserA bid 20 ICX for 100 SecondSampleToken
        LOG.infoEntering("transact", "deposit() - UserB deposit 50 ICX for 100 " + SampleMultiTokenScore.NAME);
        feeAggregationScore.ensureDepositIcxSuccess(txHandler, userB, SampleMultiTokenScore.NAME, userBDeposited);
        feeAggregationScore.ensureSyndicationInfo(userB, SampleMultiTokenScore.NAME, tokenAmount, totalDeposited);
        feeAggregationScore.ensureDepositAmount(SampleMultiTokenScore.NAME, userBDeposited, userB.getAddress());

        BigInteger currentSyndicationEndTime = feeAggregationScore.getEndTime(SampleMultiTokenScore.NAME);

        LOG.infoEntering("transfer multi-token", "200 " + SampleMultiTokenScore.NAME + " to FeeAggregationSCORE");
        TransactionResult resultOfTransfer100000MultiTokenToFAS = irc31TokenScore.transfer(ownerWallet, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, irc31TokenScore.unit().multiply(BigInteger.valueOf(200)), null);
        irc31TokenScore.ensureTransfer(resultOfTransfer100000MultiTokenToFAS, ownerWallet.getAddress(), feeAggregationScore.getAddress(), SampleMultiTokenScore.ID, irc31TokenScore.unit().multiply(BigInteger.valueOf(200)), null);
        LOG.infoExiting();

        BigInteger userASampleMultiTokenShare = new BigInteger("285714285714");
        BigInteger userBSampleMultiTokenShare = new BigInteger("714285714285");

        // wait until current syndication end
        while(currentSyndicationEndTime.longValue() > (new Date().getTime()*1000 - 3000000))
            TimeUnit.SECONDS.sleep(1);


        // UserA bid 100 ICX for 10 SampleToken
        LOG.infoEntering("transact", "deposit() - UserC deposit 10 ICX for 200 " + SampleMultiTokenScore.NAME);
        feeAggregationScore.ensureDepositIcxSuccess(txHandler, userC, SampleMultiTokenScore.NAME, ICX.multiply(BigInteger.valueOf(10)));
        feeAggregationScore.ensureSyndicationInfo(userC, SampleMultiTokenScore.NAME, irc31TokenScore.unit().multiply(BigInteger.valueOf(200)), ICX.multiply(BigInteger.valueOf(10)));
        feeAggregationScore.ensureDepositAmount(SampleMultiTokenScore.NAME, ICX.multiply(BigInteger.valueOf(10)), userC.getAddress());
        LOG.infoExiting();

        feeAggregationScore.ensureTokenBalance(SampleMultiTokenScore.NAME, userASampleMultiTokenShare, userA.getAddress());
        feeAggregationScore.ensureTokenBalance(SampleMultiTokenScore.NAME, userBSampleMultiTokenShare, userB.getAddress());
        feeAggregationScore.ensureRefundableBalance(new BigInteger("17428571428574000000"), userA.getAddress());
        feeAggregationScore.ensureRefundableBalance(new BigInteger("43571428571435000000"), userB.getAddress());
    }

    /**
     * Scenario 6: if User submits a deposit for SampleTokenA which Fee Aggregation system does not register
     *
     * Given:
     *      - Fee Aggregation System does not register SampleTokenA
     *      - UserD has 100 ICX
     * When:
     *      - UserD sends the request to deposit for SampleTokenA by 100 ICX to the Fee Aggregation system
     * Then:
     *      - Transaction Revert
     */
    @Test
    void scenario6Test() throws Exception {
        KeyWallet userD = wallets[4];
        String tokenNameNotRegisted = "SampleTokenA";

        // UserC bid 100 ICX for 10 SampleToken
        LOG.infoEntering("transact", "deposit() - UserC deposit 100 ICX to bid 10 " + tokenNameNotRegisted);
        feeAggregationScore.ensureDepositIcxFailed(txHandler, userD, tokenNameNotRegisted, ICX.multiply(BigInteger.valueOf(100)), 57);
        LOG.infoExiting("Transaction Revert");
    }

    /**
     * Scenario 7: if User submits a deposit for amount SampleTokenB equal 0 in the Fee Aggregation system
     *
     * Given:
     *      - SampleTokenB balance is 0 in Fee Aggregation System
     *      - UserD has 100 ICX
     * When:
     *      - UserD sends the request to deposit for SampleTokenB by 100 ICX to the Fee Aggregation system
     * Then:
     *      - Transaction Revert
     */
    @Test
    void scenario7Test() throws Exception {
        KeyWallet userD = wallets[4];
        String tokenNameB = "SampleTokenB";
        BigInteger tokenBId = BigInteger.valueOf(999999999);
        BigInteger tokenBDecimals = BigInteger.valueOf(8);
        LOG.infoEntering("call", "registerIRC2() - register " + tokenNameB);
        feeAggregationScore.ensureRegisterIRC31Success(ownerWallet, tokenNameB, irc31TokenScore.getAddress(), tokenBId, tokenBDecimals);
        LOG.infoExiting();

        // UserC bid 100 ICX for 0 SampleTokenB
        LOG.infoEntering("transact", "deposit() - UserC deposit 100 ICX to bid 0 " + tokenNameB);
        feeAggregationScore.ensureDepositIcxFailed(txHandler, userD, tokenNameB, ICX.multiply(BigInteger.valueOf(100)), 61);
        LOG.infoExiting("Transaction Revert");
    }

    /**
     * Scenario 8: if User withdraw their refundable ICX token
     *
     * Given:
     *      - UserA refundable balance 17428571428574000000 ICX
     * When:
     *      - UserA call Fee Aggregation to withdraw ICX token
     * Then:
     *      - Set UserA refundable balance to 0 ICX
     *      - Transfer 17428571428574000000 ICX to UserA
     */
    @Test
    void scenario8Test() throws Exception {
        KeyWallet userA = wallets[1];
        BigInteger currentFaIcxBalance = FeeAggregationSCORETest.iconService.getBalance(feeAggregationScore.getAddress()).execute();
        BigInteger currentUserAIcxBalance = FeeAggregationSCORETest.iconService.getBalance(userA.getAddress()).execute();
        BigInteger currentUserARefundableBalance = feeAggregationScore.getRefundableBalance(userA.getAddress());
        LOG.infoEntering("call", "withdrawal() " + userA.getAddress().toString());
        TransactionResult withdrawTransactionResult = feeAggregationScore.ensureWithdrawSuccess(userA);
        LOG.infoExiting();

        BigInteger withdrawTransactionFeeInIcx = withdrawTransactionResult.getStepPrice().multiply(withdrawTransactionResult.getStepUsed());

        BigInteger userAIcxBalanceAferWithdrew = FeeAggregationSCORETest.iconService.getBalance(userA.getAddress()).execute();
        BigInteger userARefundableBalanceAfterWithdrew = feeAggregationScore.getRefundableBalance(userA.getAddress());
        BigInteger faIcxBalanceAfterWithdrew = FeeAggregationSCORETest.iconService.getBalance(feeAggregationScore.getAddress()).execute();

        assertTrue(userAIcxBalanceAferWithdrew.equals(currentUserAIcxBalance.add(currentUserARefundableBalance).subtract(withdrawTransactionFeeInIcx)));
        assertTrue(userARefundableBalanceAfterWithdrew.equals(BigInteger.ZERO));
        assertTrue(faIcxBalanceAfterWithdrew.equals(currentFaIcxBalance.subtract(currentUserARefundableBalance)));
    }

    /**
     * Scenario 9: if User withdraw their refundable ICX token with avaialable refundable token is ZERO
     *
     * Given:
     *      - UserA refundable balance 0 ICX
     * When:
     *      - UserA call Fee Aggregation to withdraw ICX token
     * Then:
     *      - Transaction revert NOT_FOUND_BALANCE code 62
     */
    @Test
    void scenario9Test() throws Exception {
        KeyWallet userA = wallets[1];

        BigInteger currentFaIcxBalance = FeeAggregationSCORETest.iconService.getBalance(feeAggregationScore.getAddress()).execute();
        BigInteger currentUserAIcxBalance = FeeAggregationSCORETest.iconService.getBalance(userA.getAddress()).execute();
        BigInteger currentUserARefundableBalance = feeAggregationScore.getRefundableBalance(userA.getAddress());
        assertTrue(currentUserARefundableBalance.equals(BigInteger.ZERO));

        LOG.infoEntering("call", "withdrawal() " + userA.getAddress().toString());
        TransactionResult withdrawTransactionResult = feeAggregationScore.ensureWithdrawFailed(userA, 62);
        LOG.infoExiting();

        BigInteger withdrawTransactionFeeInIcx = withdrawTransactionResult.getStepPrice().multiply(withdrawTransactionResult.getStepUsed());

        BigInteger userAIcxBalanceAferWithdrew = FeeAggregationSCORETest.iconService.getBalance(userA.getAddress()).execute();
        BigInteger faIcxBalanceAfterWithdrew = FeeAggregationSCORETest.iconService.getBalance(feeAggregationScore.getAddress()).execute();

        assertTrue(userAIcxBalanceAferWithdrew.equals(currentUserAIcxBalance.subtract(withdrawTransactionFeeInIcx)));
        assertTrue(faIcxBalanceAfterWithdrew.equals(currentFaIcxBalance));
    }

    /**
     * Scenario 10: if User withdraw their token that not exist
     *
     * Given:
     *      - "SampleTokenA" not exist in FA;
     * When:
     *      - UserA call Fee Aggregation to claim SampleTokenA token
     * Then:
     *      - Transaction revert INVALID_TOKEN_NAME code 57
     */
    @Test
    void scenario10Test() throws Exception {
        KeyWallet userA = wallets[1];

        String claimmingToken = "SampleTokenA";

        LOG.infoEntering("call", "claim() token: " + claimmingToken + "  user: "+ userA.getAddress().toString());
        feeAggregationScore.ensureClaimFailed(userA, claimmingToken, 57);
    }

    /**
     * Scenario 11: if User withdraw their token that they has no balance
     *
     * Given:
     *      - UserC has no balance of token "SampleToken";
     * When:
     *      - UserC call Fee Aggregation to claim SampleToken token
     * Then:
     *      - Transaction revert NOT_FOUND_BALANCE code 62
     */
    @Test
    void scenario11Test() throws Exception {
        KeyWallet userC = wallets[3];

        String claimmingToken = sampleTokenScore.name();
        BigInteger currentFaSampleTokenBalance = sampleTokenScore.balanceOf(feeAggregationScore.getAddress());

        LOG.infoEntering("call", "claim() token: " + claimmingToken + "  user: "+ userC.getAddress().toString());
        feeAggregationScore.ensureClaimFailed(userC, claimmingToken, 62);

        BigInteger faSampleTokenBalanceAfterClaimed = sampleTokenScore.balanceOf(feeAggregationScore.getAddress());
        assertTrue(faSampleTokenBalanceAfterClaimed.equals(currentFaSampleTokenBalance));
    }

    /**
     * Scenario 12: if User withdraw their token successfully
     *
     * Given:
     *      - UserA has 28.5714285714 SampleMultiToken
     * When:
     *      - UserA call Fee Aggregation to claim SampleMultiToken token
     * Then:
     *      - Transfer 28.5714285714 SampleMultiToken for userA
     *      - Set SampleMultiToken balance of userA to ZERO
     *      - Update lockedBalance of SampleMultiToken of FA
     */
    @Test
    void scenario12Test() throws Exception {
        KeyWallet userA = wallets[1];

        String claimmingToken = SampleMultiTokenScore.NAME;
        BigInteger currentFaSampleTokenBalance = irc31TokenScore.balanceOf(feeAggregationScore.getAddress(), SampleMultiTokenScore.ID);
        BigInteger currentUserASampleTokenBalance = irc31TokenScore.balanceOf(userA.getAddress(), SampleMultiTokenScore.ID);
        BigInteger currentUserASampleTokenClaimableBalance = feeAggregationScore.getTokenBalance(userA.getAddress(), claimmingToken);

        LOG.infoEntering("call", "claim() token: " + claimmingToken + "  user: "+ userA.getAddress().toString());
        feeAggregationScore.ensureClaimSuccess(userA, claimmingToken);

        BigInteger faSampleTokenBalanceAfterClaimed = irc31TokenScore.balanceOf(feeAggregationScore.getAddress(), SampleMultiTokenScore.ID);
        BigInteger userASampleTokenBalanceAfterClaimed = irc31TokenScore.balanceOf(userA.getAddress(), SampleMultiTokenScore.ID);
        BigInteger userASampleTokenClaimableBalanceAfterClaimed = feeAggregationScore.getTokenBalance(userA.getAddress(), claimmingToken);
        assertTrue(faSampleTokenBalanceAfterClaimed.equals(currentFaSampleTokenBalance.subtract(currentUserASampleTokenClaimableBalance)));
        assertTrue(userASampleTokenBalanceAfterClaimed.equals(currentUserASampleTokenBalance.add(currentUserASampleTokenClaimableBalance)));
        assertTrue(userASampleTokenClaimableBalanceAfterClaimed.equals(BigInteger.ZERO));
    }
}
