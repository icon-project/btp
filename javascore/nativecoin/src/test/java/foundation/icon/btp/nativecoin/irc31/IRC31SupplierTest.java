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

package foundation.icon.btp.nativecoin.irc31;

import com.iconloop.score.token.irc31.IRC31Basic;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.RevertedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;
import score.UserRevertedException;

import java.math.BigInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class IRC31SupplierTest implements IRC31IntegrationTest {

    static Address caller = Address.of(irc31Client._wallet());
    static Address owner = caller;
    static BigInteger id = BigInteger.ONE;
    static BigInteger value = BigInteger.ONE;
    static BigInteger[] ids = new BigInteger[]{id};
    static BigInteger[] values = new BigInteger[]{value};
    static String uri = Faker.faker.internet().url();
    static Address operator = Faker.address(Address.Type.EOA);

    public static Consumer<TransactionResult> mintChecker(
            Address caller, Address to, BigInteger id, BigInteger value) {
        return IRC31IntegrationTest.eventLogChecker(TransferSingleEventLog::eventLogs, (el) -> {
            //caller must be registered owner of OwnerBasedIRC31Supplier
//            assertEquals(caller, el.getOperator());
            assertEquals(to, el.getOperator());//IRC31 must be fixed
            assertEquals(IRC31Basic.ZERO_ADDRESS, el.getFrom());
            assertEquals(to, el.getTo());
            assertEquals(id, el.getId());
            assertEquals(value, el.getValue());
        });
    }

    public static Consumer<TransactionResult> transferFromChecker(
            Address caller, Address from, Address to, BigInteger id, BigInteger value) {
        return IRC31IntegrationTest.eventLogChecker(TransferSingleEventLog::eventLogs, (el) -> {
            assertEquals(caller, el.getOperator());
            assertEquals(from, el.getFrom());
            assertEquals(to, el.getTo());
            assertEquals(id, el.getId());
            assertEquals(value, el.getValue());
        });
    }

    public static Consumer<TransactionResult> burnChecker(
            Address caller, Address from, BigInteger id, BigInteger value) {
        return IRC31IntegrationTest.eventLogChecker(TransferSingleEventLog::eventLogs, (el) -> {
            //caller must be registered owner of OwnerBasedIRC31Supplier
//            assertEquals(caller, el.getOperator());
            assertEquals(from, el.getOperator());//IRC31 must be fixed
            assertEquals(from, el.getFrom());
            assertEquals(IRC31Basic.ZERO_ADDRESS, el.getTo());
            assertEquals(id, el.getId());
            assertEquals(value, el.getValue());
        });
    }

    public static Consumer<TransactionResult> mintBatchChecker(
            Address caller, Address to, BigInteger[] ids, BigInteger[] values) {
        return IRC31IntegrationTest.eventLogChecker(TransferBatchEventLog::eventLogs, (el) -> {
            //caller must be registered owner of OwnerBasedIRC31Supplier
//            assertEquals(caller, el.getOperator());
            assertEquals(to, el.getOperator());//IRC31 must be fixed
            assertEquals(IRC31Basic.ZERO_ADDRESS, el.getFrom());
            assertEquals(to, el.getTo());
            assertArrayEquals(ids, el.getIds());
            assertArrayEquals(values, el.getValues());
        });
    }

    public static Consumer<TransactionResult> transferFromBatchChecker(
            Address caller, Address from, Address to, BigInteger[] ids, BigInteger[] values) {
        return IRC31IntegrationTest.eventLogChecker(TransferBatchEventLog::eventLogs, (el) -> {
            assertEquals(caller, el.getOperator());
            assertEquals(from, el.getFrom());
            assertEquals(to, el.getTo());
            assertArrayEquals(ids, el.getIds());
            assertArrayEquals(values, el.getValues());
        });
    }

    public static Consumer<TransactionResult> burnBatchChecker(
            Address caller, Address from, BigInteger[] ids, BigInteger[] values) {
        return IRC31IntegrationTest.eventLogChecker(TransferBatchEventLog::eventLogs, (el) -> {
            //caller must be registered owner of OwnerBasedIRC31Supplier
//            assertEquals(caller, el.getOperator());
            assertEquals(from, el.getOperator());//IRC31 must be fixed
            assertEquals(from, el.getFrom());
            assertEquals(IRC31Basic.ZERO_ADDRESS, el.getTo());
            assertArrayEquals(ids, el.getIds());
            assertArrayEquals(values, el.getValues());
        });
    }

    public static void balanceCheck(Address address, BigInteger id, BigInteger value, Executable executable) {
        BigInteger balance = irc31Supplier.balanceOf(address, id);
        try {
            executable.execute();
        } catch (UserRevertedException | RevertedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        assertEquals(balance.add(value), irc31Supplier.balanceOf(address, id));
    }

    public static void balanceBatchCheck(
            Address address, BigInteger[] ids, BigInteger[] values, Executable executable) {
        balanceBatchCheck(address, ids, values, executable, true);
    }

    public static void balanceBatchCheck(
            Address address, BigInteger[] ids, BigInteger[] values, Executable executable, boolean increase) {
        Address[] owners = new Address[ids.length];
        for (int i = 0; i < ids.length; i++) {
            owners[i] = address;
        }
        BigInteger[] balances = irc31Supplier.balanceOfBatch(owners, ids);
        assertEquals(ids.length, balances.length);
        try {
            executable.execute();
        } catch (UserRevertedException | RevertedException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        BigInteger[] expected = new BigInteger[ids.length];
        for (int i = 0; i < ids.length; i++) {
            if (increase) {
                expected[i] = balances[i].add(values[i]);
            } else {
                expected[i] = balances[i].subtract(values[i]);
            }
        }
        assertArrayEquals(expected, irc31Supplier.balanceOfBatch(owners, ids));
    }

    public static void mint(Address to, BigInteger id, BigInteger value) {
        balanceCheck(to, id, value, () ->
                ((IRC31SupplierScoreClient) irc31Supplier).mint(
                        mintChecker(caller, to, id, value),
                        to, id, value));
    }

    public static void burn(Address from, BigInteger id, BigInteger value) {
        balanceCheck(from, id, value.negate(), () ->
                ((IRC31SupplierScoreClient) irc31Supplier).burn(
                        burnChecker(caller, from, id, value),
                        from, id, value));
    }

    public static void mintBatch(Address to, BigInteger[] ids, BigInteger[] values) {
        balanceBatchCheck(to, ids, values, () ->
                ((IRC31SupplierScoreClient) irc31Supplier).mintBatch(
                        mintBatchChecker(caller, to, ids, values),
                        to, ids, values));
    }

    public static void burnBatch(Address from, BigInteger[] ids, BigInteger[] values) {
        balanceBatchCheck(from, ids, values, () ->
                ((IRC31SupplierScoreClient) irc31Supplier).burnBatch(
                        burnBatchChecker(caller, from, ids, values),
                        from, ids, values), false);
    }

    @Test
    void mintShuldSuccess() {
        mint(owner, id, value);
    }

    @Test
    void burnShuldSuccess() {
        mint(owner, id, value);

        burn(owner, id, value);
    }

    @Test
    void mintBatchShuldSuccess() {
        mintBatch(owner, ids, values);
    }

    @Test
    void burnBatchShuldSuccess() {
        mintBatch(owner, ids, values);

        burnBatch(owner, ids, values);
    }

    @Test
    void setTokenURI() {
        ((IRC31SupplierScoreClient) irc31Supplier).setTokenURI(
                IRC31IntegrationTest.eventLogChecker(URIEventLog::eventLogs, (el) -> {
                    assertEquals(id, el.getId());
                    assertEquals(uri, el.getValue());
                }),
                id, uri);
        assertEquals(uri, irc31Supplier.tokenURI(id));
    }

    //test for IRC31
    static void transferFrom(Address from, Address to, BigInteger id, BigInteger value) {
        balanceCheck(to, id, value, () ->
                ((IRC31SupplierScoreClient) irc31Supplier).transferFrom(
                        transferFromChecker(caller, from, to, id, value),
                        from, to, id, value, null));
    }

    static void transferFromBatch(Address from, Address to, BigInteger[] ids, BigInteger[] values) {
        balanceBatchCheck(to, ids, values, () ->
                ((IRC31SupplierScoreClient) irc31Supplier).transferFromBatch(
                        transferFromBatchChecker(caller, from, to, ids, values),
                        from, to, ids, values, null));
    }

    public static void setApprovalForAll(Address operator, boolean approved) {
        ((IRC31SupplierScoreClient) irc31Supplier).setApprovalForAll(
                IRC31IntegrationTest.eventLogChecker(ApprovalForAllEventLog::eventLogs, (el) -> {
                    //caller must be registered owner of OwnerBasedIRC31Supplier
                    assertEquals(caller, el.getOwner());
                    assertEquals(operator, el.getOperator());
                    assertEquals(approved, el.isApproved());
                }),
                operator, approved);
        assertEquals(approved, isApprovalForAll(operator));
    }

    public static boolean isApprovalForAll(Address operator) {
        return irc31Supplier.isApprovedForAll(caller, operator);
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        if(isApprovalForAll(operator)) {
            System.out.println("clear approvedForAll operator:"+operator);
            setApprovalForAll(operator, false);
        }
    }

    @Test
    void transferFrom() {
        mint(owner, id, value);

        transferFrom(owner, Address.of(tester), id, value);
    }

    @Test
    void transferFromBatch() {
        mintBatch(owner, ids, values);

        transferFromBatch(owner, Address.of(tester), ids, values);
    }

    @Test
    void setApprovalForAll() {
        setApprovalForAll(operator, true);
    }

}
