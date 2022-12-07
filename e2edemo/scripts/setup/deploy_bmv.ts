import fs from 'fs';
import { ethers } from 'hardhat';
import {Contract} from "../icon/contract";
import {BMC, BMV} from "../icon/btp";
import {Gov} from "../icon/system";
import {IconNetwork} from "../icon/network";
import IconService from "icon-sdk-js";
const {IconConverter} = IconService;
const {JAVASCORE_PATH, E2E_DEMO_PATH} = process.env

const DEPLOYMENTS_PATH = `${E2E_DEMO_PATH}/deployments.json`
const deployments = new Map();
const iconNetwork = IconNetwork.getDefault();

async function deploy_bmv() {
  // get last block number of ICON
  const lastBlock = await iconNetwork.getLastBlock();
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
  const bmv = new Contract(iconNetwork)
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
    throw new Error(`BMV deployment failed: ${result.txHash}`);
  }
  icon.contracts.bmv = bmv.address
  console.log(`ICON BMV: deployed to ${bmv.address}`);

  // deploy BMV-Bridge solidity module
  const BMVBridge = await ethers.getContractFactory("BMV")
  const bmvb = await BMVBridge.deploy(hardhat.contracts.bmcp, icon.network, icon.blockNum)
  await bmvb.deployed()
  hardhat.contracts.bmvb = bmvb.address
  console.log(`Hardhat BMV: deployed to ${bmvb.address}`);

  // update deployments
  deployments.set('icon', icon)
  deployments.set('hardhat', hardhat)
}

async function setup_bmv() {
  const icon = deployments.get('icon')
  const hardhat = deployments.get('hardhat')

  // get the BTP address of ICON BMC
  const bmc = new BMC(iconNetwork, icon.contracts.bmc)
  const bmv = new BMV(iconNetwork, icon.contracts.bmv)
  const bmcIconAddr = await bmc.getBtpAddress()
  console.log(`BTP address of ICON BMC: ${bmcIconAddr}`)

  // get the BTP address of hardhat BMC
  const bmcm = await ethers.getContractAt('BMCManagement', hardhat.contracts.bmcm)
  const bmcp = await ethers.getContractAt('BMCPeriphery', hardhat.contracts.bmcp)
  const bmvb = await ethers.getContractAt('BMV', hardhat.contracts.bmvb)
  const bmcHardhatAddr = await bmcp.getBtpAddress()
  console.log(`BTP address of Hardhat BMC: ${bmcHardhatAddr}`)

  console.log(`ICON: addVerifier for ${hardhat.network}`)
  await bmc.addVerifier(hardhat.network, bmv.address)
    .then((txHash) => bmc.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`ICON: failed to register BMV to BMC: ${result.txHash}`);
      }
    })
  console.log(`ICON: addLink for ${bmcHardhatAddr}`)
  await bmc.addLink(bmcHardhatAddr)
    .then((txHash) => bmc.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`ICON: failed to addLink: ${result.txHash}`);
      }
    })
  console.log(`ICON: addRelay`)
  await bmc.addRelay(bmcHardhatAddr, iconNetwork.wallet.getAddress())
    .then((txHash) => bmc.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`ICON: failed to addRelay: ${result.txHash}`);
      }
    })
  const netName = `hardhat-${icon.blockNum}`
  console.log(`ICON: open BTP network for ${netName}`)
  const gov = new Gov(iconNetwork);
  await gov.openBTPNetwork(netName, bmc.address)
    .then((txHash) => gov.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`ICON: failed to openBTPNetwork: ${result.txHash}`);
      }
    })

  console.log(`Hardhat: addVerifier for ${icon.network}`)
  await bmcm.addVerifier(icon.network, bmvb.address)
    .then((tx) => {
      return tx.wait(1)
    });
  console.log(`Hardhat: addLink: ${bmcIconAddr}`)
  await bmcm.addLink(bmcIconAddr)
    .then((tx) => {
      return tx.wait(1)
    });
  console.log(`Hardhat: addRelay`)
  const signers = await ethers.getSigners()
  await bmcm.addRelay(bmcIconAddr, signers[0].getAddress())
    .then((tx) => {
      return tx.wait(1)
    });
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
  .then(setup_bmv)
  .then(save_deployments)
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
