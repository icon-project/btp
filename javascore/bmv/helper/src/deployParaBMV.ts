import { ApiPromise, WsProvider } from "@polkadot/api";
import { xxhashAsHex } from "@polkadot/util-crypto";
import * as fs from "fs";
import * as RLP from "rlp";
import * as URLSafeBase64 from "urlsafe-base64";
import path from "path";
import IconService from "icon-sdk-js";
import { readFile, decimalToHex, convertLEtoBE, findEventIndex, deployScore, buildEventDecoder, buildParaBMV } from './util'; 

require('dotenv').config()

async function main() {
  // const wssEndpoint = "wss://rpc.polkadot.io"; // polkadot relay chain
  // const wssEndpoint = "wss://kusama-rpc.polkadot.io"; // kusama relay chain
  // const wssEndpoint = "wss://wss-relay.testnet.moonbeam.network"; // moonbase alpha relay chain

  // const wssEndpoint = "wss://wss.moonriver.moonbeam.network"; // moonriver parachain
  // const wssEndpoint = "wss://wss.testnet.moonbeam.network"; // moonbase alpha parachain
  // const wssEndpoint = "wss://icon-btp.ecl.vn:34008/"; // lecle moonbase parachain

  const bmcAddress = process.env.BMC_ADDRESS;
  const netAddress = process.env.DST_NET_ADDRESS;
  const relayWssEndpoint = process.env.RELAY_ENDPOINT; // wss endpoint of relay chain
  const paraWssEndpoint = process.env.PARA_ENDPOINT; // wss endpoint of para chain

  const relayChainOffset = process.env.RELAY_CHAIN_OFFSET; // offset of relay chain
  const paraChainOffset = process.env.PARA_CHAIN_OFFSET; // offset of para chain
  const mtaRootSize = process.env.MTA_ROOT_SIZE;
  const mtaCacheSize = process.env.MTA_CACHE_SIZE;
  const mtaIsAllowNewerWitness = process.env.MTA_IS_ALLOW_WITNESS;

  const iconNodeUrl = process.env.ICON_ENDPOINT;
  const iconNid = process.env.ICON_NID;
  const iconKeyStoreFilePath = process.env.ICON_KEYSTORE_PATH;
  const iconKeyStorePassword = process.env.ICON_KEYSTORE_PASSWORD;

  const relayWsProvider = new WsProvider(relayWssEndpoint);
  const paraWsProvider = new WsProvider(paraWssEndpoint);
  const relayApi = await ApiPromise.create({
    provider: relayWsProvider,
    types: {
      GrandpaAuthorities: {
        version: "u8",
        authorityList: "AuthorityList",
      },
    },
  });

  const iconProvider = new IconService.HttpProvider(iconNodeUrl);
  const iconService = new IconService(iconProvider);

  const wallet = IconService.IconWallet.loadKeystore((await readFile(iconKeyStoreFilePath)).toString(), iconKeyStorePassword, false);

  const paraApi = await ApiPromise.create({
    provider: paraWsProvider,
    types: {
      RoundIndex: "u32",
    },
  });

  // get relay genesis hash
  console.log(" Relay genesis hash: ", relayApi.genesisHash.toHex());
  // get relay chain name
  const relayChainName = await relayApi.rpc.system.chain();
  console.log(" Relay chain name: ", JSON.stringify(relayChainName));

  // get relay genesis hash
  console.log(" Para genesis hash: ", paraApi.genesisHash.toHex());
  // get relay chain name
  const paraChainName = await paraApi.rpc.system.chain();
  console.log(" Para chain name: ", JSON.stringify(paraChainName));

  let accountIdSize = 32;
  if (paraChainName.startsWith("moon") || paraChainName.startsWith("Moon")) {
    accountIdSize = 20;
  }

  /*
   * get meta data of relay chain
   */
  console.log(" Get metadata of relay chain...");
  const relayMetaData = await relayApi.rpc.state.getMetadata();
  fs.writeFileSync("./relayMetaData.json", JSON.stringify(relayMetaData, null, 2));

  const newAuthoritiesEventIndex = findEventIndex(relayMetaData, "Grandpa", "NewAuthorities");
  const candidateIncludedEventIndex = findEventIndex(relayMetaData, "ParaInclusion", "CandidateIncluded");

  console.log(" Build event decoder for relay chain...");
  await buildEventDecoder(path.resolve("./relayMetaData.json"));

  console.log(" Deploy relay chain event decoder...");
  const relayChainEventDecoder = await deployScore(iconService, wallet, iconNid, __dirname + '/../../eventDecoder/build/libs/eventDecoder-optimized.jar', {});

  /*
   * get meta data of para chain
   */
  console.log(" Get metadata of para chain...");
  const paraMetaData = await paraApi.rpc.state.getMetadata();
  fs.writeFileSync("./paraMetaData.json", JSON.stringify(paraMetaData, null, 2));

  const evmEventIndex = findEventIndex(paraMetaData, "EVM", "Log");

  console.log(" Build event decoder for para chain...");
  await buildEventDecoder(path.resolve("./paraMetaData.json"), accountIdSize);

  console.log(" Deploy para chain event decoder...");
  const paraChainEventDecoder = await deployScore(iconService, wallet, iconNid, __dirname + '/../../eventDecoder/build/libs/eventDecoder-optimized.jar', {});


  console.log(" Build para chain BMV...");
  await buildParaBMV();

  const relayLastBlockHash = await relayApi.rpc.chain.getBlockHash(
    relayChainOffset
  );

  const paraLastBlockHash = await paraApi.rpc.chain.getBlockHash(
    paraChainOffset
  );

  const grandpaAuthoritiesEncoded = await relayApi.rpc.state.getStorage(
    ":grandpa_authorities",
    relayLastBlockHash.toHex()
  );

  const grandpaAuthorities = await relayApi.createType(
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

  const grandpaPrefixHash = xxhashAsHex("Grandpa", 128);
  const curentSetIdKeyHash = xxhashAsHex("CurrentSetId", 128);
  const grandpaCurrentSetIdStorageKey =
    grandpaPrefixHash + curentSetIdKeyHash.replace("0x", "");
  const grandPaCurrentSetIdEndcoded = await relayApi.rpc.state.getStorage(
    grandpaCurrentSetIdStorageKey,
    relayLastBlockHash.toHex()
  );
  const grandPaCurrentSetId = await relayApi.createType(
    "SetId",
    String(grandPaCurrentSetIdEndcoded)
  );

  const parachainInfoPrefixHash = xxhashAsHex("ParachainInfo", 128);
  const parachainIdKeyHash = xxhashAsHex("ParachainId", 128);
  const parachainIdStorageKey =
    parachainInfoPrefixHash + parachainIdKeyHash.replace("0x", "");
  const paraChainIdEncoded = await paraApi.rpc.state.getStorage(
    parachainIdStorageKey,
    paraLastBlockHash.toHex()
  );
  const paraChainId = await paraApi.createType(
    "u32",
    String(paraChainIdEncoded)
  );

  const paraBMVDeployParams = {
    bmc: bmcAddress,
    net: netAddress,
    mtaRootSize: decimalToHex(mtaRootSize),
    mtaCacheSize:  decimalToHex(mtaCacheSize),
    mtaIsAllowNewerWitness: mtaIsAllowNewerWitness ? "0x1" : "0x0",
    relayEventDecoderAddress: relayChainEventDecoder,
    paraEventDecoderAddress: paraChainEventDecoder,
    relayMtaOffset: decimalToHex(relayChainOffset),
    paraMtaOffset: decimalToHex(paraChainOffset),
    relayLastBlockHash: relayLastBlockHash.toHex(),
    paraLastBlockHash: paraLastBlockHash.toHex(),
    relayCurrentSetId: convertLEtoBE(grandPaCurrentSetId.toHex().replace("0x", "")),
    paraChainId: convertLEtoBE(paraChainId.toHex().replace("0x", "")),
    encodedValidators: validatorListBase64Encoded,
    evmEventIndex: "0x" + decimalToHex(evmEventIndex[0]).replace("0x", "") + decimalToHex(evmEventIndex[1]).replace("0x", ""),
    newAuthoritiesEventIndex: "0x" + decimalToHex(newAuthoritiesEventIndex[0]).replace("0x", "") + decimalToHex(newAuthoritiesEventIndex[1]).replace("0x", ""),
    candidateIncludedEventIndex: "0x" + decimalToHex(candidateIncludedEventIndex[0]).replace("0x", "") + decimalToHex(candidateIncludedEventIndex[1]).replace("0x", ""),
  }

  console.log(" Deploy para chain BMV...");
  const paraBMV = await deployScore(iconService, wallet, iconNid, __dirname + '/../../parachain/build/libs/parachain-BMV-optimized.jar', paraBMVDeployParams);

  console.log("\n                --------------------- DONE ---------------------- ");
  console.log("- Relay event decoder score address:        ", relayChainEventDecoder);
  console.log("- Para event decoder score address:         ", paraChainEventDecoder);
  console.log("- Para chain bmv score address:   ", paraBMV);

  await relayApi.disconnect();
  await paraApi.disconnect();
}

main().catch(console.error);
