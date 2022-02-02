package foundation.icon.btp.score;

import foundation.icon.btp.*;
import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static foundation.icon.btp.Env.LOG;

public class FeeAggregationScore extends Score {

    public static FeeAggregationScore mustDeploy(TransactionHandler txHandler, Wallet wallet, Address bandProtocolAddress)
            throws ResultTimeoutException, TransactionFailureException, IOException, AssertionError {
        // Deploy CPS
        Score cps = txHandler.deploy(wallet, "CPFTreasury.zip", null);

        LOG.infoEntering("deploy", "Fee Aggregation SCORE");
        RpcObject params = new RpcObject.Builder()
                .put("_cps_address", new RpcValue(cps.getAddress()))
                .put("_band_protocol_address", new RpcValue(bandProtocolAddress))
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("fee-aggregation-system"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new FeeAggregationScore(score);
    }

    public FeeAggregationScore(Score other) {
        super(other);
    }

    public void ensureRegisterIRC2Success(Wallet wallet, String tokenName, Address tokenAddress) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .put("_tokenAddress", new RpcValue(tokenAddress))
                .build();

        invokeAndWaitResult(wallet, "registerIRC2", params);

        List<RpcItem> tokens = call("tokens", null).asArray().asList();
        for (RpcItem token : tokens) {
            if (token.asObject().getItem("name").asString().equals(tokenName)) {
                return;
            }
        }
        throw new IOException("ensureRegisterSuccess failed.");
    }

    public void ensureRegisterIRC31Success(Wallet wallet, String tokenName, Address tokenAddress, BigInteger id, BigInteger decimal) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .put("_tokenAddress", new RpcValue(tokenAddress))
                .put("_tokenId", new RpcValue(id))
                .put("_decimal", new RpcValue(decimal))
                .build();

        invokeAndWaitResult(wallet, "registerIRC31", params);

