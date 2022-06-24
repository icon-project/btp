/*
 * Copyright 2022 ICONLOOP Inc.
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

package foundation.icon.btp.bmv.btp;

import foundation.icon.btp.bmv.btp.score.ChainScore;
import foundation.icon.btp.bmv.btp.score.Score;
import foundation.icon.icx.*;
import foundation.icon.icx.data.*;
import foundation.icon.icx.transport.jsonrpc.RpcError;
import foundation.icon.icx.transport.jsonrpc.RpcItem;
import foundation.icon.icx.transport.jsonrpc.RpcObject;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TransactionHandler {
    private final IconService iconService;
    private final Env.Chain chain;

    public TransactionHandler(IconService iconService, Env.Chain chain) {
        this.iconService = iconService;
        this.chain = chain;
    }

    public Score deploy(Wallet owner, String scorePath, RpcObject params)
            throws IOException, ResultTimeoutException, TransactionFailureException {
        return deploy(owner, scorePath, params, null);
    }

    public Score deploy(Wallet owner, String scorePath, RpcObject params, BigInteger steps)
            throws IOException, ResultTimeoutException, TransactionFailureException {
        return deploy(owner, scorePath, Constants.ZERO_ADDRESS, params, steps);
    }

    public Score deploy(Wallet owner, String scorePath, Address to, RpcObject params, BigInteger steps)
            throws IOException, ResultTimeoutException, TransactionFailureException {
        byte[] data = Files.readAllBytes(Path.of(scorePath));
        if (scorePath.endsWith(".jar")) {
            return getScore(doDeploy(owner, data, to, params, steps, Constants.CONTENT_TYPE_JAVA));
        } else {
//            byte[] data = ZipFile.zipContent(scorePath);
            return getScore(doDeploy(owner, data, to, params, steps, Constants.CONTENT_TYPE_PYTHON));
        }
    }

    private Bytes doDeploy(Wallet owner, byte[] content, Address to, RpcObject params,
                           BigInteger steps, String contentType) throws IOException {
        Transaction transaction = TransactionBuilder.newBuilder()
                .nid(getNetworkId())
                .from(owner.getAddress())
                .to(to)
                .deploy(contentType, content)
                .params(params)
                .build();
        if (steps == null) {
            steps = estimateStep(transaction);
        }
        SignedTransaction signedTransaction = new SignedTransaction(transaction, owner, steps);
        return iconService.sendTransaction(signedTransaction).execute();
    }

    public Score getScore(Bytes txHash)
            throws IOException, ResultTimeoutException, TransactionFailureException {
        TransactionResult result = getResult(txHash, Constants.DEFAULT_WAITING_TIME);
        if (!Constants.STATUS_SUCCESS.equals(result.getStatus())) {
            throw new TransactionFailureException(result.getFailure());
        }
        return new Score(this, new Address(result.getScoreAddress()));
    }

    public Env.Chain getChain() {
        return this.chain;
    }

    public BigInteger getNetworkId() {
        return BigInteger.valueOf(chain.networkId);
    }

    public BigInteger getBalance(Address address) throws IOException {
        return iconService.getBalance(address).execute();
    }

    public List<ScoreApi> getScoreApi(Address scoreAddress) throws IOException {
        return iconService.getScoreApi(scoreAddress).execute();
    }

    public BigInteger estimateStep(Transaction transaction) throws IOException {
        try {
            return iconService.estimateStep(transaction).execute();
        } catch (RpcError e) {
            Env.LOG.info("estimateStep failed(" + e.getCode() + ", " + e.getMessage() + "); use default steps.");
            return (Constants.DEFAULT_STEPS.multiply(BigInteger.TWO)).add(BigInteger.valueOf(10000));
        }
    }

    public RpcItem call(Call<RpcItem> call) throws IOException {
        return this.iconService.call(call).execute();
    }

    public Bytes invoke(Wallet wallet, Transaction tx, BigInteger steps) throws IOException {
        if (steps == null) {
            steps = estimateStep(tx);
        }
        return this.iconService.sendTransaction(new SignedTransaction(tx, wallet, steps)).execute();
    }

    public TransactionResult getResult(Bytes txHash)
            throws IOException, ResultTimeoutException {
        return getResult(txHash, Constants.DEFAULT_WAITING_TIME);
    }

    public TransactionResult getResult(Bytes txHash, long waiting)
            throws IOException, ResultTimeoutException {
        long limitTime = System.currentTimeMillis() + waiting;
        while (true) {
            try {
                return iconService.getTransactionResult(txHash).execute();
            } catch (RpcError e) {
                if (e.getCode() == -31002 /* pending */
                        || e.getCode() == -31003 /* executing */
                        || e.getCode() == -31004 /* not found */) {
                    if (limitTime < System.currentTimeMillis()) {
                        throw new ResultTimeoutException(txHash);
                    }
                    try {
                        // wait until block confirmation
                        Env.LOG.debug("RpcError: code(" + e.getCode() + ") message(" + e.getMessage() + "); Retry in 1 sec.");
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    continue;
                }
                Env.LOG.warning("RpcError: code(" + e.getCode() + ") message(" + e.getMessage() + "); Retry in 1 sec.");
                throw e;
            }
        }
    }

    public Bytes transfer(Address to, BigInteger amount) throws IOException {
        return transfer(chain.godWallet, to, amount);
    }

    public Bytes transfer(Wallet owner, Address to, BigInteger amount) throws IOException {
        return transfer(owner, to, amount, Constants.DEFAULT_STEPS.multiply(BigInteger.TWO));
    }

    public Bytes transfer(Wallet owner, Address to, BigInteger amount, BigInteger steps) throws IOException {
        Transaction transaction = TransactionBuilder.newBuilder()
                .nid(getNetworkId())
                .from(owner.getAddress())
                .to(to)
                .value(amount)
                .build();
        if (steps == null) {
            steps = estimateStep(transaction).add(BigInteger.valueOf(10000));
        }
        SignedTransaction signedTransaction = new SignedTransaction(transaction, owner, steps);
        return iconService.sendTransaction(signedTransaction).execute();
    }

    public void refundAll(Wallet owner) throws IOException {
        BigInteger stepPrice = new ChainScore(this).getStepPrice();
        BigInteger remaining = getBalance(owner.getAddress());
        BigInteger fee = Constants.DEFAULT_STEPS.multiply(stepPrice);
        transfer(owner, chain.godWallet.getAddress(), remaining.subtract(fee), Constants.DEFAULT_STEPS);
    }

    public ConfirmedTransaction getTransaction(Bytes txHash) throws IOException {
        return iconService.getTransaction(txHash).execute();
    }
}
