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

package com.iconloop.btp.nativecoin;

import com.iconloop.btp.lib.*;
import com.iconloop.score.token.irc31.IRC31Receiver;
import com.iconloop.score.token.irc31.IRC31SupplierScoreInterface;
import com.iconloop.score.util.*;
import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class NativeCoinService implements NCS, NCSEvents, IRC31Receiver, BSH, OwnerManager {
    private static final Logger logger = Logger.getLogger(NativeCoinService.class);

    public static final String SERVICE = "nativecoin";
    public static final BigInteger NATIVE_COIN_ID = BigInteger.ZERO;

    //
    private final Address bmc;
    private final String net;
    private final Address irc31;
    private final String name;
    private final VarDB<NCSProperties> properties = Context.newVarDB("properties", NCSProperties.class);

    //
    private final OwnerManager ownerManager = new OwnerManagerImpl("owners");

    //
    private final ArrayDB<String> coinNames = Context.newArrayDB("coinNames", String.class);
    private final BranchDB<String, DictDB<Address, Balance>> balances = Context.newBranchDB("balances", Balance.class);
    private final DictDB<String, BigInteger> feeBalances = Context.newDictDB("feeBalances", BigInteger.class);
    private final DictDB<BigInteger, TransferTransaction> transactions = Context.newDictDB("transactions", TransferTransaction.class);

    public NativeCoinService(Address _bmc, Address _irc31, String _name) {
        bmc = _bmc;
        BMCScoreInterface bmcInterface = new BMCScoreInterface(bmc);
        BTPAddress btpAddress = BTPAddress.valueOf(bmcInterface.getBtpAddress());
        net = btpAddress.net();
        irc31 = _irc31;
        name = _name;
    }

    public NCSProperties getProperties() {
        return properties.getOrDefault(NCSProperties.DEFAULT);
    }

    public void setProperties(NCSProperties properties) {
        this.properties.set(properties);
    }

    private boolean isRegistered(String name) {
        int len = coinNames.size();
        for (int i = 0; i < len; i++) {
            if(coinNames.get(i).equals(name)) {
                return true;
            }
        }
        return false;
    }

    private List<String> getCoinNamesAsList() {
        List<String> coinNames = new ArrayList<>();
        int len = this.coinNames.size();
        for (int i = 0; i < len; i++) {
            coinNames.add(this.coinNames.get(i));
        }
        return coinNames;
    }

    static BigInteger generateTokenId(String name) {
        return new BigInteger(Context.hash("sha3-256", name.getBytes()));
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw NCSException.unknown(message);
        }
    }

    @External
    public void register(String _name) {
        requireOwnerAccess();

        require(!name.equals(_name) && !isRegistered(_name), "already existed");
        coinNames.add(_name);
    }

    @External(readonly = true)
    public String[] coinNames() {
        int len = coinNames.size();
        String[] names = new String[len + 1];
        names[0] = name;
        for (int i = 0; i < len; i++) {
            names[i+1] = coinNames.get(i);
        }
        return names;
    }

    @External(readonly = true)
    public BigInteger coinId(String _coinName) {
        if (name.equals(_coinName)) {
            return NATIVE_COIN_ID;
        } else if (isRegistered(_coinName)) {
            return generateTokenId(_coinName);
        }
        //FIXME throw NotExists? or null?
        return null;
    }

    @External(readonly = true)
    public Balance balanceOf(Address _owner, String _coinName) {
        return getBalance(_coinName, _owner);
    }

    @External(readonly = true)
    public Balance[] balanceOfBatch(Address _owner, String[] _coinNames) {
        int len = _coinNames.length;
        Balance[] balances = new Balance[len];
        for (int i = 0; i < len; i++) {
            balances[i] = getBalance(_coinNames[i], _owner);
        }
        return balances;
    }

    @External
    public void reclaim(String _coinName, BigInteger _value) {
        require(_value.compareTo(BigInteger.ZERO) > 0, "_value must be positive");

        Address owner = Context.getCaller();
        Balance balance = getBalance(_coinName, owner);
        require(balance.getRefundable().compareTo(_value) > -1, "invalid value");
        balance.setRefundable(balance.getRefundable().subtract(_value));
        setBalance(_coinName, owner, balance);

        if (name.equals(_coinName)) {
            Context.transfer(owner, _value);
        } else {
            transferFrom(Context.getAddress(), owner, generateTokenId(_coinName), _value);
        }
    }

    @Payable
    @External
    public void transferNativeCoin(String _to) {
        BigInteger value = Context.getValue();
        require(value != null && value.compareTo(BigInteger.ZERO) > 0, "Invalid amount");
        sendRequest(Context.getCaller(), BTPAddress.valueOf(_to), List.of(name), List.of(value));
    }

    @External
    public void transfer(String _coinName, BigInteger _value, String _to) {
        require(_value != null && _value.compareTo(BigInteger.ZERO) > 0, "Invalid amount");
        //TODO TBD nativeCoin check is not exists in solidity
        require(!name.equals(_coinName) && isRegistered(_coinName), "Not supported Token");

        Address owner = Context.getCaller();
        transferFrom(owner, Context.getAddress(), generateTokenId(_coinName), _value);
        sendRequest(owner, BTPAddress.valueOf(_to), List.of(_coinName), List.of(_value));
    }

    @Payable
    @External
    public void transferBatch(String[] _coinNames, BigInteger[] _values, String _to) {
        require(_coinNames.length == _values.length, "Invalid arguments");

        List<String> registeredCoinNames = getCoinNamesAsList();
        List<String> coinNames = new ArrayList<>();
        List<BigInteger> values = new ArrayList<>();
        int len = _coinNames.length;
        BigInteger[] ids = new BigInteger[len];
        for (int i = 0; i < len; i++) {
            String coinName = _coinNames[i];
            require(!name.equals(coinName) && registeredCoinNames.contains(coinName), "Not supported Token");
            coinNames.add(coinName);
            values.add(_values[i]);
            ids[i] = generateTokenId(coinName);
        }

        Address owner = Context.getCaller();
        transferFromBatch(owner, Context.getAddress(), ids, _values);

        BigInteger value = Context.getValue();
        if (value != null && value.compareTo(BigInteger.ZERO) > 0) {
            coinNames.add(name);
            values.add(value);
        }
        sendRequest(owner, BTPAddress.valueOf(_to), coinNames, values);
    }

    @EventLog(indexed = 1)
    public void TransferStart(Address _from, String _to, BigInteger _sn, byte[] _assets) {}

    @EventLog(indexed = 1)
    public void TransferEnd(Address _from, BigInteger sn, long _code, String _response) {}

    @External(readonly = true)
    public TransferTransaction getTransaction(BigInteger _sn) {
        return transactions.get(_sn);
    }

    private void sendRequest(Address owner, BTPAddress to, List<String> coinNames, List<BigInteger> amounts) {
        logger.println("sendRequest","begin");
        NCSProperties properties = getProperties();
        double feeRate = properties.getFeeRate();
        int len = coinNames.size();
        AssetTransferDetail[] assetTransferDetails = new AssetTransferDetail[len];
        Asset[] assets = new Asset[len];
        for (int i = 0; i < len; i++) {
            String coinName = coinNames.get(i);
            BigInteger amount = amounts.get(i);
            AssetTransferDetail assetTransferDetail = newAssetTransferDetail(coinName, amount, feeRate);
            lock(coinName, owner, amount);
            assetTransferDetails[i] = assetTransferDetail;
            assets[i] = new Asset(assetTransferDetail);
        }

        TransferRequest request = new TransferRequest();
        request.setFrom(owner.toString());
        request.setTo(to.account());
        request.setAssets(assets);

        TransferTransaction transaction = new TransferTransaction();
        transaction.setFrom(owner.toString());
        transaction.setTo(to.account());
        transaction.setAssets(assetTransferDetails);

        BigInteger sn = properties.getSn().add(BigInteger.ONE);
        properties.setSn(sn);
        setProperties(properties);
        transactions.set(sn, transaction);

        sendMessage(to.net(), NCSMessage.REQUEST_COIN_TRANSFER, sn, request.toBytes());
        TransferStart(owner, to.account(), sn, encode(assetTransferDetails));
        logger.println("sendRequest","end");
    }

    static byte[] encode(AssetTransferDetail[] assetTransferDetails) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(assetTransferDetails.length);
        for(AssetTransferDetail v : assetTransferDetails) {
            writer.write(v);
        }
        writer.end();
        return writer.toByteArray();
    }

    private void sendMessage(String to, int serviceType, BigInteger sn, byte[] data) {
        logger.println("sendMessage","begin");
        NCSMessage message = new NCSMessage();
        message.setServiceType(serviceType);
        message.setData(data);

        BMCScoreInterface bmc = new BMCScoreInterface(this.bmc);
        bmc.sendMessage(to, SERVICE, sn, message.toBytes());
        logger.println("sendMessage","end");
    }

    private void responseSuccess(String to, BigInteger sn) {
        TransferResponse response = new TransferResponse();
        response.setCode(TransferResponse.RC_OK);
        response.setMessage("Transfer Success");
        sendMessage(to, NCSMessage.REPONSE_HANDLE_SERVICE, sn, response.toBytes());
    }

    private void responseError(String to, BigInteger sn, String message) {
        TransferResponse response = new TransferResponse();
        response.setCode(TransferResponse.RC_ERR);
        response.setMessage(message);
        sendMessage(to, NCSMessage.REPONSE_HANDLE_SERVICE, sn, response.toBytes());
    }

    @External
    public void handleBTPMessage(String _from, String _svc, BigInteger _sn, byte[] _msg) {
        require(Context.getCaller().equals(bmc), "Only BMC");

        NCSMessage message = NCSMessage.fromBytes(_msg);
        int serviceType = message.getServiceType();
        if (serviceType == NCSMessage.REQUEST_COIN_TRANSFER) {
            TransferRequest request = TransferRequest.fromBytes(message.getData());
            handleRequest(request, _from, _sn);
        } else if (serviceType == NCSMessage.REPONSE_HANDLE_SERVICE) {
            TransferResponse response = TransferResponse.fromBytes(message.getData());
            handleResponse(_sn, response);
        } else if (serviceType == NCSMessage.UNKNOWN_TYPE) {
            //  If receiving a RES_UNKNOWN_TYPE, ignore this message
            //  or re-send another correct message
        } else {
            //  If none of those types above, BSH responds a message of RES_UNKNOWN_TYPE
            TransferResponse response = new TransferResponse();
            response.setCode(TransferResponse.RC_ERR);
            response.setMessage("UNKNOWN_TYPE");
            sendMessage(_from, NCSMessage.UNKNOWN_TYPE, _sn, response.toBytes());
        }
    }

    @External
    public void handleBTPError(String _src, String _svc, BigInteger _sn, long _code, String _msg) {
        require(Context.getCaller().equals(bmc), "Only BMC");
        TransferResponse response = new TransferResponse();
        response.setCode(TransferResponse.RC_ERR);
        response.setMessage("BTPError [code:" + _code + ",msg:" + _msg);
        handleResponse(_sn, response);
    }

    @External
    public void handleFeeGathering(String _from, String _svc) {
        require(Context.getCaller().equals(bmc), "Only BMC");
        BTPAddress from = BTPAddress.valueOf(_from);
        Address owner = Context.getAddress();

        List<String> coinNames = new ArrayList<>();
        List<BigInteger> feeAmounts = new ArrayList<>();
        for(String coinName : coinNames()) {
            BigInteger feeAmount = clearFee(coinName);
            if (feeAmount.compareTo(BigInteger.ZERO) > 0) {
                coinNames.add(coinName);
                feeAmounts.add(feeAmount);
            }
        }

        if (coinNames.size() > 0) {
            if (from.net().equals(net)) {
                Address fa = Address.fromString(from.account());
                int idx = coinNames.indexOf(name);
                if (idx >= 0) {
                    coinNames.remove(idx);
                    BigInteger feeAmount = feeAmounts.remove(idx);
                    Context.transfer(fa, feeAmount);
                }
                transferFromBatch(owner, fa, coinNamesToIds(coinNames), ArrayUtil.toBigIntegerArray(feeAmounts));
            } else {
                sendRequest(owner, from, coinNames, feeAmounts);
            }
        }
    }

    private BigInteger[] coinNamesToIds(List<String> coinNames) {
        BigInteger[] ids = new BigInteger[coinNames.size()];
        for (int i = 0; i < coinNames.size(); i++) {
            ids[i] = generateTokenId(coinNames.get(i));
        }
        return ids;
    }

    private Balance getBalance(String coinName, Address owner) {
        Balance balance = balances.at(coinName).get(owner);
        if (balance == null) {
            balance = new Balance();
            balance.setLocked(BigInteger.ZERO);
            balance.setRefundable(BigInteger.ZERO);
        }
        return balance;
    }

    private void setBalance(String coinName, Address owner, Balance balance) {
        balances.at(coinName).set(owner, balance);
    }

    private void lock(String coinName, Address owner, BigInteger value) {
        logger.println("lock","coinName:",coinName,"owner:",owner,"value:",value);
        Balance balance = getBalance(coinName, owner);
        balance.setLocked(balance.getLocked().add(value));
        setBalance(coinName, owner, balance);
    }

    private void unlock(String coinName, Address owner, BigInteger value) {
        logger.println("unlock","coinName:",coinName,"owner:",owner,"value:",value);
        Balance balance = getBalance(coinName, owner);
        balance.setLocked(balance.getLocked().subtract(value));
        setBalance(coinName, owner, balance);
    }

    private void refund(String coinName, Address owner, BigInteger value) {
        logger.println("refund","coinName:",coinName,"owner:",owner,"value:",value);
        //unlock and add refundable
        Balance balance = getBalance(coinName, owner);
        balance.setLocked(balance.getLocked().add(value));
        balance.setRefundable(balance.getRefundable().add(value));
        setBalance(coinName, owner, balance);
    }

    private void addFee(String coinName, BigInteger amount) {
        BigInteger fee = feeBalances.getOrDefault(coinName, BigInteger.ZERO);
        feeBalances.set(coinName, fee.add(amount));
    }

    private BigInteger clearFee(String coinName) {
        BigInteger fee = feeBalances.getOrDefault(coinName, BigInteger.ZERO);
        if (fee.compareTo(BigInteger.ZERO) > 0) {
            feeBalances.set(coinName, BigInteger.ZERO);
        }
        return fee;
    }

    private void handleRequest(TransferRequest request, String from, BigInteger sn) {
        logger.println("handleRequest","begin","sn:",sn);
        Address to;
        try {
            to = Address.fromString(request.getTo());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw NCSException.unknown(e.getMessage());
        }

        BigInteger nativeCoinTransferAmount = null;
        Asset[] assets = request.getAssets();
        List<String> coinNames = new ArrayList<>();
        List<BigInteger> amounts = new ArrayList<>();
        List<String> registeredCoinNames = getCoinNamesAsList();
        for (Asset asset : assets) {
            String coinName = asset.getCoinName();
            BigInteger amount = asset.getAmount();
            if (amount == null || amount.compareTo(BigInteger.ZERO) < 1) {
                throw NCSException.unknown("Amount must be positive value");
            }

            if (registeredCoinNames.contains(coinName)) {
                coinNames.add(coinName);
                amounts.add(amount);
            } else if (name.equals(coinName)) {
                nativeCoinTransferAmount = amount;
            } else {
                throw NCSException.unknown("Invalid Token");
            }
        }

        if (nativeCoinTransferAmount != null) {
            try {
                Context.transfer(to, nativeCoinTransferAmount);
            } catch (Exception e) {
                throw NCSException.unknown("fail to transfer err:"+e.getMessage());
            }
        }

        if (coinNames.size() > 0) {
            mintBatch(to, coinNamesToIds(coinNames), ArrayUtil.toBigIntegerArray(amounts));
        }

        logger.println("handleRequest","responseSuccess");
        responseSuccess(from, sn);
        logger.println("handleRequest","end");
    }

    private void handleResponse(BigInteger sn, TransferResponse response) {
        logger.println("handleResponse","begin","sn:",sn);
        TransferTransaction transaction = transactions.get(sn);
        List<String> registeredCoinNames = getCoinNamesAsList();
        // ignore when not exists pending request
        if (transaction != null) {
            long code = response.getCode();
            Address owner = Address.fromString(transaction.getFrom());
            AssetTransferDetail[] assets = transaction.getAssets();

            logger.println("handleResponse","code:",code);
            if (code == TransferResponse.RC_OK) {
                List<String> coinNames = new ArrayList<>();
                List<BigInteger> amounts = new ArrayList<>();
                for (AssetTransferDetail asset : assets) {
                    String coinName = asset.getCoinName();
                    BigInteger amount = asset.getAmount();
                    BigInteger fee = asset.getFee();
                    BigInteger locked = amount.add(fee);
                    boolean isNativeCoin = name.equals(coinName);
                    if(isNativeCoin || registeredCoinNames.contains(coinName)) {
                        unlock(coinName, owner, locked);
                        addFee(coinName, fee);
                        if (!isNativeCoin) {
                            coinNames.add(coinName);
                            amounts.add(amount);
                        }
                    } else {
                        //This should not happen
                        throw NCSException.unknown("invalid transaction, invalid coinName");
                    }
                }

                if (coinNames.size() > 0) {
                    burnBatch(coinNamesToIds(coinNames), ArrayUtil.toBigIntegerArray(amounts));
                }
            } else {
                for (AssetTransferDetail asset : assets) {
                    String coinName = asset.getCoinName();
                    BigInteger amount = asset.getAmount();
                    BigInteger fee = asset.getFee();
                    BigInteger locked = amount.add(fee);
                    boolean isNativeCoin = name.equals(coinName);
                    if(isNativeCoin || registeredCoinNames.contains(coinName)) {
                        //TODO [TBD] when failure of transfer, NCS try refund in solidity
                        //  but in requirements, user could refund by reclaim only
                        refund(coinName, owner, locked);
                    } else {
                        //This should not happen
                        throw NCSException.unknown("invalid transaction, invalid coinName");
                    }
                }
            }

            transactions.set(sn, null);
            TransferEnd(owner, sn, code, response.getMessage());
        }
        logger.println("handleResponse","end");
    }

    @External
    public void onIRC31Received(Address _operator, Address _from, BigInteger _id, BigInteger _value, byte[] _data) {
        //nothing to do
    }

    @External
    public void onIRC31BatchReceived(Address _operator, Address _from, BigInteger[] _ids, BigInteger[] _values, byte[] _data) {
        //nothing to do
    }

    @External
    public void setFeeRate(String _feeRate) {
        requireOwnerAccess();
        NCSProperties properties = getProperties();
        properties.setFeeRate(Double.parseDouble(_feeRate));
        setProperties(properties);
    }

    @External(readonly = true)
    public String getFeeRate() {
        NCSProperties properties = getProperties();
        return Double.toString(properties.getFeeRate());
    }

    private AssetTransferDetail newAssetTransferDetail(String coinName, BigInteger amount, double feeRate) {
        logger.println("newAssetTransferDetail","begin");
        BigInteger fee = BigIntegerUtil.multiply(amount, feeRate);
        BigInteger transferAmount = amount.subtract(fee);
        logger.println("newAssetTransferDetail","amount:",amount,"fee:",fee);
        if (transferAmount.compareTo(BigInteger.ZERO) < 1) {
            throw NCSException.unknown("not enough value");
        }
        AssetTransferDetail asset = new AssetTransferDetail();
        asset.setCoinName(coinName);
        asset.setAmount(transferAmount);
        asset.setFee(fee);
        logger.println("newAssetTransferDetail","end");
        return asset;
    }

    /* Intercall with IRC31Supplier */
    private void transferFrom(Address from, Address to, BigInteger id, BigInteger amount) {
        logger.println("transferFrom", from, to, id, amount);
        IRC31SupplierScoreInterface irc31 = new IRC31SupplierScoreInterface(this.irc31);
        try {
            irc31.transferFrom(from, to, id, amount, null);
        } catch (UserRevertedException e) {
            logger.println("transferFrom", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("transferFrom", "Exception:", e.toString());
            throw NCSException.irc31Failure("Exception:" + e);
        }
    }

    private void transferFromBatch(Address from, Address to, BigInteger[] ids, BigInteger[] amounts) {
        logger.println("transferFromBatch", from, to, StringUtil.toString(ids), StringUtil.toString(amounts));
        IRC31SupplierScoreInterface irc31 = new IRC31SupplierScoreInterface(this.irc31);
        try {
            irc31.transferFromBatch(from, to, ids, amounts, null);
        } catch (UserRevertedException e) {
            logger.println("transferFromBatch", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("transferFromBatch", "Exception:", e.toString());
            throw NCSException.irc31Failure("Exception:" + e);
        }
    }

    private void mint(Address to, BigInteger id, BigInteger amount) {
        logger.println("mint", to, id, amount);
        IRC31SupplierScoreInterface irc31 = new IRC31SupplierScoreInterface(this.irc31);
        try {
            irc31.mint(to, id, amount);
        } catch (UserRevertedException e) {
            logger.println("mint", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("mint", "Exception:", e.toString());
            throw NCSException.irc31Failure("Exception:" + e);
        }
    }

    private void mintBatch(Address to, BigInteger[] ids, BigInteger[] amounts) {
        logger.println("mintBatch", to, StringUtil.toString(ids), StringUtil.toString(amounts));
        IRC31SupplierScoreInterface irc31 = new IRC31SupplierScoreInterface(this.irc31);
        try {
            irc31.mintBatch(to, ids, amounts);
        } catch (UserRevertedException e) {
            logger.println("mintBatch", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("mintBatch", "Exception:", e.toString());
            throw NCSException.irc31Failure("Exception:" + e);
        }
    }

    private void burn(BigInteger id, BigInteger amount) {
        logger.println("burn", id, amount);
        IRC31SupplierScoreInterface irc31 = new IRC31SupplierScoreInterface(this.irc31);
        try {
            irc31.burn(Context.getAddress(), id, amount);
        } catch (UserRevertedException e) {
            logger.println("burn", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("burn", "Exception:", e.toString());
            throw NCSException.irc31Failure("Exception:" + e);
        }
    }

    private void burnBatch(BigInteger[] ids, BigInteger[] amounts) {
        logger.println("burnBatch", StringUtil.toString(ids), StringUtil.toString(amounts));
        IRC31SupplierScoreInterface irc31 = new IRC31SupplierScoreInterface(this.irc31);
        try {
            irc31.burnBatch(Context.getAddress(), ids, amounts);
        } catch (UserRevertedException e) {
            logger.println("mintBatch", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("mintBatch", "Exception:", e.toString());
            throw NCSException.irc31Failure("Exception:" + e);
        }
    }

    /* Delegate OwnerManager */
    private void requireOwnerAccess() {
        if (!ownerManager.isOwner(Context.getCaller())) {
            throw NCSException.unauthorized("require owner access");
        }
    }

    @External
    public void addOwner(Address _addr) {
        try {
            ownerManager.addOwner(_addr);
        } catch (IllegalStateException e) {
            throw NCSException.unauthorized(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw NCSException.unknown(e.getMessage());
        }
    }

    @External
    public void removeOwner(Address _addr) {
        try {
            ownerManager.removeOwner(_addr);
        } catch (IllegalStateException e) {
            throw NCSException.unauthorized(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw NCSException.unknown(e.getMessage());
        }
    }

    @External(readonly = true)
    public List getOwners() {
        return ownerManager.getOwners();
    }

    @External(readonly = true)
    public boolean isOwner(Address _addr) {
        return ownerManager.isOwner(_addr);
    }

}