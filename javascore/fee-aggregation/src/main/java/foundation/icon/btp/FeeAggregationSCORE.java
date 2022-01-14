package foundation.icon.btp;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class FeeAggregationSCORE extends IRC31Receiver {
    public static final VarDB<BigInteger> syndicationCount = Context.newVarDB("syndicationCount", BigInteger.class);
    public static final BranchDB<BigInteger, DictDB<Address, BigInteger>> syndicationDepositorAmount = Context.newBranchDB("syndicationDepositorAmount", BigInteger.class);
    public static final VarDB<Long> durationTime = Context.newVarDB("durationTime", Long.class);
    public static final BranchDB<BigInteger, ArrayDB<Address>> syndicationDepositorList = Context.newBranchDB("syndicationDepositorList", Address.class);

    /**
     * Address of the Contribution Proposal System.
     */
    private final VarDB<Address> addressCPS = Context.newVarDB("addressCPS", Address.class);

    /**
     * Address of the Band protocol 
     */
    private final VarDB<Address> addressBandProtocol = Context.newVarDB("addressBandProtocol", Address.class);

    /**
     * Discount percent of token price
     */
    private final VarDB<BigInteger> discount = Context.newVarDB("discount", BigInteger.class);

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
    private final DictDB<String, Syndication> syndications;

    /**
     * Lock balance not available for auction;
     * - Key: tokenName
     * - Value: amount of tokens
     */
    public final DictDB<String, BigInteger> lockedBalances;

    /**
     * Balance token of winner
     * - Key: tokenName
     */
    private final BranchDB<Address, DictDB<String, BigInteger>> tokenBalances;

    /**
     * Amount to refund
     */
    private final DictDB<Address, BigInteger> refundableBalances;

    public FeeAggregationSCORE(Address _cps_address, Address _band_protocol_address) {
        this.addressCPS.set(_cps_address);
        this.addressBandProtocol.set(_band_protocol_address);
        this.tokensScore = Context.newDictDB("tokensScore", Token.class);
        this.supportedTokens = Context.newArrayDB("supportedTokens", String.class);
        this.syndications = Context.newDictDB("syndications", Syndication.class);
        this.tokenBalances = Context.newBranchDB("tokenBalances", BigInteger.class);
        this.refundableBalances = Context.newDictDB("refundableBalances", BigInteger.class);
        this.lockedBalances = Context.newDictDB("lockedBalances", BigInteger.class);
        this.discount.set(BigInteger.valueOf(10L));
        durationTime.set(43200000000L);
        syndicationCount.set(BigInteger.ZERO);
    };

    private Token getSafeTokenScore(String _tokenName) {
        return this.tokensScore.getOrDefault(_tokenName, null);
    }

    private BigInteger getSafeLockBalance(String _tokenName) {
        return this.lockedBalances.getOrDefault(_tokenName, BigInteger.ZERO);
    }

    private Syndication getSafeSyndication(String _tokenName) {
        return this.syndications.getOrDefault(_tokenName, null);
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

    private BigInteger getTokenPrice(Token _token) {
        Map<String, Object> referenceData = (Map<String, Object>) Context.call(this.addressBandProtocol.get(), "get_reference_data", _token.name(), "ICX");
        return (BigInteger) referenceData.get("rate");
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
        if (!Context.getCaller().equals(Context.getOwner())) {
            Context.revert(ErrorCode.PERMISSION_DENIED, "permission denied");
        }
        if (getSafeTokenScore(_tokenName) != null) {
            Context.revert(ErrorCode.INVALID_TOKEN_NAME, "_tokenName is existed");
        }
        if (!_tokenAddress.isContract()) {
            Context.revert(ErrorCode.INVALID_CONTRACT_ADDRESS, "_tokenAddress is invalid");
        }

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
    public void registerIRC31(String _tokenName, Address _tokenAddress, BigInteger _tokenId, BigInteger _decimal) {
        if (!Context.getCaller().equals(Context.getOwner())) {
            Context.revert(ErrorCode.PERMISSION_DENIED, "permission denied");
        }
        if (getSafeTokenScore(_tokenName) != null) {
            Context.revert(ErrorCode.INVALID_TOKEN_NAME, "_tokenName is existed");
        }
        if (!_tokenAddress.isContract()) {
            Context.revert(ErrorCode.INVALID_CONTRACT_ADDRESS, "_tokenAddress is invalid");
        }
        if (_tokenId.compareTo(BigInteger.ZERO) <= 0) {
            Context.revert(ErrorCode.INVALID_VALUE, "_tokenId is invalid, must > 0");
        }

        this.tokensScore.set(_tokenName, new Token(_tokenName, _tokenAddress, _tokenId, _decimal));
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
        if (!Context.getCaller().equals(Context.getOwner())) {
            Context.revert(ErrorCode.PERMISSION_DENIED, "permission denied");
        }
        if (_duration.equals(BigInteger.ZERO)) {
            Context.revert(ErrorCode.INVALID_VALUE, "_duration is invalid, must > 0");
        }

        durationTime.set(_duration.longValueExact());
    }

    @External(readonly = true)
    public BigInteger getDurationTime() {
        return BigInteger.valueOf(durationTime.getOrDefault(0L));
    }

    @External(readonly = true)
    public BigInteger getTokenBalance(String _tokenName, Address _address) {
        return getSafeTokenBalance(_address, _tokenName);
    }

    @External(readonly = true)
    public BigInteger getRefundableBalance(Address _address) {
        return this.getSafeRefundableBalance(_address);
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
    public Map<String, String> getCurrentSyndication(String _tokenName) {
        if (getSafeTokenScore(_tokenName) == null) {
            Context.revert(ErrorCode.INVALID_TOKEN_NAME, "_tokenName is not registered yet");
        }

        if (isSyndicationExpired(_tokenName)) return null;
        return getSafeSyndication(_tokenName).toMap();
    }

    /**
     * Get deposited amount of user of current syndication
     *
     * @param _tokenName    - The name of token which defined in supportedTokens and tokensScore
     * @return Auction
     */
    @External(readonly = true)
    public BigInteger getDepositedAmount(String _tokenName, Address _depositor) {
        if (getSafeTokenScore(_tokenName) == null) {
            Context.revert(ErrorCode.INVALID_TOKEN_NAME, "_tokenName is not registered yet");
        }

        Syndication syndication = this.getSafeSyndication(_tokenName);
        if (syndication == null) {
            Context.revert("no active syndication");
        }

        return FeeAggregationSCORE.syndicationDepositorAmount.at(syndication.id()).getOrDefault(_depositor, BigInteger.ZERO);
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
    public void deposit(String _tokenName) {
        Token token = getSafeTokenScore(_tokenName);
        if (token == null) {
            Context.revert(ErrorCode.INVALID_TOKEN_NAME, "_tokenName is not registered yet");
        }
        BigInteger balance = getTokenBalance(token, Context.getAddress());

        if (balance.equals(BigInteger.ZERO)) {
            Context.revert(ErrorCode.INVALID_TOKEN_BALANCE, "available token balance is 0");
        }

        BigInteger value = Context.getValue();

        long now = Context.getBlockTimestamp();
        Syndication syndication = getSafeSyndication(_tokenName);
        Address caller = Context.getCaller();

        // Check if auction is not start yet
        if (syndication == null) {
            // Check available balance of token enough to start an auction
            BigInteger availableBalance = balance.subtract(getSafeLockBalance(_tokenName));
            if (availableBalance.compareTo(BigInteger.ZERO) <= 0) {
                Context.revert(ErrorCode.INVALID_TOKEN_BALANCE, "available token balance is 0");
            }

            syndication = new Syndication(availableBalance, caller, value, now);
            this.syndications.set(_tokenName, syndication);

            // Event log
            SyndicationStart(syndication.id(), _tokenName, availableBalance, caller, value, syndication.endTime());
        } else {
            // Check auction is ended
            if (isSyndicationExpired(_tokenName)) {
                Syndication oldSyndication = this.syndications.get(_tokenName);

                // Bid for new auction
                balance = getTokenBalance(token, Context.getAddress());
                BigInteger availableBalance = balance.subtract(getSafeLockBalance(_tokenName)).subtract(oldSyndication.tokenAmount());
                if (availableBalance.compareTo(BigInteger.ZERO) <= 0) {
                    Context.revert(ErrorCode.INVALID_TOKEN_BALANCE, "available token balance is 0");
                }

                syndication = new Syndication(availableBalance, caller, value, now);
                this.syndications.set(_tokenName, syndication);

                // Transfer token to winner & ICX to CPS
                endAuction(token, oldSyndication);

                // Event log
                SyndicationStart(syndication.id(), _tokenName, availableBalance, caller, value, syndication.endTime());
            } else {
                syndication = getSafeSyndication(_tokenName);
                syndication.deposit(caller, value);

                this.syndications.set(_tokenName, syndication);

                // Event Log
                DepositInfo(syndication.id(), _tokenName, caller, value);
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
        if (amount.compareTo(BigInteger.ZERO) <= 0) {
            Context.revert(ErrorCode.NOT_FOUND_BALANCE, "not found ICX balance");
        }

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
     * User use to claim their tokens when User deposit and get token
     */
    @External
    public void claim(String _tokenName) {
        Token token = getSafeTokenScore(_tokenName);
        if (token == null) {
            Context.revert(ErrorCode.INVALID_TOKEN_NAME, "_tokenName is not registered yet");
        }

        Address caller = Context.getCaller();
        BigInteger amount = getSafeTokenBalance(caller, _tokenName);
        if (amount.compareTo(BigInteger.ZERO) <= 0) {
            Context.revert(ErrorCode.NOT_FOUND_BALANCE, "not found _tokenName balance");
        }

        // release Token locked
        this.lockedBalances.set(_tokenName, this.getSafeLockBalance(_tokenName).subtract(amount));

        // Remove loser in
        this.tokenBalances.at(caller).set(_tokenName, BigInteger.ZERO);

        try {
            // transfer token to winner
            if (!token.isIRC31()) {
                Context.call(token.address(), "transfer", caller, amount, "transfer to depositor".getBytes());
            } else {
                Context.call(token.address(), "transferFrom", Context.getAddress(), caller, token.tokenId(), amount, "transfer to depositor".getBytes());
            }
        } catch (Exception e) {
            Context.println("[Exception] " + e.getMessage());

            // Lock amount token to winner claims manually
            this.lockedBalances.set(_tokenName, getSafeLockBalance(_tokenName).add(amount));
            this.tokenBalances.at(caller).set(_tokenName, amount);
        }

        // End Auction
        Syndication syndication = getSafeSyndication(_tokenName);
        if (syndication != null && Context.getBlockTimestamp() >= syndication.endTime()) {
            this.syndications.set(_tokenName, null);
            endAuction(getSafeTokenScore(_tokenName), syndication);
        }
    }

    @External(readonly = true)
    public BigInteger availableBalance(String _tokenName) {
        Token token = getSafeTokenScore(_tokenName);
        if (token == null) {
            Context.revert(ErrorCode.INVALID_TOKEN_NAME, "_tokenName is not registered yet");
        }
        BigInteger balance = getTokenBalance(token, Context.getAddress());
        Syndication syndication = getSafeSyndication(_tokenName);
        BigInteger lockedBalance = getSafeLockBalance(_tokenName);
        if (syndication == null) {
            return balance.subtract(lockedBalance);
        } else {
            if (isSyndicationExpired(_tokenName)) {
                if (balance.compareTo(syndication.tokenAmount().add(lockedBalance)) > -1) {
                    return balance.subtract(syndication.tokenAmount()).subtract(lockedBalance);
                } else return BigInteger.ZERO;
            } else {
                return syndication.tokenAmount();
            }
        }
    }

    /**
     * Set end syndication if current syndication start over TWELVE_HOURS milliseconds
     *
     * This function will:
     * - set end current syndication
     * - send token to depositor
     * - send ICX to CPS
     */
    private void endAuction(Token _token, Syndication _syndication) {
        // Send token to winner
        BigInteger balance = getTokenBalance(_token, Context.getAddress());
        if (balance.compareTo(_syndication.tokenAmount()) < 0) {
            Context.revert(ErrorCode.INVALID_TOKEN_BALANCE, "not enough token");
        }

        // Send ICX to CPS
        if (!_syndication.totalDeposited().equals(BigInteger.ZERO)) {
            Context.call(_syndication.totalDeposited(), this.addressCPS.get(), "add_fund");
        }

        this.distributeTokenShare(_token, _syndication);

        // Event log
        SyndicationEnded(_syndication.id(), _token.name(), _syndication.tokenAmount(), Context.getBlockTimestamp());
    }

    public void distributeTokenShare(Token _token, Syndication _syndication) {
        BigInteger syndicationId = _syndication.id();
        BigInteger tokenAmount = _syndication.tokenAmount();
        BigInteger tokenUnit = _token.getUnit();
        BigInteger tokenPrice = this.getTokenPrice(_token);
        BigInteger tokenPriceWithDiscount = tokenPrice.multiply(BigInteger.valueOf(100).subtract(this.discount.get())).divide(BigInteger.valueOf(100L));
        BigInteger tokenAmountInIcx = tokenAmount.multiply(tokenPriceWithDiscount).divide(tokenUnit);
        int numberDepositor = FeeAggregationSCORE.syndicationDepositorList.at(syndicationId).size();
        BigInteger totalDeposited = _syndication.totalDeposited();
        BigInteger totalDistributedToken = BigInteger.ZERO;
        for (int i = 0; i < numberDepositor; i++) {
            Address currentDepositorAddress = FeeAggregationSCORE.syndicationDepositorList.at(syndicationId).get(i);
            BigInteger currentDepositorAmount = FeeAggregationSCORE.syndicationDepositorAmount.at(syndicationId).get(currentDepositorAddress);
            BigInteger tokenShareAmount;
            BigInteger icxRemain = BigInteger.ZERO; 
            if (tokenAmountInIcx.compareTo(totalDeposited) > 0) {
                tokenShareAmount = currentDepositorAmount.multiply(tokenUnit).divide(tokenPriceWithDiscount);
            } else {
                tokenShareAmount = currentDepositorAmount.multiply(tokenAmount).divide(totalDeposited);
                icxRemain = currentDepositorAmount.subtract(tokenShareAmount.multiply(tokenPriceWithDiscount).divide(tokenUnit));
            }
            totalDistributedToken = totalDistributedToken.add(tokenShareAmount);
            this.tokenBalances.at(currentDepositorAddress).set(_token.name(), getSafeTokenBalance(currentDepositorAddress, _token.name()).add(tokenShareAmount));

            if (icxRemain.compareTo(BigInteger.ZERO) > 0) {
                this.refundableBalances.set(currentDepositorAddress, getSafeRefundableBalance(currentDepositorAddress).add(icxRemain));
            }
        }
        this.lockedBalances.set(_token.name(), getSafeLockBalance(_token.name()).add(totalDistributedToken));
    }

    boolean isSyndicationExpired(String _tokenName) {
        Syndication syndication = getSafeSyndication(_tokenName);
        // checks if auction is not started yet or ended
        return syndication == null || Context.getBlockTimestamp() >= syndication.endTime();
    }

    @EventLog(indexed = 3)
    protected void DepositInfo(BigInteger auctionID, String tokenName, Address depositor,  BigInteger amount) {}

    @EventLog(indexed = 3)
    protected void SyndicationStart(BigInteger auctionID, String tokenName, BigInteger tokenAmount, Address firstDepositor,  BigInteger amount, long deadline) {}

    @EventLog(indexed = 3)
    protected void SyndicationEnded(BigInteger auctionID, String tokenName, BigInteger tokenAmount, long deadline) {}
}