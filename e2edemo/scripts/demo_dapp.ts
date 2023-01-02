import fs from 'fs';
import IconService from 'icon-sdk-js';
import {ethers} from 'hardhat';
import {IconNetwork} from "./icon/network";
import {DAppProxy} from "./icon/dapp_proxy";
import {XCall} from "./icon/xcall";
import {BigNumber} from "ethers";
const {IconConverter} = IconService;
const {E2E_DEMO_PATH} = process.env

const DEPLOYMENTS_PATH = `${E2E_DEMO_PATH}/deployments.json`
const deployments = new Map();
const iconNetwork = IconNetwork.getDefault();

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
  return chain.network.includes('icon');
}

function isHardhatChain(chain: any) {
  return chain.network.includes('hardhat');
}

async function sendMessageFromDApp(srcChain: any, dstChain: any, msg: string,
                                   rollback?: string) {
  let sn;
  if (isIconChain(srcChain)) {
    const xcallSrc = new XCall(iconNetwork, srcChain.contracts.xcall);
    const fee = await xcallSrc.getFee(dstChain.network, false);
    console.log('fee=' + fee);

    const dappSrc = new DAppProxy(iconNetwork, srcChain.contracts.dapp);
    const to = getBtpAddress(dstChain.network, dstChain.contracts.dapp);
    const data = IconConverter.toHex(msg);
    const rbData = rollback ? IconConverter.toHex(rollback) : undefined;

    sn = await dappSrc.sendMessage(to, data, rbData, fee)
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
    const rbData = rollback ? IconConverter.toHex(rollback) : "0x";

    sn = await dappSrc.sendMessage(to, data, rbData, {value: fee})
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
  return BigNumber.from(sn);
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
    const logs = await dappDst.queryFilter(dappDst.filters.MessageReceived(), -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "MessageReceived"`);
    }
    console.log(logs)
    _from = logs[0].args._from;
    _data = logs[0].args._data;
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

async function confirmMessageCleanup(srcChain: any, sn: BigNumber) {
  let _recvSn: number | BigNumber;
  if (isIconChain(srcChain)) {
    const xcallSrc = new XCall(iconNetwork, srcChain.contracts.xcall);
    const logs = await xcallSrc.queryFilter("CallRequestCleared(int)", -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "CallRequestCleared"`);
    }
    console.log(logs[0]);
    _recvSn = logs[0].indexed ? parseInt(logs[0].indexed[1], 16) : -1;
  } else if (isHardhatChain(srcChain)) {
    const xcallSrc = await ethers.getContractAt('CallService', srcChain.contracts.xcall);
    const logs = await xcallSrc.queryFilter(xcallSrc.filters.CallRequestCleared(), -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "CallRequestCleared"`);
    }
    console.log(logs)
    _recvSn = logs[0].args._sn;
  } else {
    throw new Error(`DApp: unknown source chain: ${srcChain}`);
  }
  if (!sn.eq(_recvSn)) {
    throw new Error(`DApp: received serial number (${_recvSn}) is different from the sent one (${sn})`);
  }
}

async function checkRollbackMessage(srcChain: any) {
  if (isHardhatChain(srcChain)) {
    const xcallSrc = await ethers.getContractAt('CallService', srcChain.contracts.xcall);
    const logs = await xcallSrc.queryFilter(xcallSrc.filters.RollbackMessage(), -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "RollbackMessage"`);
    }
    console.log(logs[0]);
    return logs[0].args._sn;
  } else if (isIconChain(srcChain)) {
    const xcallSrc = new XCall(iconNetwork, srcChain.contracts.xcall);
    const logs = await xcallSrc.queryFilter("RollbackMessage(int,bytes,str)", -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "RollbackMessage"`);
    }
    console.log(logs[0]);
    const hexSn = logs[0].indexed && logs[0].indexed[1];
    return BigNumber.from(hexSn);
  } else {
    throw new Error(`DApp: unknown source chain: ${srcChain}`);
  }
}

async function invokeExecuteRollback(srcChain: any, sn: BigNumber) {
  if (isHardhatChain(srcChain)) {
    const xcallSrc = await ethers.getContractAt('CallService', srcChain.contracts.xcall);
    await xcallSrc.executeRollback(sn)
      .then((tx) => tx.wait(1));
  } else if (isIconChain(srcChain)) {
    const xcallSrc = new XCall(iconNetwork, srcChain.contracts.xcall);
    await xcallSrc.executeRollback(sn.toHexString())
      .then((txHash) => xcallSrc.getTxResult(txHash));
  } else {
    throw new Error(`DApp: unknown source chain: ${srcChain}`);
  }
}

async function verifyRollbackDataReceivedMessage(srcChain: any, rollback: string | undefined) {
  let _from, _data, _ssn;
  if (isHardhatChain(srcChain)) {
    const dappSrc = await ethers.getContractAt('DAppProxySample', srcChain.contracts.dapp);
    const logs = await dappSrc.queryFilter(dappSrc.filters.RollbackDataReceived(), -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "RollbackDataReceived"`);
    }
    console.log(logs)
    _from = logs[0].args._from;
    _ssn = logs[0].args._ssn;
    _data = logs[0].args._rollback;
  } else if (isIconChain(srcChain)) {
    const dappSrc = new DAppProxy(iconNetwork, srcChain.contracts.dapp);
    const logs = await dappSrc.queryFilter("RollbackDataReceived(str,int,bytes)", -5, "latest");
    if (logs.length == 0) {
      throw new Error(`DApp: could not find event: "RollbackDataReceived"`);
    }
    console.log(logs)
    if (logs[0].data === undefined) {
      throw new Error("invalid eventlog \"RollbackDataReceived\"");
    }
    _from = logs[0].data[0];
    _ssn = logs[0].data[1];
    _data = logs[0].data[2];
  } else {
    throw new Error(`DApp: unknown source chain: ${srcChain}`);
  }

  const receivedRollback = hexToString(_data)
  console.log(`From: ${_from}`);
  console.log(`Ssn: ${_ssn}`);
  console.log(`Data: ${_data}`);
  console.log(`Rollback: ${receivedRollback}`);
  if (rollback !== receivedRollback) {
    throw new Error(`DApp: received rollback is different from the sent data`);
  }
}

