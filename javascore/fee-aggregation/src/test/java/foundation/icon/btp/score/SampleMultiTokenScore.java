package foundation.icon.btp.score;

import foundation.icon.icx.Wallet;
import foundation.icon.icx.data.Address;
import foundation.icon.icx.data.TransactionResult;
import foundation.icon.icx.transport.jsonrpc.RpcObject;
import foundation.icon.icx.transport.jsonrpc.RpcValue;
import foundation.icon.test.Log;
import foundation.icon.test.ResultTimeoutException;
import foundation.icon.test.TransactionFailureException;
import foundation.icon.test.TransactionHandler;
import foundation.icon.test.score.Score;

import java.io.IOException;
import java.math.BigInteger;

public class SampleMultiTokenScore extends Score {
    public final static BigInteger ID = BigInteger.valueOf(100);
    public final static String NAME = "MyIRC31SampleToken";
    private static final Log LOG = Log.getGlobal();
    private BigInteger decimals;

    public static SampleMultiTokenScore mustDeploy(TransactionHandler txHandler, Wallet wallet, BigInteger decimals)
            throws ResultTimeoutException, TransactionFailureException, IOException {
        LOG.infoEntering("deploy", "SampleMultiToken");
        Score score = txHandler.deploy(wallet, "SampleMultiToken", null);
        LOG.info("scoreAddr = " + score.getAddress());
        LOG.infoExiting();

        return new SampleMultiTokenScore(score, decimals);
    }

    public SampleMultiTokenScore(Score other, BigInteger decimals) {
        super(other);
        this.decimals = decimals;
    }

    public BigInteger unit() {
        return BigInteger.TEN.pow(this.decimals.intValue());
    }

    public BigInteger balanceOf(Address owner, BigInteger id) throws IOException {
        RpcObject params = new RpcObject.Builder()
                .put("_owner", new RpcValue(owner))
                .put("_id", new RpcValue(id))
                .build();
        return call("balanceOf", params).asInteger();
    }

    public void mintToken(Wallet wallet) throws Exception {
        // mint a Token
        RpcObject params = new RpcObject.Builder()
                .put("_id", new RpcValue(ID))
                .put("_supply", new RpcValue(BigInteger.TEN.pow(10).multiply(BigInteger.valueOf(10000))))
                .put("_uri", new RpcValue("https://example.com"))
                .build();

        invokeAndWaitResult(wallet, "mint", params);
    }

    public TransactionResult transfer(Wallet wallet, Address from, Address to, BigInteger id, BigInteger value, byte[] data)
            throws IOException, ResultTimeoutException {
        RpcObject.Builder builder = new RpcObject.Builder()
                .put("_from", new RpcValue(from))
                .put("_to", new RpcValue(to))
                .put("_id", new RpcValue(id))
                .put("_value", new RpcValue(value));
        if (data != null) {
            builder.put("_data", new RpcValue(data));
        }
        return this.invokeAndWaitResult(wallet, "transferFrom", builder.build());
    }

    public void ensureTransfer(TransactionResult result, Address from, Address to, BigInteger id, BigInteger value, byte[] data)
            throws IOException {
        TransactionResult.EventLog event = findEventLog(result, getAddress(), "TransferSingle(Address,Address,Address,int,int)");
        if (event != null) {
            Address _from = event.getIndexed().get(2).asAddress();
            Address _to = event.getIndexed().get(3).asAddress();
            BigInteger _id = event.getData().get(0).asInteger();
            BigInteger _value = event.getData().get(1).asInteger();
            if (from.equals(_from) && to.equals(_to) && id.equals(_id) && value.equals(_value)) {
                return; // ensured
            }
        }
        throw new IOException("ensureTransfer failed.");
    }
}
