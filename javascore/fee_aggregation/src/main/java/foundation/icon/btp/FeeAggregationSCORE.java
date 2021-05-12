package foundation.icon.btp;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class FeeAggregationSCORE {
    private static final BigInteger ONE_ICX = new BigInteger("1000000000000000000");
    private static final VarDB<BigInteger> minimumBidAmount = Context.newVarDB("minimumBidAmount", BigInteger.class);
    private static final VarDB<BigInteger> minimumIncrementalBidPercent = Context.newVarDB("minimumIncrementalBidPercent", BigInteger.class);
    public static final VarDB<BigInteger> auctionCount = Context.newVarDB("auctionCount", BigInteger.class);
    public static final VarDB<Long> durationTime = Context.newVarDB("durationTime", Long.class);

    /**
     * Address of the Contribution Proposal System.
     */
    private final VarDB<Address> addressCPS = Context.newVarDB("addressCPS", Address.class);

    /**
     * List of token contract, which system accepted.
     *
     * Example: {
     *      "IRC2Token1": {
     *          "address": "0x213123123123123",
     *          "tokenId": 1
     *      }
     * }
     */
    private final DictDB<String, Token> tokensScore;
    private final ArrayDB<String> supportedTokens;

    /**
     * Auction Information by tokenName
     */
    private final DictDB<String, Auction> auctions;

    /**
     * Lock balance not available for auction;
     * - Key: tokenName
     * - Value: amount of tokens
     */
    private final DictDB<String, BigInteger> lockedBalances;

    /**
     * Balance token of winner
     * - Key: tokenName
     */
    private final BranchDB<Address, DictDB<String, BigInteger>> tokenBalances;

    /**
     * Amount to refund
     */
    private final DictDB<Address, BigInteger> refundableBalances;

    public FeeAggregationSCORE(Address _cps_address) {
        this.addressCPS.set(_cps_address);
        this.tokensScore = Context.newDictDB("tokensScore", Token.class);
        this.supportedTokens = Context.newArrayDB("supportedTokens", String.class);
        this.auctions = Context.newDictDB("auctions", Auction.class);
        this.tokenBalances = Context.newBranchDB("tokenBalances", BigInteger.class);
        this.refundableBalances = Context.newDictDB("refundableBalances", BigInteger.class);
        this.lockedBalances = Context.newDictDB("lockedBalances", BigInteger.class);
        durationTime.set(1000*60*60*12L);
        auctionCount.set(BigInteger.ZERO);
        minimumBidAmount.set(BigInteger.valueOf(100L).multiply(ONE_ICX));
        minimumIncrementalBidPercent.set(BigInteger.valueOf(10L));
    };

    private Token getSafeTokenScore(String _tokenName) {
        return this.tokensScore.getOrDefault(_tokenName, null);
    }

    private BigInteger getSafeLockBalance(String _tokenName) {
        return this.lockedBalances.getOrDefault(_tokenName, BigInteger.ZERO);
    }

    private Auction getSafeAuction(String _tokenName) {
        return this.auctions.getOrDefault(_tokenName, null);
    }

    private BigInteger getSafeRefundableBalance(Address _address) {
        return this.refundableBalances.getOrDefault(_address, BigInteger.ZERO);
    }

    private BigInteger getSafeTokenBalance(Address _address, String _tokenName) {
        return this.tokenBalances.at(_address).getOrDefault(_tokenName, BigInteger.ZERO);
    }

    private BigInteger getTokenBalance(Token _token, Address _owner) {
        if (_token.isIRC31()) {
            return (BigInteger) Context.call(_token.address(), "balanceOf", _owner, _token.tokenId());
        } else {
            return (BigInteger) Context.call(_token.address(), "balanceOf", _owner);
        }
    }

    @External
    public void register(String _tokenName, Address _tokenAddress) {
        Context.require(Context.getCaller().equals(Context.getOwner()));
        Context.require(getSafeTokenScore(_tokenName) == null);
        Context.require(_tokenAddress.isContract());

        this.tokensScore.set(_tokenName, new Token(_tokenName, _tokenAddress));
        this.supportedTokens.add(_tokenName);
    }

    @External
    public void registerIRC31(String _tokenName, Address _tokenAddress, BigInteger _tokenId) {
        Context.require(Context.getCaller().equals(Context.getOwner()));
        Context.require(getSafeTokenScore(_tokenName) == null);
        Context.require(_tokenAddress.isContract());
        Context.require(_tokenId.compareTo(BigInteger.ZERO) > 0);

        this.tokensScore.set(_tokenName, new Token(_tokenName, _tokenAddress, _tokenId));
        this.supportedTokens.add(_tokenName);
    }

    /**
     *  List of tokens which defined in supportedTokens and tokensScore
     *
     * @return List of Token
     */
    @External(readonly = true)
    public List<Map<String, String>> tokens() {
        int len = this.supportedTokens.size();
        Map<String, String>[] tokens = new Map[len];

        for (int i = 0; i < len; i++) {
            tokens[i] = (this.tokensScore.get(this.supportedTokens.get(i)).toMap());
        }

        return List.of(tokens);
    }

    /**
     * Get auction information.
     *
     * @param _tokenName    - The name of token which defined in supportedTokens and tokensScore
     * @return Auction
     */
    @External(readonly = true)
    public Map<String, String> getCurrentAuction(String _tokenName) {
        Context.require(getSafeTokenScore(_tokenName) != null);
        return getSafeAuction(_tokenName).toMap();
    }

    /**
     * Pure transfer ICX
     */
    @Payable
    public void fallback() {
        // TODO: used for unit test, will be removed
        Address _from = Context.getCaller();
        BigInteger _value = Context.getValue();
        Context.require(_value.compareTo(BigInteger.ZERO) > 0);
    }

    // TODO: used for receive token from IRC2 contract, will be removed
    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) {
    }

    /**
     * Called when a user submit a bid on asset. This function will:
     *  - Hold ICX of user
     *  - Add bid of user
     *
     * @param _tokenName     - The name of token which defined in supportedTokens and tokensScore
     */
    @External
    @Payable
    public void bid(String _tokenName) {
        Token token = getSafeTokenScore(_tokenName);
        Context.require(token != null);
        BigInteger balance = getTokenBalance(token, Context.getAddress());
        Context.require(!balance.equals(BigInteger.ZERO));

        BigInteger value = Context.getValue();

        // Check if minimum value is 100 ICX
        Context.require(value.compareTo(minimumBidAmount.get()) > -1);

        long now = Context.getTransactionTimestamp();
        Auction auction = getSafeAuction(_tokenName);
        Address caller = Context.getCaller();

        // Check if auction is not start yet
        if (auction == null) {
            // Check available balance of token enough to start an auction
            BigInteger availableBalance = balance.subtract(getSafeLockBalance(_tokenName));
            Context.require(availableBalance.compareTo(BigInteger.ZERO) > -1);

            auction = new Auction(availableBalance, caller, value, now);
            this.auctions.set(_tokenName, auction);

            // Event log
            AuctionStart(caller, auction.id(), _tokenName, value, auction.endTime());
        } else {
            // Check auction is ended
            if (isAuctionExpired(_tokenName)) {
                endAuction(token, auction);

                // Bid for new auction
                balance = getTokenBalance(token, Context.getAddress());
                BigInteger availableBalance = balance.subtract(getSafeLockBalance(_tokenName));
                auction = new Auction(availableBalance, caller, value, now);
                this.auctions.set(_tokenName, auction);

                // Event log
                AuctionStart(caller, auction.id(), _tokenName, value, auction.endTime());
            } else {
                BigInteger existingBidAmount = auction.bidAmount();

                // Check if minimum value is greater MINIMUM_INCREMENTAL_BID_PERCENT than old value
                BigInteger minimumBidAmount = existingBidAmount.add(existingBidAmount.multiply(minimumIncrementalBidPercent.get()).divide(BigInteger.valueOf(100)));
                Context.require(minimumBidAmount.compareTo(value) <= 0);

                // Refund to the previous bidder
                Address previousBidder = auction.bidder();
                try {
                    Context.transfer(previousBidder, existingBidAmount);
                } catch (Exception e) {
                    Context.println("[Exception] " + e.getMessage());
                    this.refundableBalances.set(caller, getSafeRefundableBalance(caller).add(auction.bidAmount()));
                }

                this.auctions.set(_tokenName, this.auctions.get(_tokenName).setNewBidder(caller, value));

                // Event Log
                BidInfo(auction.id(), _tokenName, previousBidder, existingBidAmount, auction.bidder(), auction.bidAmount());
            }
        }
    }

    @External
    public void withdrawal() {
        Address caller = Context.getCaller();
        BigInteger amount = getSafeRefundableBalance(caller);
        Context.require(amount.compareTo(BigInteger.ZERO) > 0);

        // refund to loser
        Context.transfer(caller, amount);

        // Remove loser in
        this.refundableBalances.set(caller, BigInteger.ZERO);
    }

    @External
    public void claim(String _tokenName) {
        // End Auction
        if (isAuctionExpired(_tokenName)) {
            endAuction(getSafeTokenScore(_tokenName), getSafeAuction(_tokenName));
        }

        Token token = getSafeTokenScore(_tokenName);
        Context.require(token != null);

        Address caller = Context.getCaller();
        BigInteger amount = getSafeTokenBalance(caller, _tokenName);
        Context.require(amount.compareTo(BigInteger.ZERO) > 0);

        // transfer token to loser
        if (!token.isIRC31()) {
            Context.call(token.address(), "safeTransferFrom", Context.getAddress(), caller, token.tokenId(), amount, "transfer to bidder".getBytes());
        } else {
            Context.call(token.address(), "transfer", caller, amount, "transfer to bidder".getBytes());
        }

        // release Token locked
        this.lockedBalances.set(_tokenName, this.getSafeLockBalance(_tokenName).subtract(amount));

        // Remove loser in
        this.tokenBalances.at(caller).set(_tokenName, BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger availableBalance(String _tokenName) {
        Token token = getSafeTokenScore(_tokenName);
        Context.require(token != null);
        BigInteger balance = getTokenBalance(token, Context.getAddress());
        Auction auction = getSafeAuction(_tokenName);
        BigInteger lockedBalance = getSafeLockBalance(_tokenName);
        if (auction == null) {
            return balance.subtract(lockedBalance);
        } else {
            if (isAuctionExpired(_tokenName)) {
                if (balance.compareTo(auction.tokenAmount().add(lockedBalance)) > -1) {
                    return balance.subtract(auction.tokenAmount()).subtract(lockedBalance);
                } else return BigInteger.ZERO;
            } else {
                return auction.tokenAmount();
            }
        }
    }

    /**
     * Set end auction if current auction start over TWELVE_HOURS milliseconds
     *
     * This function will:
     * - set end current auction
     * - send token to last bidder
     * - send ICX to CPS
     */
    private void endAuction(Token _token, Auction _auction) {
        // Send token to winner
        BigInteger balance = getTokenBalance(_token, Context.getAddress());
        Context.require(balance.compareTo(this.auctions.get(_token.name()).tokenAmount()) >= 0);

        try {
            if (_token.isIRC31()) {
                Context.call(_token.address(), "safeTransferFrom", Context.getAddress(), _auction.bidder(), _token.tokenId(), _auction.tokenAmount(), "transfer to bidder".getBytes());
            } else {
                Context.call(_token.address(), "transfer", _auction.bidder(), _auction.tokenAmount(), "transfer to bidder".getBytes());
            }
        } catch (Exception e) {
            Context.println("[Exception] " + e.getMessage());

            // Lock amount token to winner claims manually
            this.lockedBalances.set(_token.name(), getSafeLockBalance(_token.name()).add(_auction.tokenAmount()));
            this.tokenBalances.at(_auction.bidder()).set(_token.name(), getSafeTokenBalance(_auction.bidder(), _token.name()).add(_auction.tokenAmount()));
        }

        // Send ICX to CPS
        if (!_auction.bidAmount().equals(BigInteger.ZERO)) {
            Context.call(_auction.bidAmount(), this.addressCPS.get(), "add_fund");
        }

        // Reset auction
        this.auctions.set(_token.name(), null);

        // Event log
        AuctionEnded(_auction.id(), _token.name(), _auction.bidder(), _auction.tokenAmount(), _auction.bidAmount(), Context.getTransactionTimestamp());
    }

    boolean isAuctionExpired(String _tokenName) {
        Auction auction = getSafeAuction(_tokenName);
        // checks if auction is not started yet or ended
        return auction == null || Context.getTransactionTimestamp() >= auction.endTime();
    }

    @EventLog(indexed = 3)
    protected void BidInfo(BigInteger auctionID, String tokenName, Address currentBidder, BigInteger currentBidAmount, Address newBidder,  BigInteger newBidAmount ) {}

    @EventLog(indexed = 3)
    protected void AuctionStart(Address firstBidder, BigInteger auctionID, String tokenName, BigInteger bidAmount, long deadline) {}

    @EventLog(indexed = 3)
    protected void AuctionEnded(BigInteger auctionID, String tokenName, Address winner, BigInteger tokenAmount, BigInteger bidAmount, long deadline) {}
}