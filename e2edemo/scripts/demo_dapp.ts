import fs from 'fs';
import IconService from 'icon-sdk-js';
import {ethers} from 'hardhat';
import {IconNetwork} from "./icon/network";
import {Contract} from "./icon/contract";
import {XCall} from "./icon/xcall";
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

async function sendMessageWithoutRollback() {
  const icon = deployments.get('icon');
  const hardhat = deployments.get('hardhat');

  const xcall = new XCall(iconNetwork, icon.contracts.xcall);
  const fee = await xcall.getFee(hardhat.network, false);
  console.log('fee=' + fee);

  const dapp = new DAppProxy(iconNetwork, icon.contracts.dapp);
  const to = getBtpAddress(hardhat.network, hardhat.contracts.dapp);
  const data = IconConverter.toHex('sendMessageWithoutRollback');
  let sn;
  console.log('[1] sendCallMessage: withoutRollback');
  await dapp.sendMessage(to, data, fee)
    .then((txHash) => dapp.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`DApp: failed to sendMessage: ${result.txHash}`);
      }
      return dapp.filterEvent(result.eventLogs,
        'CallMessageSent(Address,str,int,int,bytes)', xcall.address);
    })
    .then((event) => {
      console.log(event);
      sn = event.indexed ? parseInt(event.indexed[3], 16) : -1;
    })
  console.log(`serialNum=${sn}`);

  console.log('wait some time for the message delivery...');
  await sleep(3000);

  console.log('[2] check CallMessage event on destination chain');
  const xcallDst = await ethers.getContractAt('CallService', hardhat.contracts.xcall);
  const filterCM = xcallDst.filters.CallMessage(
    getBtpAddress(icon.network, icon.contracts.dapp),
    hardhat.contracts.dapp
  )
  const logs = await xcallDst.queryFilter(filterCM, -10, "latest");
  if (logs.length == 0) {
    throw new Error(`DApp: could not find event: "CallMessage"`);
  }
  console.log(logs[0]);
  const reqId = logs[0].args._reqId;

  console.log(`[3] invoke executeCall with reqId=${reqId}`);
  await xcallDst.executeCall(reqId)
    .then((tx) => tx.wait(1));

  console.log(`[4] verify the received message`);
  const dappDst = await ethers.getContractAt('DAppProxySample', hardhat.contracts.dapp);
  const filterMR = dappDst.filters.MessageReceived();
  const logs2 = await dappDst.queryFilter(filterMR, -10, "latest");
  if (logs2.length == 0) {
    throw new Error(`DApp: could not find event: "MessageReceived"`);
  }
  console.log(logs2)
  const _from = logs2[0].args._from;
  const _data = logs2[0].args._data;
  console.log(`From: ${_from}`);
  console.log(`Data: ${_data}`);
  console.log(`Msg: ${hexToString(_data)}`);
}

async function load_deployments() {
  const data = fs.readFileSync(DEPLOYMENTS_PATH);
  const json = JSON.parse(data.toString());
  deployments.set('icon', json.icon);
  deployments.set('hardhat', json.hardhat);
}

load_deployments()
  .then(sendMessageWithoutRollback)
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
