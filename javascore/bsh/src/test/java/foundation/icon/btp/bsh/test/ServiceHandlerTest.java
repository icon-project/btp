/*
 * Copyright 2021 ICON Foundation
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

package foundation.icon.btp.bsh.test;

import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import foundation.icon.btp.bsh.ServiceHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import score.ByteArrayObjectWriter;
import score.Context;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ServiceHandlerTest extends TestBase {
    final static String RLPn = "RLPn";
    private static final ServiceManager sm = getServiceManager();
    private Account[] owners;
    private Score bshScore;
    private ServiceHandler bshSyp;

    @BeforeEach
    void setup() throws Exception {
        // setup accounts and deploy
        owners = new Account[3];
        for (int i = 0; i < owners.length; i++) {
            owners[i] = sm.createAccount(100);
        }
        String initialOwners = Arrays.stream(owners)
                .map(a -> a.getAddress().toString())
                .collect(Collectors.joining(","));
        bshScore = sm.deploy(owners[0], ServiceHandler.class);

        // setup spy
       /* bshSyp = (ServiceHandler) spy(bshScore.getInstance());
        bshScore.setInstance(bshSyp);*/
    }


    @Test
    public void shouldHandleBTPMessage() {

        //Step 1: register the token with BSH
        bshScore.invoke(owners[0], "register",
                "BNB",
                owners[0].getAddress()
        );

        // Verify the Token Registration
        String[] tokenNames = (String[]) bshScore.call("tokenNames");
        assertEquals(tokenNames.length, 1);

        //Step 2: Deposit some registered token into BSH
        bshScore.invoke(owners[0], "tokenFallback",
                owners[0].getAddress(),
                BigInteger.TEN,
                new byte[0]);
        // Verify the Balance of the token
        BigInteger[] tokenBalance = (BigInteger[]) bshScore.call("getBalance", owners[0].getAddress(), "BNB");
        assertEquals(BigInteger.TEN, tokenBalance[0]);

        //Step 3: invoke ShouldHandleBTP message
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        writer.beginList(2);
        writer.write(ActionTypes.REQUEST_TOKEN_TRANSFER.value);//ActionType
        //Action Data writer -start
        //DataWriter dataWriter = Codec.rlp.newWriter();
        writer.beginList(4);
        writer.write(owners[0].getAddress().toString());
        writer.write(owners[0].getAddress().toString());
        writer.write("BNB");
        writer.write(BigInteger.TWO);
        writer.end();
        //Action Data - end
        //writer.write(dataWriter.toByteArray());//Action Data
        writer.end();
        bshScore.invoke(owners[0], "handleBTPMessage",
                owners[0].getAddress().toString(),
                "BMC",
                BigInteger.TWO,
                writer.toByteArray());

    }

    @Test
    public void transfer() {
        //Step 1: register the token with BSH
        bshScore.invoke(owners[0], "register",
                "BNB",
                owners[0].getAddress()
        );

        // Verify the Token Registration
        String[] tokenNames = (String[]) bshScore.call("tokenNames");
        assertEquals(tokenNames.length, 1);

        //Step 2: Deposit some registered token into BSH
        bshScore.invoke(owners[0], "tokenFallback",
                owners[0].getAddress(),
                BigInteger.TEN,
                new byte[0]);
        // Verify the Balance of the token
        BigInteger[] tokenBalance = (BigInteger[]) bshScore.call("getBalance", owners[0].getAddress(), "BNB");
        assertEquals(BigInteger.TEN, tokenBalance[0]);

        //Step 3: Call transfer method
        bshScore.invoke(owners[0], "transfer", "BNB", owners[0].getAddress().toString(), BigInteger.TWO);

    }


    // This enum holds the index of the different type of balances for an User token
    enum ActionTypes {
        REQUEST_TOKEN_TRANSFER("REQUEST_TOKEN_TRANSFER ", 0),
        REQUEST_TOKEN_REGISTER("REQUEST_TOKEN_REGISTER ", 1),
        RESPONSE_HANDLE_SERVICE("RESPONSE_HANDLE_SERVICE  ", 2),
        RESPONSE_UNKNOWN_("RESPONSE_UNKNOWN_ ", 3);

        private final String key;
        private final Integer value;

        ActionTypes(String key, Integer value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public Integer getValue() {
            return value;
        }
    }
}