async function sendCallMessage(src: string, dst: string, msgData?: string, needRollback?: boolean) {
  const srcChain = deployments.get(src);
  const dstChain = deployments.get(dst);

  const testName = sendCallMessage.name + (needRollback ? "WithRollback" : "");
  console.log(`\n### ${testName}: ${src} => ${dst}`);
  if (!msgData) {
    msgData = `${testName}_${src}_${dst}`;
  }
  const rollback = needRollback ? `ThisIsRollbackMessage_${src}_${dst}` : undefined;

  console.log(`[1] send message from DApp`);
  const sn = await sendMessageFromDApp(srcChain, dstChain, msgData, rollback);

  console.log(`[-] wait some time for the message delivery on ${dst}...`);
  await sleep(5000);

  console.log(`[2] check CallMessage event on ${dst} chain`);
  const reqId = await checkCallMessage(srcChain, dstChain);

  console.log(`[3] invoke executeCall with reqId=${reqId}`);
  await invokeExecuteCall(dstChain, reqId);

  let step = 4;
  if (msgData !== "revertMessage") {
    console.log(`[${step++}] verify the received message`);
    await verifyReceivedMessage(dstChain, msgData);
  }

  if (needRollback) {
    console.log(`[-] wait some time for the message delivery on ${src}...`);
    await sleep(5000);

    if (msgData === "revertMessage") {
      console.log(`[${step++}] check RollbackMessage event on ${src} chain`);
      const sn = await checkRollbackMessage(srcChain);

      console.log(`[${step++}] invoke executeRollback with sn=${sn}`);
      await invokeExecuteRollback(srcChain, sn);

      console.log(`[${step++}] verify rollback data received message`);
      await verifyRollbackDataReceivedMessage(srcChain, rollback);
    }

    console.log(`[${step++}] confirm message cleanup on ${src}`);
    await confirmMessageCleanup(srcChain, sn);
  }
}

async function load_deployments() {
  const data = fs.readFileSync(DEPLOYMENTS_PATH);
  const json = JSON.parse(data.toString());
  deployments.set('icon', json.icon);
  deployments.set('hardhat', json.hardhat);
}

load_deployments()
  .then(() => sendCallMessage('icon', 'hardhat'))
  .then(() => sendCallMessage('hardhat', 'icon'))
  .then(() => sendCallMessage('icon', 'hardhat', "checkMessageCleanup", true))
  .then(() => sendCallMessage('hardhat', 'icon', "checkMessageCleanup", true))
  .then(() => sendCallMessage('icon', 'hardhat', "revertMessage", true))
  .then(() => sendCallMessage('hardhat', 'icon', "revertMessage", true))
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
