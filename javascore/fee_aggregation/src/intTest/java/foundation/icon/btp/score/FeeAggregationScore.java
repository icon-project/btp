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

    public static FeeAggregationScore mustDeploy(TransactionHandler txHandler, Wallet wallet)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        // Deploy CPS
        Score cps = txHandler.deploy(wallet, "CPFTreasury.zip", null);

        LOG.infoEntering("deploy", "Fee Aggregation SCORE");
        RpcObject params = new RpcObject.Builder()
                .put("_cps_address", new RpcValue(cps.getAddress()))
                .build();
        Score score = txHandler.deploy(wallet, getFilePath("fee-aggregation-system"), params);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();
        return new FeeAggregationScore(score);
    }

    public FeeAggregationScore(Score other) {
        super(other);
    }

    public TransactionResult checkGoalReached(Wallet wallet)
            throws ResultTimeoutException, IOException {
        return invokeAndWaitResult(wallet, "checkGoalReached", null, null);
    }

    public TransactionResult safeWithdrawal(Wallet wallet)
            throws ResultTimeoutException, IOException {
        return invokeAndWaitResult(wallet, "safeWithdrawal", null, null);
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

    public void ensureRegisterIRC31Success(Wallet wallet, String tokenName, Address tokenAddress, BigInteger id) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .put("_tokenAddress", new RpcValue(tokenAddress))
                .put("_tokenId", new RpcValue(id))
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

    public void ensureBidSuccess(TransactionHandler txHandler, Wallet wallet, String tokenName, BigInteger amount) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .build();

        TransactionResult result = invokeAndWaitResult(wallet, "bid", params, amount, null);
        if (!Constants.STATUS_SUCCESS.equals(result.getStatus())) {
            throw new TransactionFailureException(result.getFailure());
        }
        if (findEventLog(result, getAddress(), "AuctionStart(int,str,Address,int,int)") == null && findEventLog(result, getAddress(), "BidInfo(int,str,Address,int,Address,int)") == null) {
            throw new TransactionFailureException(result.getFailure());
        }
    }

    public void ensureFundTransfer(TransactionResult result, Address backer, BigInteger amount)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "FundTransfer(Address,int,bool)");
        if (event != null) {
            Address _backer = event.getIndexed().get(1).asAddress();
            BigInteger _amount = event.getIndexed().get(2).asInteger();
            Boolean isContribution = event.getIndexed().get(3).asBoolean();
            if (backer.equals(_backer) && amount.equals(_amount) && !isContribution) {
                return; // ensured
            }
        }
        throw new IOException("ensureFundTransfer failed.");
    }

    public void ensureAuctionInfo(Wallet wallet, String tokenName, BigInteger bidAmount, Address bidder) throws Exception {
        RpcObject params = new RpcObject.Builder()
                .put("_tokenName", new RpcValue(tokenName))
                .build();

        RpcObject auction = call("getCurrentAuction", params).asObject();
        if (!auction.getItem("_bidAmount").asInteger().equals(bidAmount)) {
            throw new IOException("ensureAuctionInfo failed due to not equal bidAmount.");
        }

        if (!auction.getItem("_bidder").asAddress().equals(bidder)) {
            throw new IOException("ensureAuctionInfo failed due to not equal bidder.");
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
    }
}
