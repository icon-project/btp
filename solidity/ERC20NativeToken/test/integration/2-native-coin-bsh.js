const MockBSHPeriphery = artifacts.require("MockBSHPeriphery");
const BSHPeriphery = artifacts.require("BSHPeriphery");
const BSHCore = artifacts.require("BSHCore");
const BMC = artifacts.require("MockBMC");
const Holder = artifacts.require("Holder");
const NotPayable = artifacts.require("NotPayable");
const NonRefundable = artifacts.require("NonRefundable");
const Refundable = artifacts.require("Refundable");
const EncodeMsg = artifacts.require("EncodeMessage");
const { assert, AssertionError } = require('chai');
const truffleAssert = require('truffle-assertions');
const rlp = require('rlp');

let toHex = (buf) => {
    buf = buf.toString('hex');
    if (buf.substring(0, 2) == '0x')
        return buf;
    return '0x' + buf.toString('hex');
};

contract('As a user, I want to send MOVR to ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, nonrefundable, refundable;
    let service = 'Coin/WrappedCoin'; let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    let _net = '1234.iconee'; let _to = 'btp://1234.iconee/0x12345678';
    let RC_OK = 0; let RC_ERR = 1;
    let _native = 'MOVR'; let deposit = 1000000000000;
    let _fee = 10; let _fixed_fee = 500000;
    let REPONSE_HANDLE_SERVICE = 2; let _uri = 'https://github.com/icon-project/btp';
    let _tokenName = 'ICX';
    let _tokenSymbol = 'ICX';
    let _initialSupply = 1;

    let DECIMALS = 18;
    let INITIAL_SUPPLY = web3.utils.toBN(100000) // 100000 tokens
    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.movr');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_native, _fee, _fixed_fee, _tokenName, _tokenSymbol, INITIAL_SUPPLY);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        nonrefundable = await NonRefundable.new();
        refundable = await Refundable.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
    });

    it('Scenario 5: Account client transfers a valid native coin to a side chain', async () => {
        let amount = 600000;
        let account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        let tx = await bsh_core.transferNativeCoin(_to, { from: accounts[0], value: amount });
        let account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        let bsh_coin_balance = await bsh_core.getBalanceOf(bsh_core.address, _native);
        let chargedFee = Math.floor(amount / 1000) + _fixed_fee;

        const transferEvents = await bsh_perif.getPastEvents('TransferStart', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        let event = transferEvents[0].returnValues;
        assert.equal(event._from, accounts[0]);
        assert.equal(event._to, _to);
        assert.equal(event._sn, 1);
        assert.equal(event._assetDetails.length, 1);
        assert.equal(event._assetDetails[0].coinName, 'MOVR');
        assert.equal(event._assetDetails[0].value, amount - chargedFee);
        assert.equal(event._assetDetails[0].fee, chargedFee);

        const linkStatus = await bmc.getStatus(_bmcICON);
        const bmcBtpAddress = await bmc.getBmcBtpAddress();

        const messageEvents = await bmc.getPastEvents('Message', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        event = messageEvents[0].returnValues;
        assert.equal(event._next, _bmcICON);
        assert.equal(event._seq, linkStatus.txSeq);

        const bmcMsg = rlp.decode(event._msg);

        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[0])), bmcBtpAddress);
        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[1])), _bmcICON);
        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[2])), service);
        assert.equal(web3.utils.hexToNumber(toHex(bmcMsg[3])), 1);

        const ServiceMsg = rlp.decode(bmcMsg[4]);
        assert.equal(web3.utils.hexToUtf8(toHex(ServiceMsg[0])), 0);

        const coinTransferMsg = rlp.decode(ServiceMsg[1]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[0])), accounts[0]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[1])), _to.split('/').slice(-1)[0]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[2][0][0])), _native);
        assert.equal(web3.utils.hexToNumber(toHex(coinTransferMsg[2][0][1])), amount - chargedFee);

        assert(
            web3.utils.BN(bsh_coin_balance._usableBalance).toNumber() === amount &&
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === amount
        );
    });

    it('Scenario 6: BSHPeriphery receives a successful response of a recent request', async () => {
        let amount = 600000;
        let account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        let tx = await bmc.receiveResponse(_net, service, 1, _msg);
        let account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        let fees = await bsh_core.getAccumulatedFees();

        const transferEvents = await bsh_perif.getPastEvents('TransferEnd', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        let event = transferEvents[0].returnValues;

        assert.equal(event._from, accounts[0]);
        assert.equal(event._sn, 1);
        assert.equal(event._code, 0);
        assert.equal(event._response, '');

        assert(
            fees[0].coinName === _native &&
            Number(fees[0].value) === (Math.floor(amount / 1000) + _fixed_fee) &&
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === amount &&
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === 0
        );
    });
});

