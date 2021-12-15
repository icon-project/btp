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

package foundation.icon.btp.nativecoinIRC2;

import foundation.icon.btp.lib.BSH;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerImpl;
import foundation.icon.score.util.ArrayUtil;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class NativeCoinService implements NCSEvents, BSH, OwnerManager {
    private static final Logger logger = Logger.getLogger(NativeCoinService.class);

    public static final String SERVICE = "nativecoin";
    public static final BigInteger NATIVE_COIN_ID = BigInteger.ZERO;
    public static final BigInteger FEE_DENOMINATOR = BigInteger.valueOf(10000);
    protected static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);

    //
    private final Address bmc;
    private final String net;
    private final Address irc2;
    private final String name;
    private final String tokenName;
    private final VarDB<NCSProperties> properties = Context.newVarDB("properties", NCSProperties.class);

    //
    private final OwnerManager ownerManager = new OwnerManagerImpl("owners");

    //
    private final ArrayDB<String> coinNames = Context.newArrayDB("coinNames", String.class);
    private final BranchDB<String, DictDB<Address, Balance>> balances = Context.newBranchDB("balances", Balance.class);
    private final DictDB<String, BigInteger> feeBalances = Context.newDictDB("feeBalances", BigInteger.class);
    private final DictDB<BigInteger, TransferTransaction> transactions = Context.newDictDB("transactions", TransferTransaction.class);

    public NativeCoinService(Address _bmc, Address _irc2, String _name, String _tokenName) {
        bmc = _bmc;
        BMCScoreInterface bmcInterface = new BMCScoreInterface(bmc);
        //BTPAddress btpAddress = BTPAddress.valueOf(bmcInterface.getBtpAddress());
        String btpAddr = (String) Context.call(bmcInterface._getAddress(), "getBtpAddress");
        BTPAddress btpAddress = BTPAddress.valueOf(btpAddr);
        net = btpAddress.net();
        irc2 = _irc2;
        name = _name;
        tokenName = _tokenName;
        coinNames.add(_tokenName);
    }

    public NCSProperties getProperties() {
        return properties.getOrDefault(NCSProperties.DEFAULT);
    }

    public void setProperties(NCSProperties properties) {
        this.properties.set(properties);
    }

    private boolean isRegistered(String name) {
        if (tokenName.equals(name)) {
            return true;
        }
        return false;
    }

    static void require(boolean condition, String message) {
        if (!condition) {
            throw NCSException.unknown(message);
        }
    }

    @External(readonly = true)
    public String[] coinNames() {
        String[] names = new String[2];
        names[0] = name;
        names[1] = tokenName;
        return names;
    }

    @External(readonly = true)
    public Balance balanceOf(Address _owner, String _coinName) {
        if (_owner.equals(Context.getAddress())) {
            Balance balance = new Balance(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
            balance.setRefundable(feeBalances.getOrDefault(_coinName, BigInteger.ZERO));
            return balance;
        } else {
            return getBalance(_coinName, _owner);
        }
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
            transfer(irc2, owner, _value, "Reclaim: transferring to Caller".getBytes());
        }
    }

    @External
    public void tokenFallback(Address from, BigInteger value, @Optional byte[] data) {
        require(value.compareTo(BigInteger.ZERO) > 0, "Invalid Amount");
        require(Context.getCaller() == irc2, "Invalid Token Address");
        require(tokenName != null && !tokenName.equals(""), "Token not registered");
        deposit(tokenName, from, value);
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
        require(!name.equals(_coinName) && isRegistered(_coinName), "Not supported Token");

        Address owner = Context.getCaller();
        Balance balance = getBalance(_coinName, owner);
        require(balance.getUsable().compareTo(_value) >= 0, "Overdrawn: Insufficient Balance");
        sendRequest(owner, BTPAddress.valueOf(_to), List.of(_coinName), List.of(_value));
    }

    @EventLog(indexed = 1)
    public void TransferStart(Address _from, String _to, BigInteger _sn, byte[] _assetDetails) {
    }

    @EventLog(indexed = 1)
    public void TransferEnd(Address _from, BigInteger _sn, BigInteger _code, byte[] _msg) {
    }

    @EventLog(indexed = 1)
    public void UnknownResponse(String _from, BigInteger _sn) {
    }

    @External(readonly = true)
    public TransferTransaction getTransaction(BigInteger _sn) {
        return transactions.get(_sn);
    }

    private void sendRequest(Address owner, BTPAddress to, List<String> coinNames, List<BigInteger> amounts) {
        logger.println("sendRequest", "begin");
        NCSProperties properties = getProperties();

        BigInteger feeRatio = properties.getFeeRatio();
        if (owner.equals(Context.getAddress())) {
            feeRatio = BigInteger.ZERO;
        }
        int len = coinNames.size();
        AssetTransferDetail[] assetTransferDetails = new AssetTransferDetail[len];
        Asset[] assets = new Asset[len];
        for (int i = 0; i < len; i++) {
            String coinName = coinNames.get(i);
            BigInteger amount = amounts.get(i);
            AssetTransferDetail assetTransferDetail = newAssetTransferDetail(coinName, amount, feeRatio);
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
        transaction.setTo(to.toString());
        transaction.setAssets(assetTransferDetails);

        BigInteger sn = properties.getSn().add(BigInteger.ONE);
        properties.setSn(sn);
        setProperties(properties);
        transactions.set(sn, transaction);

        sendMessage(to.net(), NCSMessage.REQUEST_COIN_TRANSFER, sn, request.toBytes());
        TransferStart(owner, to.toString(), sn, encode(assetTransferDetails));
        logger.println("sendRequest", "end");
    }

    static byte[] encode(AssetTransferDetail[] assetTransferDetails) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(assetTransferDetails.length);
        for (AssetTransferDetail v : assetTransferDetails) {
            //writer.write(v);
            AssetTransferDetail.writeObject(writer, v);
        }
        writer.end();
        return writer.toByteArray();
    }

    private void sendMessage(String to, int serviceType, BigInteger sn, byte[] data) {
        logger.println("sendMessage", "begin");
        NCSMessage message = new NCSMessage();
        message.setServiceType(serviceType);
        message.setData(data);
        Context.call(bmc, "sendMessage", to, SERVICE, sn, message.toBytes());
        logger.println("sendMessage", "end");
    }

    private void responseSuccess(String to, BigInteger sn) {
        TransferResponse response = new TransferResponse();
        response.setCode(TransferResponse.RC_OK);
        response.setMessage(TransferResponse.OK_MSG);
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
            UnknownResponse(_from, _sn);
        } else {
            //  If none of those types above, BSH responds a message of RES_UNKNOWN_TYPE
            TransferResponse response = new TransferResponse();
            response.setCode(TransferResponse.RC_ERR);
            response.setMessage(TransferResponse.ERR_MSG_UNKNOWN_TYPE);
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
    public void handleFeeGathering(String _fa, String _svc) {
        require(Context.getCaller().equals(bmc), "Only BMC");
        BTPAddress from = BTPAddress.valueOf(_fa);
        Address owner = Context.getAddress();

        List<String> coinNames = new ArrayList<>();
        List<BigInteger> feeAmounts = new ArrayList<>();
        for (String coinName : coinNames()) {
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
                _transferBatch(fa, ArrayUtil.toStringArray(coinNames), ArrayUtil.toBigIntegerArray(feeAmounts));
            } else {
                sendRequest(owner, from, coinNames, feeAmounts);
            }
        }
    }

    private void _transferBatch(Address to, String[] coinNames, BigInteger[] amounts) {
        logger.println("transferBatch", to, StringUtil.toString(coinNames), StringUtil.toString(amounts));
        for (int i = 0; i < coinNames.length; i++) {
            _transfer(irc2, to, amounts[i]);
        }
    }

    private Balance getBalance(String coinName, Address owner) {
        Balance balance = balances.at(coinName).get(owner);
        if (balance == null) {
            balance = new Balance(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
            return balance;
        }
        Balance newBal = new Balance(balance.getUsable(), balance.getLocked(), balance.getRefundable());
        return newBal;
    }

    private void setBalance(String coinName, Address owner, Balance balance) {
        Balance newBalance = new Balance(balance.getUsable(), balance.getLocked(), balance.getRefundable());
        balances.at(coinName).set(owner, newBalance);
    }

    private void lock(String coinName, Address owner, BigInteger value) {
        logger.println("lock", "coinName:", coinName, "owner:", owner, "value:", value);
        Balance balance = getBalance(coinName, owner);
        balance.setUsable(balance.getUsable().subtract(value));
        balance.setLocked(balance.getLocked().add(value));
        setBalance(coinName, owner, balance);
    }

    private void unlock(String coinName, Address owner, BigInteger value) {
        logger.println("unlock", "coinName:", coinName, "owner:", owner, "value:", value);
        Balance balance = getBalance(coinName, owner);
        balance.setLocked(balance.getLocked().subtract(value));
        setBalance(coinName, owner, balance);
    }

    private void refund(String coinName, Address owner, BigInteger value) {
        logger.println("refund", "coinName:", coinName, "owner:", owner, "value:", value);
        //unlock and add refundable
        Balance balance = getBalance(coinName, owner);
        balance.setLocked(balance.getLocked().subtract(value));
        if (!owner.equals(Context.getAddress())) {
            balance.setRefundable(balance.getRefundable().add(value));
        }
        setBalance(coinName, owner, balance);
    }

    private void deposit(String coinName, Address owner, BigInteger value) {
        logger.println("deposit", "coinName:", coinName, "owner:", owner, "value:", value);
        Balance balance = getBalance(coinName, owner);
        balance.setUsable(balance.getUsable().add(value));
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
        logger.println("handleRequest", "begin", "sn:", sn);
        Address to;
        try {
            to = Address.fromString(request.getTo());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw NCSException.unknown(e.getMessage());
        }

        BigInteger nativeCoinTransferAmount = null;
        Asset[] assets = request.getAssets();
        for (Asset asset : assets) {
            String coinName = asset.getCoinName();
            BigInteger amount = asset.getAmount();
            if (amount == null || amount.compareTo(BigInteger.ZERO) < 1) {
                throw NCSException.unknown("Amount must be positive value");
            }

            if (tokenName.equals(coinName)) {
                _transfer(irc2, to, amount);
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
                throw NCSException.unknown("fail to transfer err:" + e.getMessage());
            }
        }

        logger.println("handleRequest", "responseSuccess");
        responseSuccess(from, sn);
        logger.println("handleRequest", "end");
    }

    private void handleResponse(BigInteger sn, TransferResponse response) {
        logger.println("handleResponse", "begin", "sn:", sn);
        TransferTransaction transaction = transactions.get(sn);
        // ignore when not exists pending request
        if (transaction != null) {
            BigInteger code = response.getCode();
            Address owner = Address.fromString(transaction.getFrom());
            AssetTransferDetail[] assets = transaction.getAssets();

            logger.println("handleResponse", "code:", code);
            if (TransferResponse.RC_OK.equals(code)) {
                for (AssetTransferDetail asset : assets) {
                    String coinName = asset.getCoinName();
                    BigInteger amount = asset.getAmount();
                    BigInteger fee = asset.getFee();
                    BigInteger locked = amount.add(fee);
                    boolean isNativeCoin = name.equals(coinName);
                    if (isNativeCoin || tokenName.equals(coinName)) {
                        unlock(coinName, owner, locked);
                        addFee(coinName, fee);
                        if (!isNativeCoin) {
                            _burn(irc2, amount);
                        }
                    } else {
                        //This should not happen
                        throw NCSException.unknown("invalid transaction, invalid coinName");
                    }
                }
            } else {
                for (AssetTransferDetail asset : assets) {
                    String coinName = asset.getCoinName();
                    BigInteger amount = asset.getAmount();
                    BigInteger fee = asset.getFee();
                    BigInteger locked = amount.add(fee);
                    boolean isNativeCoin = name.equals(coinName);
                    if (isNativeCoin || tokenName.equals(coinName)) {
                        refund(coinName, owner, locked);
                    } else {
                        //This should not happen
                        throw NCSException.unknown("invalid transaction, invalid coinName");
                    }
                }
            }

            transactions.set(sn, null);
            TransferEnd(owner, sn, code, response.getMessage() != null ? response.getMessage().getBytes() : null);
        }
        logger.println("handleResponse", "end");
    }

    @External
    public void setFeeRatio(BigInteger _feeNumerator) {
        requireOwnerAccess();
        require(_feeNumerator.compareTo(BigInteger.ONE) >= 0 &&
                        _feeNumerator.compareTo(FEE_DENOMINATOR) < 0,
                "The feeNumetator should be less than FEE_DEMONINATOR and greater than 1");
        NCSProperties properties = getProperties();
        properties.setFeeRatio(_feeNumerator);
        setProperties(properties);
    }

    @External(readonly = true)
    public BigInteger feeRatio() {
        NCSProperties properties = getProperties();
        return properties.getFeeRatio();
    }

    private AssetTransferDetail newAssetTransferDetail(String coinName, BigInteger amount, BigInteger feeRatio) {
        logger.println("newAssetTransferDetail", "begin");
        BigInteger fee = amount.multiply(feeRatio).divide(FEE_DENOMINATOR);
        if (feeRatio.compareTo(BigInteger.ZERO) > 0 && fee.compareTo(BigInteger.ZERO) == 0) {
            fee = BigInteger.ONE;
        }
        BigInteger transferAmount = amount.subtract(fee);
        logger.println("newAssetTransferDetail", "amount:", amount, "fee:", fee);
        if (transferAmount.compareTo(BigInteger.ZERO) < 1) {
            throw NCSException.unknown("not enough value");
        }
        AssetTransferDetail asset = new AssetTransferDetail();
        asset.setCoinName(coinName);
        asset.setAmount(transferAmount);
        asset.setFee(fee);
        logger.println("newAssetTransferDetail", "end");
        return asset;
    }

    private void _transfer(Address token, Address to, BigInteger amount) {
        logger.println("transfer", to, amount);
        try {
            transfer(token, to, amount, "transfer to Receiver".getBytes());
        } catch (UserRevertedException e) {
            logger.println("transfer", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("transfer", "Exception:", e.toString());
            throw NCSException.irc31Failure("Exception:" + e);
        }
    }

    private void _burn(Address token, BigInteger amount) {
        logger.println("burn", ZERO_ADDRESS, amount);
        try {
            transfer(token, ZERO_ADDRESS, amount, "Burn Transfer to Zero Address".getBytes());
        } catch (UserRevertedException e) {
            logger.println("burn", "code:", e.getCode(), "msg:", e.getMessage());
            throw NCSException.irc31Reverted("code:" + e.getCode() + "msg:" + e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("burn", "Exception:", e.toString());
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
    public Address[] getOwners() {
        return ownerManager.getOwners();
    }

    @External(readonly = true)
    public boolean isOwner(Address _addr) {
        return ownerManager.isOwner(_addr);
    }

    public void transfer(Address tokenAddr, Address _to, BigInteger _value, byte[] _data) {
        Context.call(tokenAddr, "transfer", _to, _value, _data);
    }

}