        List<RpcItem> tokens = call("tokens", null).asArray().asList();
        for (RpcItem token : tokens) {
            if (token.asObject().getItem("name").asString().equals(tokenName)) {
                return;
            }
        }
        throw new IOException("ensureRegisterIRC2Success failed.");
    }

    public void ensureDepositIcxSuccess(TransactionHandler txHandler, Wallet wallet, String tokenName, BigInteger amount) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .build();

        TransactionResult result = invokeAndWaitResult(wallet, "deposit", params, amount, null);
        if (!Constants.STATUS_SUCCESS.equals(result.getStatus())) {
            throw new TransactionFailureException(result.getFailure());
        }
        if (findEventLog(result, getAddress(), "SyndicationStart(int,str,int,Address,int,int)") == null && findEventLog(result, getAddress(), "DepositInfo(int,str,Address,int)") == null) {
            throw new TransactionFailureException(result.getFailure());
        }
    }

    public void ensureDepositIcxFailed(TransactionHandler txHandler, Wallet wallet, String tokenName, BigInteger amount, int errorCode) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .build();

        TransactionResult result = invokeAndWaitResult(wallet, "deposit", params, amount, null);
        if (Constants.STATUS_SUCCESS.equals(result.getStatus())) {
            throw new AssertionError("Expect to fail but success");
        }

        if (!result.getFailure().getMessage().equals("Reverted(" + errorCode + ")")) {
            throw new AssertionError("Expect error code to be " + errorCode + " but receive " + result.getFailure().getMessage());
        }
    }

    public void ensureDepositAmount(String tokenName, BigInteger amount, Address depositor) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .put("_depositor", new RpcValue(depositor))
                .build();
        BigInteger depositAmount = call("getDepositedAmount", params).asInteger();
        if (depositAmount.compareTo(amount) != 0) {
            throw new AssertionError("Deposit amount are not equal to expected");
        }
    }

    public void ensureTokenBalance(String tokenName, BigInteger expected, Address address) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .put("_address", new RpcValue(address))
                .build();
        BigInteger tokenBalance = call("getTokenBalance", params).asInteger();
        if (tokenBalance.compareTo(expected) != 0) {
            throw new AssertionError("Token balance of " + address.toString() + " expected: " + expected.toString() + " but received: " + tokenBalance.toString());
        }
    }

    public void ensureRefundableBalance(BigInteger expected, Address address) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_address", new RpcValue(address))
                .build();
        BigInteger tokenBalance = call("getRefundableBalance", params).asInteger();
        if (tokenBalance.compareTo(expected) != 0) {
            throw new AssertionError("Refundable balance of " + address.toString() + " expected: " + expected.toString() + " but received: " + tokenBalance.toString());
        }
    }

    public void ensureSyndicationInfo(Wallet wallet, String tokenName, BigInteger tokenAmount, BigInteger totalDeposited) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .build();

        RpcObject syndication = call("getCurrentSyndication", params).asObject();
        if (!syndication.getItem("_totalDeposited").asInteger().equals(totalDeposited)) {
            throw new IOException("ensureAuctionInfo failed due to not equal totalDeposited.");
        }

        if (!syndication.getItem("_tokenAmount").asInteger().equals(tokenAmount)) {
            throw new IOException("ensureAuctionInfo failed due to not equal tokenAmount.");
        }

        return;
    }

    public void ensureSetDurationSuccess(Wallet wallet, BigInteger duration) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_duration", new RpcValue(duration))
                .build();

        TransactionResult result = invokeAndWaitResult(wallet, "setDurationTime", params, null);
        if (!result.getStatus().equals(BigInteger.ONE)) {
            throw new IOException("ensureSetDurationSuccess failed");
        };

        RpcItem durationTime = call("getDurationTime", new RpcObject.Builder().build());
        if (!durationTime.asInteger().equals(duration)) {
            throw new IOException("State data duration time not changed");
        };
    }

    public TransactionResult ensureWithdrawSuccess(Wallet wallet) throws Exception {
        RpcObject params = new RpcObject.Builder().build();

        TransactionResult result = invokeAndWaitResult(wallet, "withdrawal", params, null);
        if (!result.getStatus().equals(BigInteger.ONE)) {
            throw new IOException("ensureWithdrawSuccess failed");
        };
        return result;
    }

    public TransactionResult ensureWithdrawFailed(Wallet wallet, int errorCode) throws Exception {
        RpcObject params = new RpcObject.Builder().build();

        TransactionResult result = invokeAndWaitResult(wallet, "withdrawal", params, null);
        if (Constants.STATUS_SUCCESS.equals(result.getStatus())) {
            throw new AssertionError("Expect to fail but success");
        }

        if (!result.getFailure().getMessage().equals("Reverted(" + errorCode + ")")) {
            throw new AssertionError("Expect error code to be " + errorCode + " but receive " + result.getFailure().getMessage());
        }

        return result;
    }

    public TransactionResult ensureClaimFailed(Wallet wallet, String tokenName, int errorCode) throws Exception {
        RpcObject params = new RpcObject.Builder().put("_tokenName", new RpcValue(tokenName)).build();

        TransactionResult result = invokeAndWaitResult(wallet, "claim", params, null);
        if (Constants.STATUS_SUCCESS.equals(result.getStatus())) {
            throw new AssertionError("Expect to fail but success");
        }

        if (!result.getFailure().getMessage().equals("Reverted(" + errorCode + ")")) {
            throw new AssertionError("Expect error code to be " + errorCode + " but receive " + result.getFailure().getMessage());
        }

        return result;
    }

    public TransactionResult ensureClaimSuccess(Wallet wallet, String tokenName) throws Exception {
        RpcObject params = new RpcObject.Builder().put("_tokenName", new RpcValue(tokenName)).build();

        TransactionResult result = invokeAndWaitResult(wallet, "claim", params, null);
        if (!result.getStatus().equals(BigInteger.ONE)) {
            throw new IOException("ensureClaimSuccess failed");
        };
        return result;
    }

    public BigInteger getDepositAmount(String tokenName, Address depositor) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .put("_depositor", new RpcValue(depositor))
                .build();
        return call("getDepositedAmount", params).asInteger();
    }

    public BigInteger getRefundableBalance(Address address) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_address", new RpcValue(address))
                .build();
        return call("getRefundableBalance", params).asInteger();
    }

    public BigInteger getTokenBalance(Address address, String tokenName) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_address", new RpcValue(address))
                .put("_tokenName", new RpcValue(tokenName))
                .build();
        return call("getTokenBalance", params).asInteger();
    }

    public BigInteger getTotalDeposited(String tokenName) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .build();

        RpcItem syndication = call("getCurrentSyndication", params);
        return syndication.asObject().getItem("_totalDeposited").asInteger();
    }

    public BigInteger getEndTime(String tokenName) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .build();

        RpcObject syndication = call("getCurrentSyndication", params).asObject();
        return syndication.getItem("_endTime").asInteger();
    }
}
