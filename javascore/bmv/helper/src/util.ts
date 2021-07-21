import * as fs from "fs";
import util from "util";
import { exec } from "child_process";
import IconService from "icon-sdk-js";

require('dotenv').config()

const execPromise = util.promisify(exec);
export const readFile = util.promisify(fs.readFile);

export function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

export function decimalToHex(d) {
  let hex = Number(d).toString(16);

  if (hex.length % 2 != 0) {
    hex = "0" + hex;
  }

  return "0x" + hex;
}

export function convertLEtoBE(input) {
  let result = "";
  for (let i = Math.floor(input.length / 2) - 1; i >= 0; i--) {
    result += input[i * 2];
    if (input[i * 2 + 1]) {
      result += input[i * 2 + 1];
    } else {
      result += "0";
    }
  }

  return "0x" + result.replace(/^0+/, "");
}

export function findEventIndex(relayMetaData, moduleName, eventName) {
  let newAuthoritiesEventIndex = [];

  // @ts-ignore
  const grandpaModule = relayMetaData
    .toJSON()
    .metadata.v13.modules.find((module) => module.name == moduleName);
  if (!grandpaModule) {
    throw new Error("can not find " + moduleName + " module in metadata");
  }
  newAuthoritiesEventIndex.push(grandpaModule.index);
  const secondIndex = grandpaModule.events.findIndex(
    (e) => e.name === eventName
  );
  if (secondIndex < 0) {
    throw new Error("can not find " + eventName + " event");
  }
  // @ts-ignore
  newAuthoritiesEventIndex.push(secondIndex);
  return newAuthoritiesEventIndex;
}

export async function deployScore(
  iconService,
  wallet,
  nid,
  contractPath,
  params
) {
  const { DeployTransactionBuilder } = IconService.IconBuilder;

  const iconInstallScoreAddr = "cx0000000000000000000000000000000000000000";

  const walletAddress = wallet.getAddress();
  const stepLimit = IconService.IconConverter.toBigNumber(10000000000);

  const contractContent = await readFile(contractPath, { encoding: "hex" });

  const txObj = new DeployTransactionBuilder()
    .from(walletAddress)
    .to(iconInstallScoreAddr)
    .stepLimit(IconService.IconConverter.toBigNumber(13610920010))
    .nid(IconService.IconConverter.toBigNumber(nid))
    .nonce(IconService.IconConverter.toBigNumber(1))
    .version(IconService.IconConverter.toBigNumber(3))
    .timestamp(new Date().getTime() * 1000)
    // @ts-ignore
    .contentType("application/java")
    .content("0x" + contractContent)
    .params(params)
    .build();

  /* Create SignedTransaction instance */
  const signedTransaction = new IconService.SignedTransaction(txObj, wallet);
  /* Send transaction. It returns transaction hash. */
  const transactionId = await iconService
    .sendTransaction(signedTransaction)
    .execute();
  await sleep(4000);
  const transactionResult = await iconService
    .getTransactionResult(transactionId)
    .execute();
  if (transactionResult.failure) {
    throw new Error(
      "deploy score " +
        contractPath +
        " error: " +
        JSON.stringify(transactionResult.failure)
    );
  }
  return transactionResult.scoreAddress;
}

export async function buildEventDecoder(metaDataFilePath: String, accountIdSize: number = 32) {
  try {
    await execPromise(
      `cd .. && gradle loadMetaData -PmetaDataFilePath=${metaDataFilePath} -PaccountIdSize=${accountIdSize}`
    );
    await execPromise(`cd .. && cd eventDecoder && gradle optimizedJar`);
  } catch (error) {
    throw new Error(error.toString());
  }
}

export async function buildParaBMV() {
  try {
    await execPromise(`cd .. && cd parachain && gradle optimizedJar`);
  } catch (error) {
    throw new Error(error.toString());
  }
}

export async function buildSovereignBMV() {
    try {
      await execPromise(`cd .. && cd sovereignChain && gradle optimizedJar`);
    } catch (error) {
      throw new Error(error.toString());
    }
  }
