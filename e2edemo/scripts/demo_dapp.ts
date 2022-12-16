import fs from 'fs';
import IconService from 'icon-sdk-js';
import {ethers} from 'hardhat';
import {IconNetwork} from "./icon/network";
import {Contract} from "./icon/contract";
import {XCall} from "./icon/xcall";
import {BigNumber} from "ethers";
const {IconConverter} = IconService;
const {E2E_DEMO_PATH} = process.env

const DEPLOYMENTS_PATH = `${E2E_DEMO_PATH}/deployments.json`
const deployments = new Map();
const iconNetwork = IconNetwork.getDefault();

class DAppProxy extends Contract {
  constructor(iconNetwork: IconNetwork, address: string) {
    super(iconNetwork, address)
  }

  sendMessage(to: string, data: string, value?: string) {
    return this.invoke({
      method: 'sendMessage',
      value: value ? value : '0x0',
      params: {
        _to: to,
        _data: data
      }
    })
  }
}

function getBtpAddress(network: string, dapp: string) {
  return `btp://${network}/${dapp}`;
}

function sleep(millis: number) {
  return new Promise(resolve => setTimeout(resolve, millis));
}

function hexToString(data: string) {
  const hexArray = ethers.utils.arrayify(data);
  let msg = '';
  for (let i = 0; i < hexArray.length; i++) {
    msg += String.fromCharCode(hexArray[i]);
  }
  return msg;
}

function isIconChain(chain: any) {
  return chain.network.indexOf('icon') != -1;
}

function isHardhatChain(chain: any) {
  return chain.network.indexOf('hardhat') != -1;
}

async function sendMessageFromDApp(srcChain: any, dstChain: any, msg: string) {
  let sn;
  if (isIconChain(srcChain)) {
    const xcallSrc = new XCall(iconNetwork, srcChain.contracts.xcall);
    const fee = await xcallSrc.getFee(dstChain.network, false);
    console.log('fee=' + fee);

    const dappSrc = new DAppProxy(iconNetwork, srcChain.contracts.dapp);
    const to = getBtpAddress(dstChain.network, dstChain.contracts.dapp);
    const data = IconConverter.toHex(msg);

    sn = await dappSrc.sendMessage(to, data, fee)
      .then((txHash) => dappSrc.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`DApp: failed to sendMessage: ${result.txHash}`);
        }
        return dappSrc.filterEvent(result.eventLogs,
          'CallMessageSent(Address,str,int,int,bytes)', xcallSrc.address);
      })
      .then((event) => {
        console.log(event);
        return event.indexed ? parseInt(event.indexed[3], 16) : -1;
      })
  } else if (isHardhatChain(srcChain)) {
    const xcallSrc = await ethers.getContractAt('CallService', srcChain.contracts.xcall);
    const fee = await xcallSrc.getFee(dstChain.network, false);
    console.log('fee=' + fee);

    const dappSrc = await ethers.getContractAt('DAppProxySample', srcChain.contracts.dapp);
    const to = getBtpAddress(dstChain.network, dstChain.contracts.dapp);
    const data = IconConverter.toHex(msg);

    sn = await dappSrc.sendMessage(to, data, "0x", {value: fee})
      .then((tx) => tx.wait(1))
      .then((receipt) => {
        if (receipt.status != 1) {
          throw new Error(`DApp: failed to sendMessage: ${receipt.transactionHash}`);
        }
        return xcallSrc.queryFilter(xcallSrc.filters.CallMessageSent(), receipt.blockHash);
      })
      .then((events) => {
        if (events.length == 0) {
          throw new Error(`DApp: could not find event: "CallMessageSent"`);
        }
        console.log(events);
        return events[0].args._sn;
      })
  } else {
    throw new Error(`DApp: unknown source chain: ${srcChain}`);
  }
  console.log(`serialNum=${sn}`);
}

