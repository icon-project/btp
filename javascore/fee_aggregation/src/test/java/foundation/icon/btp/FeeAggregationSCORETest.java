/*
 * Copyright 2020 ICONLOOP Inc.
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

package foundation.icon.btp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import score.Address;
import tools.Account;
import tools.Score;
import tools.ServiceManager;
import tools.TestBase;


import java.math.BigInteger;
import java.util.Map;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FeeAggregationSCORETest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static final Account cps = sm.createAccount();
    private Score tokenScore;
    private Score token31Score;
    private Score feeAggregationSCORE;
    private FeeAggregationSCORE feeAggregationSCORESpy;
    private Address contractAddress;

    private static final String TOKEN_NAME = "IRCToken";
    private static final String TOKEN_NAME_IRC31 = "IRC31Token";
    private static final BigInteger TOKEN_ID = BigInteger.ONE;
    private static final String SYMBOL = "SAT";
    private static final int decimals = 18;
    private static final BigInteger initialSupply = BigInteger.valueOf(1000);
    private static final BigInteger totalSupply = initialSupply.multiply(TEN.pow(decimals));

    @BeforeEach
    public void setup() throws Exception {
        tokenScore = sm.deploy(owner, SampleToken.class, TOKEN_NAME, SYMBOL, BigInteger.valueOf(decimals), initialSupply);
        token31Score = sm.deploy(owner, SampleToken.class, TOKEN_NAME, SYMBOL, BigInteger.valueOf(decimals), initialSupply);
        contractAddress = tokenScore.getAddress();
        feeAggregationSCORE = sm.deploy(owner, FeeAggregationSCORE.class, cps.getAddress());
        feeAggregationSCORESpy = (FeeAggregationSCORE) spy(feeAggregationSCORE.getInstance());
        feeAggregationSCORE.setInstance(feeAggregationSCORESpy);
        feeAggregationSCORE.invoke(owner, "setDurationTime", new BigInteger("1000000000000"));
    }

    @Test
    void register_Success() {
        // register IRC2 Token
        feeAggregationSCORE.invoke(owner, "registerIRC2",TOKEN_NAME, contractAddress);
        verify(feeAggregationSCORESpy).registerIRC2(TOKEN_NAME, contractAddress);

        // register IRC31 Token
        feeAggregationSCORE.invoke(owner, "registerIRC31",TOKEN_NAME_IRC31, contractAddress, TOKEN_ID);
        verify(feeAggregationSCORESpy).registerIRC31(TOKEN_NAME_IRC31, contractAddress, TOKEN_ID);
    }

    @Test
    void register_Failed() {
        Address notContractAddress = sm.createAccount().getAddress();

        // Throw error when address is not contract
        assertThrows(AssertionError.class, () -> feeAggregationSCORE.invoke(owner, "registerIRC2", TOKEN_NAME, notContractAddress));
        assertThrows(AssertionError.class, () -> feeAggregationSCORE.invoke(owner, "registerIRC31", TOKEN_NAME_IRC31, notContractAddress, TOKEN_ID));

        // Throw error when tokenId is invalid
        assertThrows(AssertionError.class, () -> feeAggregationSCORE.invoke(owner, "registerIRC31", TOKEN_NAME_IRC31, contractAddress, BigInteger.ZERO));

        // Throw error when token name existed
        register_Success();
        assertThrows(AssertionError.class, () -> feeAggregationSCORE.invoke(owner, "registerIRC2", TOKEN_NAME, contractAddress));
        assertThrows(AssertionError.class, () -> feeAggregationSCORE.invoke(owner, "registerIRC31", TOKEN_NAME_IRC31, contractAddress, TOKEN_ID));
    }

    @Test
    void transferToken() {
        tokenScore.invoke(owner, "transfer", feeAggregationSCORE.getAddress(), totalSupply, "transfer fee".getBytes());
        assertEquals(totalSupply, tokenScore.call("balanceOf", feeAggregationSCORE.getAddress()));
    }

    @Test
    void availableBalance() {
        register_Success();
        transferToken();

        BigInteger balance = (BigInteger) feeAggregationSCORE.call("availableBalance", TOKEN_NAME);
        assertEquals(balance, tokenScore.call("balanceOf", feeAggregationSCORE.getAddress()));
    }

    @Test
    void bid_Success() {
        register_Success();
        transferToken();

        // Success when first bid
        Account userA = sm.createAccount(1000);
        BigInteger fundA = ICX.multiply(BigInteger.valueOf(100));

        BigInteger availableBalanceA = userA.getBalance();
        sm.call(userA, fundA, feeAggregationSCORE.getAddress(), "bid", TOKEN_NAME);
        assertEquals(availableBalanceA.subtract(fundA), Account.getAccount(userA.getAddress()).getBalance());
        assertEquals(fundA, Account.getAccount(feeAggregationSCORE.getAddress()).getBalance());

        Map<String, String> bidInfo = (Map<String, String>) feeAggregationSCORE.call("getCurrentAuction", TOKEN_NAME);
        assertNotEquals(bidInfo, null);
        assertEquals(bidInfo.get("_bidder"), userA.getAddress().toString());
        assertEquals(bidInfo.get("_bidAmount"), "0x" + fundA.toString(16));

        BigInteger amount = (BigInteger) tokenScore.call("balanceOf", feeAggregationSCORE.getAddress());
        assertEquals(bidInfo.get("_tokenAmount"), "0x" + amount.toString(16));

        // Success when bid an ongoing auction
        Account userB = sm.createAccount(1000);
        BigInteger fundB = ICX.multiply(BigInteger.valueOf(150));
        BigInteger availableBalanceB = userB.getBalance();
        sm.call(userB, fundB, feeAggregationSCORE.getAddress(), "bid", TOKEN_NAME);

        assertNotEquals(fundA, Account.getAccount(feeAggregationSCORE.getAddress()).getBalance());
        assertEquals(availableBalanceB.subtract(fundB), Account.getAccount(userB.getAddress()).getBalance());
        assertEquals(fundB, Account.getAccount(feeAggregationSCORE.getAddress()).getBalance());
    }

    @Test
    void bid_Failed() {
        Account userA = sm.createAccount(1000);
        BigInteger fundA = ICX.multiply(BigInteger.valueOf(100));

        // Failed when Token Name is not registered yet
        String tokenNameRegister = "ABCToken";
        assertThrows(AssertionError.class, () -> sm.call(userA, fundA, feeAggregationSCORE.getAddress(), "bid", tokenNameRegister));

        // Failed when balance of Token Name equal 0
        register_Success();
        assertThrows(AssertionError.class, () -> sm.call(userA, fundA, feeAggregationSCORE.getAddress(), "bid", TOKEN_NAME));

        // Failed when bid value less than 100 ICX
        transferToken();
        assertThrows(AssertionError.class, () -> sm.call(userA, BigInteger.valueOf(99), feeAggregationSCORE.getAddress(), "bid", TOKEN_NAME));

        // Failed when bid value less MINIMUM_INCREMENTAL_BID_PERCENT than old value
        sm.call(userA, fundA, feeAggregationSCORE.getAddress(), "bid", TOKEN_NAME);
        Account userB = sm.createAccount(1000);
        BigInteger fundB = ICX.multiply(BigInteger.valueOf(109));
        assertThrows(AssertionError.class, () -> sm.call(userB, fundB, feeAggregationSCORE.getAddress(), "bid", TOKEN_NAME));
    }
}
