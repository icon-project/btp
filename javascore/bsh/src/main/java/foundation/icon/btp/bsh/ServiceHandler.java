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

package foundation.icon.btp.bsh;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import scorex.util.ArrayList;
import java.math.BigInteger;
import java.util.NoSuchElementException;

public class ServiceHandler {
    final static String RLPn = "RLPn";

    private static final int MIN_OWNER_COUNT = 1;

    private static final int REQUEST_TOKEN_TRANSFER = 0;
    private static final int REQUEST_TOKEN_REGISTER = 1;
    private static final int RESPONSE_HANDLE_SERVICE = 2;
    private static final int RESPONSE_UNKNOWN_ = 3;

    private static final int BD_USABLE_IDX = 0;
    private static final int BD_LOCKED_IDX = 1;
    private static final int BD_REFUNDABLE_IDX = 2;
    private static final int RC_OK = 0;
    private static final int RC_ERR_UNREGISTERED_TOKEN = -1;
    private static float FEE_PERCENT = 0.1f;
    private final DictDB<String, String> tokenAddrDb = Context.newDictDB("token_addr", String.class);
    private final ArrayDB<String> tokenNameDb = Context.newArrayDB("token_name", String.class);
    private final VarDB<BigInteger> snDb = Context.newVarDB("sn", BigInteger.class);
    private final VarDB<Address> bmcDb = Context.newVarDB("bmc", Address.class);
    private final DictDB<BigInteger, byte[]> pendingDb = Context.newDictDB("pending", byte[].class);
    BranchDB<Address, DictDB<String, BigInteger>> usuableBalances = Context.newBranchDB("usuableBalances", BigInteger.class);
    BranchDB<Address, DictDB<String, BigInteger>> lockedBalances = Context.newBranchDB("lockedBalances", BigInteger.class);
    BranchDB<Address, DictDB<String, BigInteger>> refundableBalances = Context.newBranchDB("refundableBalances", BigInteger.class);
    ArrayList<Address> ownersDb = new ArrayList<>();
    Address feeAggregationScore;

    public ServiceHandler() {
        //register the BMC link for this BSH
        //bmcDb.set(bmc);
    }

    /**
     * @param address address of an owner for BSH to add
     */
    @External
    public void addOwner(Address address) {
        onlyOwner();
        ownersDb.add(address);
        AddOwner(address);
    }

    /**
     * @param address address of an owner for BSH to remove
     */
    @External
    public void removeOwner(Address address) {
        onlyOwner();
        checkMinOwners();
        ownersDb.remove(address);
        RemoveOwner(address);
    }


    private void onlyOwner() {
        assert (Context.getCaller() != null);
        Context.require(Context.getOwner().equals(Context.getCaller()) || isAnOwner(Context.getCaller()));
    }

    private void checkMinOwners() {
        Context.require(ownersDb.size() >= MIN_OWNER_COUNT);
    }

    private boolean isAnOwner(Address caller) {
        return ownersDb.contains(caller);
    }


    @External
    public void addFeeAggregationScore(Address address) {
        onlyOwner();
        feeAggregationScore = address;
        AddFeeAggregationScore(address);
    }

    @External
    public void updateFeeAggregationScore(Address address) {
        onlyOwner();
        feeAggregationScore = address;
        UpdateFeeAggregationScore(address);
    }

    /**
     * @param percent percentage of fees in String format, converted internally to float
     */
    @External
    public void setFeePercent(String percent) {
        onlyOwner();
        FEE_PERCENT = Float.parseFloat(percent);
        SetFeePercent(percent);
    }

    /**
     * @param name    name of the token
     * @param address Address of the token contract
     */

