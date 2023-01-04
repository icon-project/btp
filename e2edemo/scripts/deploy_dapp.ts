import fs from 'fs';
import {ethers} from 'hardhat';
import {Contract} from "./icon/contract";
import {IconNetwork} from "./icon/network";
import {Deployments} from "./setup/config";
const {E2E_DEMO_PATH} = process.env

const deployments = Deployments.getDefault();
const iconNetwork = IconNetwork.getDefault();

async function deploy_dapp() {
  // deploy DApp java
  const icon = deployments.get('icon')
  const dappJar = E2E_DEMO_PATH + '/dapp-sample/build/libs/dapp-sample-0.1.0-optimized.jar'
  const content = fs.readFileSync(dappJar).toString('hex')
  const dapp = new Contract(iconNetwork)
  const deployTxHash = await dapp.deploy({
    content: content,
    params: {
      _callService: icon.contracts.xcall,
    }
  })
  const result = await dapp.getTxResult(deployTxHash)
  if (result.status != 1) {
    throw new Error(`DApp deployment failed: ${result.txHash}`);
  }
  icon.contracts.dapp = dapp.address
  console.log(`ICON DApp: deployed to ${dapp.address}`);

  // deploy DApp solidity
  const hardhat = deployments.get('hardhat')
  const DAppSample = await ethers.getContractFactory("DAppProxySample")
  const dappSol = await DAppSample.deploy()
  await dappSol.deployed()
  await dappSol.initialize(hardhat.contracts.xcall)
  hardhat.contracts.dapp = dappSol.address
  console.log(`Hardhat DApp: deployed to ${dappSol.address}`);

  // update deployments
  deployments.set('icon', icon)
  deployments.set('hardhat', hardhat)
  deployments.save();
}

deploy_dapp()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
