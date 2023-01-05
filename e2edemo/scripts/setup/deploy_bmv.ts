import fs from 'fs';
import { ethers } from 'hardhat';
import {Contract} from "../icon/contract";
import {BMC, BMV} from "../icon/btp";
import {Gov} from "../icon/system";
import {IconNetwork} from "../icon/network";
import IconService from "icon-sdk-js";
import {Deployments} from "./config";
const {IconConverter} = IconService;
const {JAVASCORE_PATH, BMV_BTP_BLOCK} = process.env

const bridgeMode = !BMV_BTP_BLOCK || BMV_BTP_BLOCK !== "true";
const deployments = Deployments.getDefault();
const iconNetwork = IconNetwork.getDefault();

let netTypeId = '';
let netId = '';

async function open_btp_network() {
  // open BTP network first before deploying BMV
  const icon = deployments.get('icon')
  const lastBlock = await iconNetwork.getLastBlock();
  const netName = `hardhat-${lastBlock.height}`
  console.log(`ICON: open BTP network for ${netName}`)
  const gov = new Gov(iconNetwork);
  await gov.openBTPNetwork(netName, icon.contracts.bmc)
    .then((txHash) => gov.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`ICON: failed to openBTPNetwork: ${result.txHash}`);
      }
      return gov.filterEvent(result.eventLogs,
        'BTPNetworkOpened(int,int)', 'cx0000000000000000000000000000000000000000')
    })
    .then((event) => {
      console.log(event);
      if (!event.indexed) {
        throw new Error(`ICON: failed to find networkId`);
      }
      netTypeId = event.indexed[1];
      netId = event.indexed[2];
    })
  console.log(`networkTypeId=${netTypeId}`);
  console.log(`networkId=${netId}`);
}

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
  console.log(`ICON BMV-Bridge: deployed to ${bmv.address}`);

  if (bridgeMode) {
    // deploy BMV-Bridge solidity module
    const BMVBridge = await ethers.getContractFactory("BMV")
    const bmvb = await BMVBridge.deploy(hardhat.contracts.bmcp, icon.network, icon.blockNum)
    await bmvb.deployed()
    hardhat.contracts.bmvb = bmvb.address
    console.log(`Hardhat BMV-Bridge: deployed to ${bmvb.address}`);
  } else {
    // get firstBlockHeader via btp2 API
    const networkInfo = await iconNetwork.getBTPNetworkInfo(netId);
    console.log('networkInfo:', networkInfo);
    const startHeight = parseInt(networkInfo.startHeight, 16);
    console.log('startHeight:', startHeight);
    const receiptHeight = IconConverter.toHex(startHeight + 1);
    const header = await iconNetwork.getBTPHeader(netId, receiptHeight);
    const firstBlockHeader = '0x' + Buffer.from(header, 'base64').toString('hex');
    console.log('firstBlockHeader:', firstBlockHeader);

    // deploy BMV-BtpBlock solidity module
    const BMVBtp = await ethers.getContractFactory("BtpMessageVerifier")
    const bmvBtp = await BMVBtp.deploy(hardhat.contracts.bmcp, icon.network, netTypeId, firstBlockHeader, '0x0')
    await bmvBtp.deployed()
    hardhat.contracts.bmv = bmvBtp.address
    console.log(`Hardhat BMV: deployed to ${bmvBtp.address}`);
  }

  // update deployments
  deployments.set('icon', icon)
  deployments.set('hardhat', hardhat)
  deployments.save();
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
  console.log(`ICON: addBTPLink for ${bmcHardhatAddr}`)
  await bmc.addBTPLink(bmcHardhatAddr, netId)
    .then((txHash) => bmc.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`ICON: failed to addBTPLink: ${result.txHash}`);
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

  console.log(`Hardhat: addVerifier for ${icon.network}`)
  let bmvAddress;
  if (bridgeMode) {
    const bmvb = await ethers.getContractAt('BMV', hardhat.contracts.bmvb)
    bmvAddress = bmvb.address;
  } else {
    const bmvBtp = await ethers.getContractAt('BtpMessageVerifier', hardhat.contracts.bmv)
    bmvAddress = bmvBtp.address;
  }
  await bmcm.addVerifier(icon.network, bmvAddress)
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

open_btp_network()
  .then(deploy_bmv)
  .then(setup_bmv)
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