    @External
    public void register(String name, Address address) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        onlyOwner();
        if (tokenAddrDb.get(name) != null) {
            throw new UserRevertedException("Duplicated token name");
        }
        tokenAddrDb.set(name, address.toString());
        tokenNameDb.add(name);
    }

    /**
     * Works as a deposit function from IRC2 token
     *
     * @param from  Address of the token transfer request sender
     * @param value amount of tokens to transfer
     * @param data  serialized data
     */
    @External
    public void tokenFallback(Address from, BigInteger value, @Optional byte[] data) {
        if (value.compareTo(BigInteger.ZERO) == -1) {
            throw new UserRevertedException("Invalid Amount");
        }
        String targetName = null;
        for (int i = 0; i < tokenNameDb.size(); i++) {
            if (Context.getCaller().toString().compareTo(tokenAddrDb.get(tokenNameDb.get(i))) == 0) {
                targetName = tokenNameDb.get(i);
                break;
            }
        }
        if (targetName == null || targetName.equals("")) {
            //throw new UserRevertedException("Token not registered");
        }

        setBalance(from, targetName, value, BigInteger.ZERO);
    }

    /**
     * @param tokenName Name of the token to transfer
     * @param to        String Address of the receiver
     * @param value     Amount to transfer
     */
    @External
    public void transfer(String tokenName, String to, BigInteger value) {
        String tokenAddr = this.tokenAddrDb.getOrDefault(tokenName, null);
        if (tokenAddr == null) {
            throw new UserRevertedException("Token not registered");
        }
        if (value.compareTo(BigInteger.ZERO) == -1) {
            throw new UserRevertedException("Not allowed amount value");
        }
        //Address _to = Address.fromString(to);
        Address sender = Context.getCaller();
        BigInteger[] balance = getBalance(sender, tokenName);
        if (balance[BD_USABLE_IDX].compareTo(value) == -1) {
            throw new UserRevertedException("Overdrawn");
        }
        setBalance(sender, tokenName, value.negate(), value);
        BigInteger sn = generateSerialNumber();
        byte[] msg = createMessage(REQUEST_TOKEN_TRANSFER, sender.toString(), to, tokenName, value);
        putPending(sn, msg);
        //TODO: call sendMessage method to send it to BMC
        Context.println("################# BMC.SendMessage initiated");
        TransferStart(sender, tokenName, sn, value, to);
    }

    /**
     * Handles BTP Messages from other blockchains, accepts messages only from BMC. If it fails,
     * then BMC will generate a BTP Message that includes error information, then delivered to the source.
     *
     * @param from Network Address of source network / blockchain
     * @param svc  Service Name
     * @param sn   Serial Number of the message
     * @param msg  Serialised byte of service message (from, to, tokenName, value) in order
     */
    @External
    public void handleBTPMessage(String from, String svc, BigInteger sn, byte[] msg) {

        if (!Context.getCaller().equals(bmcDb.get())) {
            //TODO: Not allowed caller
        }
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, msg);
        reader.beginList();
        int actionType = reader.readInt();
        Context.println("[Exception] " + actionType);
        if (actionType == REQUEST_TOKEN_TRANSFER) {
            reader.beginList();
            Address dataFrom = Address.fromString(reader.readString());
            Address dataTo = Address.fromString(reader.readString());
            String tokenName = reader.readString();
            int value = reader.readInt();
            reader.end();
            reader.end();
            String tokenAddr = this.tokenAddrDb.getOrDefault(tokenName, null);
            int code = RC_OK;
            if (tokenAddr != null) {
                setBalance(dataTo, tokenName, BigInteger.valueOf(value), BigInteger.ZERO);
            } else {
                code = RC_ERR_UNREGISTERED_TOKEN;
            }
            // send response message for `req_token_transfer`
            byte[] res = createMessage(RESPONSE_HANDLE_SERVICE, code);
            //TODO: call sendMessage method to send it to BMC
        } else if (actionType == RESPONSE_HANDLE_SERVICE) {
            if (!hasPending(sn)) {
                throw new NoSuchElementException("No pending message");
            }

            byte[] pmsg = getPending(sn);
            ObjectReader pmsgReader = Context.newByteArrayObjectReader(RLPn, pmsg);
            pmsgReader.beginList();
            Address pmsgFrom = Address.fromString(pmsgReader.readString());
            Address pmsgTo = Address.fromString(pmsgReader.readString());
            String pmsgTokenName = pmsgReader.readString();
            int pmsgValue = pmsgReader.readInt();
            pmsgReader.end();
            pmsgReader.end();
            int code = reader.readInt();
            if (code == RC_OK) {
                setBalance(pmsgFrom, pmsgTokenName, BigInteger.ZERO, BigInteger.valueOf(pmsgValue).negate());
            } else {
                setBalance(pmsgFrom, pmsgTokenName, BigInteger.valueOf(pmsgValue), BigInteger.valueOf(pmsgValue).negate());
            }
            // delete pending message
            deletePending(sn);
            TransferEnd(sn, code, msg);
        } else {
            byte[] res = createMessage(RESPONSE_UNKNOWN_);
            //TODO: call sendMessage method to send it to BMC

        }
    }

    /**
     * @param src     Source
     * @param service Service Name
     * @param sn      Serial Number
     * @param code    Error Code
     * @param msg     Serialized Message (from, to, tokenName, value) in order
     */
    @External
    public void handleBTPError(String src, String service, BigInteger sn, int code, String msg) {
        //TODO: not allowed caller
        //TODO: no pending message
        byte[] pmsg = getPending(sn);
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, pmsg);
        reader.beginList();
        int actionType = reader.readInt();
        // ROllback token transfer
        if (actionType == REQUEST_TOKEN_TRANSFER) {
            Address from = Address.fromString(reader.readString());
            Address to = Address.fromString(reader.readString());
            String tokenName = reader.readString();
            int value = reader.readInt();
            reader.end();
            setBalance(from, tokenName, BigInteger.valueOf(value), BigInteger.valueOf(value).negate());
        }
        // delete pending message
        deletePending(sn);
    }

    @External
    public String[] tokenNames() {
        String[] tokenNames = new String[tokenNameDb.size()];
        for (int i = 0; i < tokenNameDb.size(); i++) {
            tokenNames[i] = tokenNameDb.get(i);
        }
        return tokenNames;
    }

    @External(readonly = true)
    public int numberOfTokens() {
        return tokenNameDb.size();
    }


    @External(readonly = true)
    public BigInteger[] getBalance(Address user, String tokenName) {
        //TODO: check the tokenName involvement in the logic again
        BigInteger[] out = new BigInteger[3];
        out[BD_USABLE_IDX] = usuableBalances.at(user).getOrDefault(tokenName, BigInteger.ZERO);
        out[BD_LOCKED_IDX] = lockedBalances.at(user).getOrDefault(tokenName, BigInteger.ZERO);
        return out;
    }

    @External
    public void setBalance(Address user, String tokenName, BigInteger usable, BigInteger locked) {
        BigInteger[] newBalance = getBalance(user, tokenName);
        newBalance[BD_USABLE_IDX] = newBalance[BD_USABLE_IDX].add(usable);
        newBalance[BD_LOCKED_IDX] = newBalance[BD_LOCKED_IDX].add(locked);
        if (newBalance[BD_USABLE_IDX].compareTo(BigInteger.ZERO) == 0 && newBalance[BD_LOCKED_IDX].compareTo(BigInteger.ZERO) == 0) {
            //balanceDb.set(user,null);
            Context.println("########### User Balance Invalid. removing from the db:" + user);
            //TODO: check/implement to remove the token from DB
        } else {
            usuableBalances.at(user).set(tokenName, newBalance[BD_USABLE_IDX]);
            lockedBalances.at(user).set(tokenName, newBalance[BD_LOCKED_IDX]);
        }
    }

    private BigInteger generateSerialNumber() {
        BigInteger newSnNo = snDb.getOrDefault(BigInteger.ZERO).add(BigInteger.ONE);
        snDb.set(newSnNo);
        return newSnNo;
    }

    private void putPending(BigInteger sn, byte[] msg) {
        pendingDb.set(sn, msg);
    }

    private byte[] getPending(BigInteger sn) {
        return pendingDb.get(sn);
    }

    private boolean hasPending(BigInteger sn) {
        return pendingDb.get(sn) != null;
    }

    private void deletePending(BigInteger sn) {
        pendingDb.set(sn, null);
    }

    private byte[] createMessage(int type, Object... args) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        if (type == REQUEST_TOKEN_TRANSFER) {
            writer.beginList(2);
            writer.write(REQUEST_TOKEN_TRANSFER);//ActionType
            //Action Data writer -start
            writer.beginList(4);
            writer.write((String) args[0]);
            writer.write((String) args[1]);
            writer.write((String) args[2]);
            writer.write((BigInteger) args[3]);
            writer.end();
            //Action Data - end
            writer.end();
        } else if (type == RESPONSE_HANDLE_SERVICE) {
            writer.beginList(2);
            writer.write(RESPONSE_HANDLE_SERVICE);//ActionType
            writer.write((int) args[0]);//Code
            writer.end();
        } else if (type == RESPONSE_UNKNOWN_) {
            writer.beginList(1);
            writer.write(RESPONSE_UNKNOWN_);//ActionType
            writer.end();
        }
        return writer.toByteArray();
    }

    @EventLog(indexed = 2)
    public void TransferStart(Address from, String tokenName, BigInteger sn, BigInteger value, String to) {
    }

    @EventLog(indexed = 1)
    protected void TransferEnd(BigInteger sn, int code, byte[] msg) {
    }

    @EventLog(indexed = 1)
    protected void AddOwner(Address owner) {
    }

    @EventLog(indexed = 1)
    protected void RemoveOwner(Address owner) {
    }

    @EventLog(indexed = 1)
    protected void AddFeeAggregationScore(Address owner) {
    }

    @EventLog(indexed = 1)
    protected void UpdateFeeAggregationScore(Address owner) {
    }

    @EventLog(indexed = 1)
    protected void SetFeePercent(String owner) {
    }
}
