import IconService from "icon-sdk-js";
import {IconNetwork} from "./icon/network";
import {Chain, Gov} from "./icon/system";

const {IconAmount} = IconService;

const iconNetwork = IconNetwork.getDefault();
const chain = new Chain(iconNetwork);
const gov = new Gov(iconNetwork);
const prepAddress = iconNetwork.wallet.getAddress()

async function ensure_decentralization() {
  const mainPReps = await chain.getMainPReps();
  console.log(mainPReps)
  const prep = await chain.getPRep(prepAddress)
    .catch((error) => {
      console.log('Need to register PRep and get power first')
    });
  if (mainPReps.preps.length == 0 && prep == undefined) {
    const totalSupply = await iconNetwork.getTotalSupply()
    const minDelegated = totalSupply.div(500)
    const bondAmount = IconAmount.of(100_000, IconAmount.Unit.ICX).toLoop()

    console.log(`ICON: registerPRep`)
    const name = `node_${prepAddress}`
    await chain.registerPRep(name)
      .then((txHash) => gov.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`ICON: failed to registerPrep: ${result.txHash}`);
        }
      })

    console.log(`ICON: setStake`)
    await chain.setStake(minDelegated.plus(bondAmount))
      .then((txHash) => gov.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`ICON: failed to setStake: ${result.txHash}`);
        }
      })

    console.log(`ICON: setDelegation`)
    await chain.setDelegation(prepAddress, minDelegated)
      .then((txHash) => gov.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`ICON: failed to setDelegation: ${result.txHash}`);
        }
      })

    console.log(`ICON: setBonderList`)
    await chain.setBonderList(prepAddress)
      .then((txHash) => gov.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`ICON: failed to setBonderList: ${result.txHash}`);
        }
      })

    console.log(`ICON: setBond`)
    await chain.setBond(prepAddress, bondAmount)
      .then((txHash) => gov.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`ICON: failed to setBond: ${result.txHash}`);
        }
      })
  }

  if (mainPReps.preps.length == 0) {
    console.log(prep)
    throw new Error(`ICON: need to wait until the next term for decentralization`);
  }
}

async function setup() {
  // ensure BTP revision
  const BTP_REVISION = 21
  const rev = parseInt(await chain.getRevision(), 16);
  console.log(`ICON: revision: ${rev}`)
  if (rev < BTP_REVISION) {
    console.log(`ICON: Set revision to ${BTP_REVISION}`)
    await gov.setRevision(BTP_REVISION)
      .then((txHash) => gov.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`ICON: failed to set revision: ${result.txHash}`);
        }
      })
  }

  // ensure public key registration
  const pubkey = await chain.getPRepNodePublicKey(prepAddress)
    .catch((error) => {
      console.log(`error: ${error}`)
    })
  console.log(`ICON: pubkey: ${pubkey}`)
  if (pubkey == undefined) {
    console.log('ICON: register PRep node publicKey')
    // prefixing "04" for indicating uncompressed format
    const pkey = '0x04' + iconNetwork.wallet.getPublicKey();
    await chain.registerPRepNodePublicKey(prepAddress, pkey)
      .then((txHash) => gov.getTxResult(txHash))
      .then((result) => {
        if (result.status != 1) {
          throw new Error(`ICON: failed to registerPRepNodePublicKey: ${result.txHash}`);
        }
      })
  }
  console.log('ICON: node setup completed')
}

ensure_decentralization()
  .then(setup)
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
