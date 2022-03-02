import { ApiPromise, WsProvider } from "@polkadot/api";
import { xxhashAsHex } from "@polkadot/util-crypto";
import * as fs from "fs";
import * as RLP from "rlp";
import * as URLSafeBase64 from "urlsafe-base64";
import path from "path";
import IconService from "icon-sdk-js";
import { readFile, decimalToHex, convertLEtoBE, findEventIndex, deployScore, buildEventDecoder, buildSovereignBMV } from './util'; 

require('dotenv').config()

async function main() {
  const bmcAddress = process.env.BMC_ADDRESS;
  const netAddress = process.env.DST_NET_ADDRESS;
  const sovereignWssEndpoint = process.env.SOVEREIGN_ENDPOINT;
  const sovereignChainOffset = process.env.SOVEREIGN_CHAIN_OFFSET;
  const mtaRootSize = process.env.MTA_ROOT_SIZE;
  const mtaCacheSize = process.env.MTA_CACHE_SIZE;
  const mtaIsAllowNewerWitness = process.env.MTA_IS_ALLOW_WITNESS;

  const iconNodeUrl = process.env.ICON_ENDPOINT;
  const iconNid = process.env.ICON_NID;
  const iconKeyStoreFilePath = process.env.ICON_KEYSTORE_PATH;
  const iconKeyStorePassword = process.env.ICON_KEYSTORE_PASSWORD;

  const sovereignWsProvider = new WsProvider(sovereignWssEndpoint);
  const sovereignApi = await ApiPromise.create({
    provider: sovereignWsProvider,
    types: {
      GrandpaAuthorities: {
        version: "u8",
        authorityList: "AuthorityList",
      },
      CurrencyIdOf: "Bytes"
    },
  });

  const iconProvider = new IconService.HttpProvider(iconNodeUrl);
  const iconService = new IconService(iconProvider);

  const wallet = IconService.IconWallet.loadKeystore((await readFile(iconKeyStoreFilePath)).toString(), iconKeyStorePassword, false);

  // get relay genesis hash
  console.log(" Sovereign genesis hash: ", sovereignApi.genesisHash.toHex());
  // get relay chain name
  const relayChainName = await sovereignApi.rpc.system.chain();
  console.log(" Sovereign chain name: ", JSON.stringify(relayChainName));

  /*
   * get meta data of relay chain
   */
  console.log(" Get metadata of Sovereign chain...");
  const sovereignMetaData = await sovereignApi.rpc.state.getMetadata();
  fs.writeFileSync("./sovereignMetaData.json", JSON.stringify(sovereignMetaData, null, 2));

  const newAuthoritiesEventIndex = findEventIndex(sovereignMetaData, "Grandpa", "NewAuthorities");
  const evmEventIndex = findEventIndex(sovereignMetaData, "EVM", "Log");

  console.log(" Build event decoder for sovereign chain...");
  await buildEventDecoder(path.resolve("./relayMetaData.json"));

  console.log(" Deploy relay chain event decoder...");
  const sovereignChainEventDecoder = await deployScore(iconService, wallet, iconNid, __dirname + '/../../eventDecoder/build/libs/eventDecoder-optimized.jar', {});

  console.log(" Build sovereign chain BMV...");
  await buildSovereignBMV();

  const sovereignLastBlockHash = await sovereignApi.rpc.chain.getBlockHash(
    sovereignChainOffset
  );

  const grandpaAuthoritiesEncoded = await sovereignApi.rpc.state.getStorage(
    ":grandpa_authorities",
    sovereignLastBlockHash.toHex()
  );

  const grandpaAuthorities = await sovereignApi.createType(
    // @ts-ignore
    "GrandpaAuthorities",
    String(grandpaAuthoritiesEncoded)
  );
  // @ts-ignore
  const validatorList = grandpaAuthorities.authorityList.map((item) =>
    item[0].toHex()
  );
  const encoded = RLP.encode(validatorList);
  const validatorListBase64Encoded = URLSafeBase64.encode(encoded);

  const grandpaPrefixHash = xxhashAsHex("GrandpaFinality", 128);
  const curentSetIdKeyHash = xxhashAsHex("CurrentSetId", 128);
  const grandpaCurrentSetIdStorageKey =
    grandpaPrefixHash + curentSetIdKeyHash.replace("0x", "");
  const grandPaCurrentSetIdEndcoded = await sovereignApi.rpc.state.getStorage(
    grandpaCurrentSetIdStorageKey,
    sovereignLastBlockHash.toHex()
  );

  const grandPaCurrentSetId = await sovereignApi.createType(
    "SetId",
    String(grandPaCurrentSetIdEndcoded)
  );

  const paraBMVDeployParams = {
    bmc: bmcAddress,
    net: netAddress,
    mtaRootSize: decimalToHex(mtaRootSize),
    mtaCacheSize:  decimalToHex(mtaCacheSize),
    mtaIsAllowNewerWitness: mtaIsAllowNewerWitness ? "0x1" : "0x0",
    eventDecoderAddress: sovereignChainEventDecoder,
    mtaOffset: decimalToHex(sovereignChainOffset),
    lastBlockHash: sovereignLastBlockHash.toHex(),
    currentSetId: convertLEtoBE(grandPaCurrentSetId.toHex().replace("0x", "")),
    encodedValidators: validatorListBase64Encoded,
    evmEventIndex: "0x" + decimalToHex(evmEventIndex[0]).replace("0x", "") + decimalToHex(evmEventIndex[1]).replace("0x", ""),
    newAuthoritiesEventIndex: "0x" + decimalToHex(newAuthoritiesEventIndex[0]).replace("0x", "") + decimalToHex(newAuthoritiesEventIndex[1]).replace("0x", "")
  }

  console.log(" Deploy sovereign chain BMV...");
  const paraBMV = await deployScore(iconService, wallet, iconNid, __dirname + '/../../sovereignChain/build/libs/SovereignChain-BMV-optimized.jar', paraBMVDeployParams);

  console.log("\n                --------------------- DONE ---------------------- ");
  console.log("- Sovereign event decoder score address:        ", sovereignChainEventDecoder);
  console.log("- Para chain bmv score address:                 ", paraBMV);

  await sovereignApi.disconnect();
}

main().catch(console.error);
