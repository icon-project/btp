package foundation.icon.btp;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class FeeAggregationSCORE extends IRC31Receiver {
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
        durationTime.set(43200000000L);
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

    /**
     * (Operator)
     * Operator register a IRC2 token
     *
     * @param _tokenName        Name of a token
     * @param _tokenAddress     Address of token contract
     */
    @External
    public void registerIRC2(String _tokenName, Address _tokenAddress) {
        Context.require(Context.getCaller().equals(Context.getOwner()));
        Context.require(getSafeTokenScore(_tokenName) == null);
        Context.require(_tokenAddress.isContract());

        this.tokensScore.set(_tokenName, new Token(_tokenName, _tokenAddress));
        this.supportedTokens.add(_tokenName);
    }

    /**
     * (Operator)
     * Operator register a IRC31 Token
     *
     * @param _tokenName        Name of a token
     * @param _tokenAddress     Address of token contract
     * @param _tokenId          Id of token
     */
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
     * (Operator)
     * Operator set the duration time for auction
     *
     * @param _duration        Duration time of a auction
     */
    @External
    public void setDurationTime(BigInteger _duration) {
        Context.require(Context.getCaller().equals(Context.getOwner()));
        Context.require(!_duration.equals(BigInteger.ZERO));

        durationTime.set(_duration.longValueExact());
    }

    @External
    public BigInteger getDurationTime() {
        return BigInteger.valueOf(durationTime.getOrDefault(0L));
    }

    /**
     * (User)
     * List of tokens which defined in supportedTokens and tokensScore
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

        if (isAuctionExpired(_tokenName)) return null;
        return getSafeAuction(_tokenName).toMap();
    }

    /**
     * Pure transfer ICX
     */
    @Payable
    public void fallback() {
        Address _from = Context.getCaller();
        BigInteger _value = Context.getValue();
        Context.require(_value.compareTo(BigInteger.ZERO) > 0);

        Context.call(_value, this.addressCPS.get(), "add_fund");
    }

    @External
    public void tokenFallback(Address _from, BigInteger _value, byte[] _data) { }

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

        long now = Context.getBlockTimestamp();
        Auction auction = getSafeAuction(_tokenName);
        Address caller = Context.getCaller();

        // Check if auction is not start yet
        if (auction == null) {
            // Check available balance of token enough to start an auction
            BigInteger availableBalance = balance.subtract(getSafeLockBalance(_tokenName));
            Context.require(availableBalance.compareTo(BigInteger.ZERO) > 0);

            auction = new Auction(availableBalance, caller, value, now);
            this.auctions.set(_tokenName, auction);

            // Event log
            AuctionStart(auction.id(), _tokenName, caller , value, auction.endTime());
        } else {
            // Check auction is ended
            if (isAuctionExpired(_tokenName)) {
                Auction oldAuction = this.auctions.get(_tokenName);

                // Bid for new auction
                balance = getTokenBalance(token, Context.getAddress());
                BigInteger availableBalance = balance.subtract(getSafeLockBalance(_tokenName)).subtract(oldAuction.tokenAmount());
                Context.require(availableBalance.compareTo(BigInteger.ZERO) > 0);

                auction = new Auction(availableBalance, caller, value, now);
                this.auctions.set(_tokenName, auction);

                // Transfer token to winner & ICX to CPS
                endAuction(token, oldAuction);

                // Event log
                AuctionStart(auction.id(), _tokenName, caller, value, auction.endTime());
            } else {
                BigInteger existingBidAmount = auction.bidAmount();

                // Check if minimum value is greater MINIMUM_INCREMENTAL_BID_PERCENT than old value
                BigInteger minimumBidAmount = existingBidAmount.add(existingBidAmount.multiply(minimumIncrementalBidPercent.get()).divide(BigInteger.valueOf(100)));
                Context.require(minimumBidAmount.compareTo(value) <= 0);

                Address previousBidder = auction.bidder();

                this.auctions.set(_tokenName, this.auctions.get(_tokenName).setNewBidder(caller, value));

                // Refund to the previous bidder
                try {
                    Context.transfer(previousBidder, existingBidAmount);
                } catch (Exception e) {
                    Context.println("[Exception] " + e.getMessage());
                    this.refundableBalances.set(previousBidder, getSafeRefundableBalance(previousBidder).add(existingBidAmount));
                }

                auction = getSafeAuction(_tokenName);

                // Event Log
                BidInfo(auction.id(), _tokenName, previousBidder, existingBidAmount, auction.bidder(), auction.bidAmount());
            }
        }
    }

    /**
     * (User)
     * User use to withdraw their ICX when another bid higher but the system can not transfer ICX to them
     */
    @External
    public void withdrawal() {
        Address caller = Context.getCaller();
        BigInteger amount = getSafeRefundableBalance(caller);
        Context.require(amount.compareTo(BigInteger.ZERO) > 0);

        // Remove loser in
        this.refundableBalances.set(caller, BigInteger.ZERO);

        // refund to loser
        try {
            Context.transfer(caller, amount);
        } catch (Exception e) {
            Context.println("[Exception] Withdrawal " + e.getMessage());
            this.refundableBalances.set(caller, getSafeRefundableBalance(caller).add(amount));
        }
    }

    /**
     * (User)
     * User use to claim their tokens when User win this token but the system can not transfer tokens to them
     */
    @External
    public void claim(String _tokenName) {
        Token token = getSafeTokenScore(_tokenName);
        Context.require(token != null);

        Address caller = Context.getCaller();
        BigInteger amount = getSafeTokenBalance(caller, _tokenName);
        Context.require(amount.compareTo(BigInteger.ZERO) > 0);

        // release Token locked
        this.lockedBalances.set(_tokenName, this.getSafeLockBalance(_tokenName).subtract(amount));

        // Remove loser in
        this.tokenBalances.at(caller).set(_tokenName, BigInteger.ZERO);

        try {
            // transfer token to winner
            if (!token.isIRC31()) {
                Context.call(token.address(), "transferFrom", Context.getAddress(), caller, token.tokenId(), amount, "transfer to bidder".getBytes());
            } else {
                Context.call(token.address(), "transfer", caller, amount, "transfer to bidder".getBytes());
            }
        } catch (Exception e) {
            Context.println("[Exception] " + e.getMessage());

            // Lock amount token to winner claims manually
            this.lockedBalances.set(_tokenName, getSafeLockBalance(_tokenName).add(amount));
            this.tokenBalances.at(caller).set(_tokenName, amount);
        }

        // End Auction
        Auction auction = getSafeAuction(_tokenName);
        if (auction != null && Context.getBlockTimestamp() >= auction.endTime()) {
            this.auctions.set(_tokenName, null);
            endAuction(getSafeTokenScore(_tokenName), auction);
        }
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
        Context.require(balance.compareTo(_auction.tokenAmount()) >= 0);

        // Send ICX to CPS
        if (!_auction.bidAmount().equals(BigInteger.ZERO)) {
            Context.call(_auction.bidAmount(), this.addressCPS.get(), "add_fund");
        }

        try {
            if (_token.isIRC31()) {
                Context.call(_token.address(), "transferFrom", Context.getAddress(), _auction.bidder(), _token.tokenId(), _auction.tokenAmount(), "transfer to bidder".getBytes());
            } else {
                Context.call(_token.address(), "transfer", _auction.bidder(), _auction.tokenAmount(), "transfer to bidder".getBytes());
            }
        } catch (Exception e) {
            Context.println("[Exception] " + e.getMessage());

            // Lock amount token to winner claims manually
            this.lockedBalances.set(_token.name(), getSafeLockBalance(_token.name()).add(_auction.tokenAmount()));
            this.tokenBalances.at(_auction.bidder()).set(_token.name(), getSafeTokenBalance(_auction.bidder(), _token.name()).add(_auction.tokenAmount()));
        }

        // Event log
        AuctionEnded(_auction.id(), _token.name(), _auction.bidder(), _auction.tokenAmount(), _auction.bidAmount(), Context.getBlockTimestamp());
    }

    boolean isAuctionExpired(String _tokenName) {
        Auction auction = getSafeAuction(_tokenName);
        // checks if auction is not started yet or ended
        return auction == null || Context.getBlockTimestamp() >= auction.endTime();
    }

    @EventLog(indexed = 3)
    protected void BidInfo(BigInteger auctionID, String tokenName, Address currentBidder, BigInteger currentBidAmount, Address newBidder,  BigInteger newBidAmount) {}

    @EventLog(indexed = 3)
    protected void AuctionStart(BigInteger auctionID, String tokenName, Address firstBidder, BigInteger bidAmount, long deadline) {}

    @EventLog(indexed = 3)
    protected void AuctionEnded(BigInteger auctionID, String tokenName, Address winner, BigInteger tokenAmount, BigInteger bidAmount, long deadline) {}
}