async function checkCallMessage(srcChain: any, dstChain: any) {
  if (isHardhatChain(dstChain)) {
    const xcallDst = await ethers.getContractAt('CallService', dstChain.contracts.xcall);
    const filterCM = xcallDst.filters.CallMessage(
      getBtpAddress(srcChain.network, srcChain.contracts.dapp),
      dstChain.contracts.dapp
    )
    const logs = await xcallDst.queryFilter(filterCM, -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "CallMessage"`);
    }
    console.log(logs[0]);
    return logs[0].args._reqId;
  } else if (isIconChain(dstChain)) {
    const xcallDst = new XCall(iconNetwork, dstChain.contracts.xcall);
    const logs = await xcallDst.queryFilter("CallMessage(str,str,int,int,bytes)", -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "CallMessage"`);
    }
    console.log(logs[0]);
    const hexReqId = logs[0].data && logs[0].data[0];
    return BigNumber.from(hexReqId);
  } else {
    throw new Error(`DApp: unknown destination chain: ${dstChain}`);
  }
}

async function invokeExecuteCall(dstChain: any, reqId: BigNumber) {
  if (isHardhatChain(dstChain)) {
    const xcallDst = await ethers.getContractAt('CallService', dstChain.contracts.xcall);
    await xcallDst.executeCall(reqId)
      .then((tx) => tx.wait(1));
  } else if (isIconChain(dstChain)) {
    const xcallDst = new XCall(iconNetwork, dstChain.contracts.xcall);
    await xcallDst.executeCall(reqId.toHexString())
      .then((txHash) => xcallDst.getTxResult(txHash));
  } else {
    throw new Error(`DApp: unknown destination chain: ${dstChain}`);
  }
}

async function verifyReceivedMessage(dstChain: any, msg: string) {
  let _from, _data;
  if (isHardhatChain(dstChain)) {
    const dappDst = await ethers.getContractAt('DAppProxySample', dstChain.contracts.dapp);
    const filterMR = dappDst.filters.MessageReceived();
    const logs2 = await dappDst.queryFilter(filterMR, -5, "latest");
    if (logs2.length == 0) {
      throw new Error(`DApp: could not find event: "MessageReceived"`);
    }
    console.log(logs2)
    _from = logs2[0].args._from;
    _data = logs2[0].args._data;
  } else if (isIconChain(dstChain)) {
    const dappDst = new DAppProxy(iconNetwork, dstChain.contracts.dapp);
    const logs = await dappDst.queryFilter("MessageReceived(str,bytes)", -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "MessageReceived"`);
    }
    console.log(logs)
    if (logs[0].data === undefined) {
      throw new Error("invalid eventlog \"MessageReceived\"");
    }
    _from = logs[0].data[0];
    _data = logs[0].data[1];
  } else {
    throw new Error(`DApp: unknown destination chain: ${dstChain}`);
  }

  const receivedMsg = hexToString(_data)
  console.log(`From: ${_from}`);
  console.log(`Data: ${_data}`);
  console.log(`Msg: ${receivedMsg}`);
  if (msg !== receivedMsg) {
    throw new Error(`DApp: received message is different from the sent message`);
  }
}

async function sendMessageWithoutRollback(src: string, dst: string) {
  const srcChain = deployments.get(src);
  const dstChain = deployments.get(dst);

  const funcName = sendMessageWithoutRollback.name
  console.log(`\n### ${funcName}: ${src} => ${dst}`);
  const msgData = `${funcName}_${src}_${dst}`;

  console.log(`[1] send message from DApp`);
  await sendMessageFromDApp(srcChain, dstChain, msgData);

  console.log('[-] wait some time for the message delivery...');
  await sleep(5000);

  console.log(`[2] check CallMessage event on ${dst} chain`);
  const reqId = await checkCallMessage(srcChain, dstChain);

  console.log(`[3] invoke executeCall with reqId=${reqId}`);
  await invokeExecuteCall(dstChain, reqId);

  console.log(`[4] verify the received message`);
  await verifyReceivedMessage(dstChain, msgData)
}

async function load_deployments() {
  const data = fs.readFileSync(DEPLOYMENTS_PATH);
  const json = JSON.parse(data.toString());
  deployments.set('icon', json.icon);
  deployments.set('hardhat', json.hardhat);
}

load_deployments()
  .then(() => sendMessageWithoutRollback('icon', 'hardhat'))
  .then(() => sendMessageWithoutRollback('hardhat', 'icon'))
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
