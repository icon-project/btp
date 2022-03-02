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

package foundation.icon.btp.irc2Tradeable;

import com.iconloop.score.token.irc2.IRC2Basic;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.RevertedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import score.UserRevertedException;

import java.math.BigInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
public class IRC2SupplierTest implements IRC2IntegrationTest {

    static Address caller = Address.of(irc2Client._wallet());
    static Address owner = caller;
    static BigInteger value = new BigInteger("1000000000000000000000");
    static Address operator = Faker.address(Address.Type.EOA);
    static Address ZERO_ADDRESS = new Address("hx0000000000000000000000000000000000000000");

    public static Consumer<TransactionResult> mintEventChecker(
            Address caller, Address to, BigInteger value) {
        return IRC2IntegrationTest.eventLogChecker(TransferEventLog::eventLogs, (el) -> {
            assertEquals(ZERO_ADDRESS, el.getFrom());
            assertEquals(to, el.getTo());
            assertEquals(value, el.getValue());
        });
    }

    public static Consumer<TransactionResult> transferFromEventChecker(
            Address caller, Address from, Address to, BigInteger value) {
        return IRC2IntegrationTest.eventLogChecker(TransferEventLog::eventLogs, (el) -> {
            assertEquals(from, el.getFrom());
            assertEquals(to, el.getTo());
            assertEquals(value, el.getValue());
        });
    }

    public static Consumer<TransactionResult> burnEventChecker(
            Address caller, BigInteger value) {
        return IRC2IntegrationTest.eventLogChecker(TransferEventLog::eventLogs, (el) -> {
            assertEquals(caller, el.getFrom());
            assertEquals(ZERO_ADDRESS, el.getTo());
            assertEquals(value, el.getValue());
        });
    }

    public static Consumer<TransactionResult> approvalEventChecker(
            Address spender, BigInteger value) {
        return IRC2IntegrationTest.eventLogChecker(ApprovalEventLog::eventLogs, (el) -> {
            assertEquals(caller, el.getOwner());
            assertEquals(spender, el.getSpender());
            assertEquals(value, el.getValue());
        });
    }

    public static void balanceCheck(Address address, BigInteger value, Executable executable) {
        BigInteger balance = irc2Supplier.balanceOf(address);
        try {
            executable.execute();
        } catch (UserRevertedException | RevertedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        assertEquals(balance.add(value), irc2Supplier.balanceOf(address));
    }

    public static void allowanceCheck(Address spender, BigInteger value, Executable executable) {
        BigInteger alowance = irc2Supplier.allowance(caller, spender);
        try {
            executable.execute();
        } catch (UserRevertedException | RevertedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        assertEquals(value, irc2Supplier.allowance(caller, spender));
    }

    public static void mint(Address to, BigInteger value) {
        balanceCheck(to, value, () ->
                ((IRC2SupplierScoreClient) irc2Supplier).mint(
                        mintEventChecker(caller, to, value),
                        to, value));
    }

    public static void burn(BigInteger value) {
        balanceCheck(caller, value.negate(), () ->
                ((IRC2SupplierScoreClient) irc2Supplier).burn(
                        burnEventChecker(caller, value), value));
    }

    public static void approve(Address spender, BigInteger allowance) {
        allowanceCheck(spender, allowance, () ->
                ((IRC2SupplierScoreClient) irc2Supplier).approve(
                        approvalEventChecker(spender, allowance), spender, allowance));
    }

    @Test
    @Order(1)
    void mintShouldSuccess() {
        System.out.println("-------- start mintShouldSuccess" + caller.toString());
        mint(owner, value);
    }

    @Test
    @Order(2)
    void burnShouldSuccess() {
        mint(owner, value);

        burn(value);
    }

    @Test
    @Order(3)
    void approveShouldSuccess() {
        approve(Address.of(tester), new BigInteger("1000000000000000000000"));
    }

    static void transferFromWithTester(Address from, Address to, BigInteger value) {
        balanceCheck(to, value, () ->
                ((IRC2SupplierScoreClient) irc2SupplierWithTester).transferFrom(
                        transferFromEventChecker(owner, from, to, value),
                        from, to, value, null));
    }

    @Test
    @Order(4)
    void transferFrom() {
        // set allowance from owner to tester to zero
        approve(Address.of(tester), BigInteger.ZERO);

        BigInteger transferAmount = new BigInteger("1000000000000000000");
        mint(Address.of(tester), transferAmount);
        approve(Address.of(tester), transferAmount);

        transferFromWithTester(owner, Address.of(tester), transferAmount);
    }
}
