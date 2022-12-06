import fs from 'fs';
import { ethers } from 'hardhat';
import {Contract} from "./icon/contract";
import {IconNetwork} from "./icon/network";
const {JAVASCORE_PATH, E2E_DEMO_PATH} = process.env

const deployments = new Map();

async function deploy_java() {
  const iconNetwork = IconNetwork.getDefault();
  const NID = iconNetwork.nid
  const BMC_NETWORK_ID = `0x${NID}.icon`
  console.log(`ICON: deploy BMC for ${BMC_NETWORK_ID}`)

  const bmcJar = JAVASCORE_PATH + '/bmc/build/libs/bmc-0.1.0-optimized.jar'
  const content = fs.readFileSync(bmcJar).toString('hex')
  const bmc = new Contract(iconNetwork)
  const deployTxHash = await bmc.deploy({
    content: content,
    params: {
      _net: BMC_NETWORK_ID
    }
  })
  const result = await bmc.getTxResult(deployTxHash)
  if (result.status != 1) {
    throw new Error(`BMC deployment failed: ${result.txHash}`);
  }
  console.log(`ICON BMC: deployed to ${bmc.address}`);

  deployments.set('icon', {
    'network': BMC_NETWORK_ID,
    'contracts': {
      'bmc': bmc.address
    }
  })
}

async function deploy_solidity() {
  const network = await ethers.provider.getNetwork()
  const BMC_NETWORK_ID = "0x" + network.chainId.toString(16) + ".hardhat"
  console.log(`Hardhat: deploy BMC modules for ${BMC_NETWORK_ID}`)

  const BMCManagement = await ethers.getContractFactory("BMCManagement");
  const bmcm = await BMCManagement.deploy();
  await bmcm.deployed();
  await bmcm.initialize()
  console.log(`BMCManagement: deployed to ${bmcm.address}`);

  const BMCService = await ethers.getContractFactory("BMCService");
  const bmcs = await BMCService.deploy();
  await bmcs.deployed();
  await bmcs.initialize(bmcm.address)
  console.log(`BMCService: deployed to ${bmcs.address}`);

  const BMCPeriphery = await ethers.getContractFactory("BMCPeriphery");
  const bmcp = await BMCPeriphery.deploy();
  await bmcp.deployed();
  await bmcp.initialize(BMC_NETWORK_ID, bmcm.address, bmcs.address);
  console.log(`BMCPeriphery: deployed to ${bmcp.address}`);

  console.log('Hardhat: management.setBMCPeriphery');
  await bmcm.setBMCPeriphery(bmcp.address)
    .then((tx) => {
      return tx.wait(1)
    });
  console.log('Hardhat: management.setBMCService');
  await bmcm.setBMCService(bmcs.address)
    .then((tx) => {
      return tx.wait(1)
    });
  console.log('Hardhat: service.setBMCPeriphery');
  await bmcs.setBMCPeriphery(bmcp.address)
    .then((tx) => {
      return tx.wait(1)
    });

  deployments.set('hardhat', {
    'network': BMC_NETWORK_ID,
    'contracts': {
      'bmcm': bmcm.address,
      'bmcs': bmcs.address,
      'bmcp': bmcp.address,
    }
  })
}

async function save_deployments() {
  const path = `${E2E_DEMO_PATH}/deployments.json`
  fs.writeFileSync(path, JSON.stringify(Object.fromEntries(deployments)), 'utf-8')
}

deploy_java()
  .then(deploy_solidity)
  .then(save_deployments)
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