contract('As a user, I want to send ERC20_ICX to ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder;
    let service = 'Coin/WrappedCoin';
    let _native = 'MOVR'; let _fee = 10; let _fixed_fee = 500000; let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    let _net = '1234.iconee'; let _from = '0x12345678'; let _value = 999999999999999;
    let REPONSE_HANDLE_SERVICE = 2; let RC_OK = 0; let RC_ERR = 1;
    let _tokenName = 'ICX';
    let _tokenSymbol = 'ICX';
    let INITIAL_SUPPLY = web3.utils.toBN(10000000000000) // 100000 tokens

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.movr');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_native, _fee, _fixed_fee, _tokenName, _tokenSymbol, INITIAL_SUPPLY);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _tokenName, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 0, _msg);
        let balanceBefore = await bsh_core.balanceOf(accounts[0]);
    });

    it('Scenario 8: User sends a valid transferring request', async () => {
        let _to = 'btp://1234.iconee/0x12345678';
        let amount = 600000;
        await bsh_core.transfer(accounts[1], amount);
        let balanceBefore = await bsh_core.getBalanceOf(accounts[1], _tokenName);
        await bsh_core.approve(bsh_core.address, amount, { from: accounts[1] });
        const data = await bsh_core.contract.methods["transfer(string,uint256,string)"](_tokenName, amount, _to).encodeABI();
        let tx = await bsh_core.sendTransaction({ data, from: accounts[1] });
        //let tx = await bsh_core.transfer(_tokenName, amount, _to, {from: accounts[1]});
        let balanceAfter = await bsh_core.getBalanceOf(accounts[1], _tokenName);
        let bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _tokenName);
        let chargedFee = Math.floor(amount / 1000) + _fixed_fee;

        const transferEvents = await bsh_perif.getPastEvents('TransferStart', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        let event = transferEvents[0].returnValues;
        assert.equal(event._from, accounts[1]);
        assert.equal(event._to, _to);
        assert.equal(event._sn, 1);
        assert.equal(event._assetDetails.length, 1);
        assert.equal(event._assetDetails[0].coinName, _tokenName);
        assert.equal(event._assetDetails[0].value, amount - chargedFee);
        assert.equal(event._assetDetails[0].fee, chargedFee);

        const linkStatus = await bmc.getStatus(_bmcICON);
        const bmcBtpAddress = await bmc.getBmcBtpAddress();

        const messageEvents = await bmc.getPastEvents('Message', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        event = messageEvents[0].returnValues;
        assert.equal(event._next, _bmcICON);
        assert.equal(event._seq, linkStatus.txSeq);

        const bmcMsg = rlp.decode(event._msg);

        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[0])), bmcBtpAddress);
        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[1])), _bmcICON);
        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[2])), service);
        assert.equal(web3.utils.hexToNumber(toHex(bmcMsg[3])), 1);

        const ServiceMsg = rlp.decode(bmcMsg[4]);
        assert.equal(web3.utils.hexToUtf8(toHex(ServiceMsg[0])), 0);

        const coinTransferMsg = rlp.decode(ServiceMsg[1]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[0])), accounts[1]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[1])), _to.split('/').slice(-1)[0]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[2][0][0])), _tokenName);
        assert.equal(web3.utils.hexToNumber(toHex(coinTransferMsg[2][0][1])), amount - chargedFee);

        assert.equal(web3.utils.BN(balanceBefore._lockedBalance).toNumber(), 0);
        assert.equal(web3.utils.BN(balanceAfter._lockedBalance).toNumber(), amount);
        assert.equal(
            web3.utils.BN(balanceAfter._usableBalance).toNumber(),
            web3.utils.BN(balanceBefore._usableBalance).toNumber() - amount
        );
        assert.equal(web3.utils.BN(bsh_core_balance._usableBalance).toNumber(), amount);
    });

    it('Scenario 9: BSHPeriphery receives a successful response of a recent request', async () => {
        let amount = 600000;
        let chargedFee = Math.floor(amount / 1000) + _fixed_fee;
        let contract_balanceBefore = await bsh_core.getBalanceOf(accounts[1], _tokenName);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        let tx = await bmc.receiveResponse(_net, service, 1, _msg);
        let contract_balanceAfter = await bsh_core.getBalanceOf(accounts[1], _tokenName);
        let fees = await bsh_core.getAccumulatedFees();
        let bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _tokenName);

        const transferEvents = await bsh_perif.getPastEvents('TransferEnd', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        let event = transferEvents[0].returnValues;

        assert.equal(event._from, accounts[1]);
        assert.equal(event._sn, 1);
        assert.equal(event._code, 0);
        assert.equal(event._response, '');

        assert.equal(web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber(), amount);
        assert.equal(web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber(), 0);
        assert.equal(
            web3.utils.BN(contract_balanceBefore._usableBalance).toNumber(),
            web3.utils.BN(contract_balanceAfter._usableBalance).toNumber()
        );
        assert.equal(web3.utils.BN(bsh_core_balance._usableBalance).toNumber(), chargedFee);
        assert.equal(fees[1].coinName, _tokenName);//todo: check this
        assert.equal(Number(fees[1].value), chargedFee)
    });
});

