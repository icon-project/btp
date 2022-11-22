import fs from 'fs';
import { ethers } from 'hardhat';
import {Contract} from "./icon/contract";
import {IconNetwork} from "./icon/network";
import IconService from "icon-sdk-js";
const {IconConverter} = IconService;
const {JAVASCORE_PATH, E2E_DEMO_PATH} = process.env

const DEPLOYMENTS_PATH = `${E2E_DEMO_PATH}/deployments.json`
const deployments = new Map();

async function deploy_bmv() {
  // get last block number of ICON
  const localNetwork = IconNetwork.getDefault();
  const lastBlock = await localNetwork.getLastBlock();
  const icon = deployments.get('icon')
  icon.blockNum = lastBlock.height
  console.log(`Block number (${icon.network}): ${icon.blockNum}`);

  // get last block number of hardhat
  const blockNum = await ethers.provider.getBlockNumber();
  const hardhat = deployments.get('hardhat')
  hardhat.blockNum = blockNum
  console.log(`Block number (${hardhat.network}): ${hardhat.blockNum}`);

  // deploy BMV-Bridge java module
  const bmvJar = JAVASCORE_PATH + '/bmv/bridge/build/libs/bmv-bridge-0.1.0-optimized.jar'
  const content = fs.readFileSync(bmvJar).toString('hex')
  const bmv = new Contract(localNetwork)
  const deployTxHash = await bmv.deploy({
    content: content,
    params: {
      _bmc: icon.contracts.bmc,
      _net: hardhat.network,
      _offset: IconConverter.toHex(hardhat.blockNum)
    }
  })
  const result = await bmv.getTxResult(deployTxHash)
  if (result.status != 1) {
    throw new Error(`BMV deployment failed: ${result.failure}`);
  }
  icon.contracts.bmv = bmv.address
  console.log(`BMV: deployed to ${bmv.address}`);

  // deploy BMV-Bridge solidity module
  const BMVBridge = await ethers.getContractFactory("BMV")
  const bmvb = await BMVBridge.deploy(hardhat.contracts.bmcp, icon.network, icon.blockNum)
  await bmvb.deployed()
  hardhat.contracts.bmvb = bmvb.address
  console.log(`BMV-Bridge: deployed to ${bmvb.address}`);

  // update deployments
  deployments.set('icon', icon)
  deployments.set('hardhat', hardhat)
}

async function load_deployments() {
  const data = fs.readFileSync(DEPLOYMENTS_PATH);
  const json = JSON.parse(data.toString());
  deployments.set('icon', json.icon)
  deployments.set('hardhat', json.hardhat)
}

async function save_deployments() {
  fs.writeFileSync(DEPLOYMENTS_PATH, JSON.stringify(Object.fromEntries(deployments)), 'utf-8')
}

load_deployments()
  .then(deploy_bmv)
  .then(save_deployments)
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
