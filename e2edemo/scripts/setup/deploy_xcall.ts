import fs from 'fs';
import { ethers } from 'hardhat';
import {Contract} from "../icon/contract";
import {IconNetwork} from "../icon/network";
import {BMC} from "../icon/btp";
import {Deployments} from "./config";
const {JAVASCORE_PATH} = process.env

const deployments = Deployments.getDefault();
const iconNetwork = IconNetwork.getDefault();

async function deploy_xcall() {
  // deploy xCall java module
  const icon = deployments.get('icon')
  const xcallJar = JAVASCORE_PATH + '/xcall/build/libs/xcall-0.1.0-optimized.jar'
  const content = fs.readFileSync(xcallJar).toString('hex')
  const xcall = new Contract(iconNetwork)
  const deployTxHash = await xcall.deploy({
    content: content,
    params: {
      _bmc: icon.contracts.bmc,
    }
  })
  const result = await xcall.getTxResult(deployTxHash)
  if (result.status != 1) {
    throw new Error(`xCall deployment failed: ${result.txHash}`);
  }
  icon.contracts.xcall = xcall.address
  console.log(`ICON xCall: deployed to ${xcall.address}`);

  // deploy xCall solidity module
  const hardhat = deployments.get('hardhat')
  const CallSvc = await ethers.getContractFactory("CallService")
  const xcallSol = await CallSvc.deploy()
  await xcallSol.deployed()
  await xcallSol.initialize(hardhat.contracts.bmcp)
  hardhat.contracts.xcall = xcallSol.address
  console.log(`Hardhat xCall: deployed to ${xcallSol.address}`);

  // update deployments
  deployments.set('icon', icon)
  deployments.set('hardhat', hardhat)
  deployments.save();
}

async function setup_xcall() {
  const icon = deployments.get('icon')
  const hardhat = deployments.get('hardhat')

  console.log("ICON: register xCall to BMC");
  const bmc = new BMC(iconNetwork, icon.contracts.bmc)
  await bmc.addService('xcall', icon.contracts.xcall)
    .then((txHash) => bmc.getTxResult(txHash))
    .then((result) => {
      if (result.status != 1) {
        throw new Error(`ICON: failed to register xCall to BMC: ${result.txHash}`);
      }
    })

  console.log("Hardhat: register xCall to BMC");
  const bmcm = await ethers.getContractAt('BMCManagement', hardhat.contracts.bmcm)
  await bmcm.addService('xcall', hardhat.contracts.xcall);
}

deploy_xcall()
  .then(setup_xcall)
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