contract('As a user, I want to receive MOVR from ICON blockchain', (accounts) => {
    let bmc, bsh_perif, bsh_core, notpayable, refundable;
    let service = 'Coin/WrappedCoin'; let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    let _net = '1234.iconee'; let _to = 'btp://1234.iconee/0x12345678';
    let _native = 'MOVR'; let _fee = 10; let _fixed_fee = 500000;
    let RC_ERR = 1; let RC_OK = 0;
    let _uri = 'https://github.com/icon-project/btp';
    let _tokenName = 'ICX';
    let _tokenSymbol = 'ICX';
    let INITIAL_SUPPLY = web3.utils.toBN(10000000000000) // 100000 tokens

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.movr');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_native, _fee, _fixed_fee, _tokenName, _tokenSymbol, INITIAL_SUPPLY);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        notpayable = await NotPayable.new();
        refundable = await Refundable.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await bsh_core.transferNativeCoin(_to, { from: accounts[0], value: 100000000 });
        btpAddr = await bmc.bmcAddress();
    });

    it('Scenario 4: BSHPeriphery receives a request of transferring coins', async () => {
        let _from = '0x12345678';
        let _value = 12345;
        let balanceBefore = await bmc.getBalance(accounts[1]);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _native, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bmc.getBalance(accounts[1]);

        assert.equal(
            web3.utils.BN(balanceAfter).toString(),
            web3.utils.BN(balanceBefore).add(new web3.utils.BN(_value)).toString()
        );
        assert.equal(output.logs[0].args._next, _bmcICON);
        assert.equal(output.logs[0].args._msg, _eventMsg);
    });
});

contract('As a user, I want to receive ERC20_ICX from ICON blockchain', (accounts) => {
    let bmc, bsh_perif, bsh_core, holder, notpayable;
    let service = 'Coin/WrappedCoin'; let _uri = 'https://github.com/icon-project/btp';
    let _native = 'MOVR'; let _fee = 10; let _fixed_fee = 500000;
    let _name = 'ICON'; let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    let _net = '1234.iconee'; let _from = '0x12345678';
    let RC_ERR = 1; let RC_OK = 0;
    let _tokenName = 'ICX';
    let _tokenSymbol = 'ICX';
    let INITIAL_SUPPLY = web3.utils.toBN(10000000000000) // 100000 tokens


    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.movr');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_native, _fee, _fixed_fee, _tokenName, _tokenSymbol, INITIAL_SUPPLY);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        notpayable = await NotPayable.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        btpAddr = await bmc.bmcAddress();
    });

    it('Scenario 5: Receiver is an account client', async () => {
        let _value = 5500;
        let balanceBefore = await bsh_core.balanceOf(accounts[1]);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _tokenName, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bsh_core.balanceOf(accounts[1]);

        assert.equal(
            web3.utils.BN(balanceAfter).toNumber(),
            web3.utils.BN(balanceBefore).toNumber() + _value
        );
        assert.equal(output.logs[0].args._next, _bmcICON);
        assert.equal(output.logs[0].args._msg, _eventMsg);
    });
});