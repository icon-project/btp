import { ApiPromise, WsProvider } from '@polkadot/api';
import { xxhashAsHex } from '@polkadot/util-crypto';
import * as fs from 'fs';
import * as RLP from 'rlp';
import * as URLSafeBase64 from 'urlsafe-base64';
import { findEventIndex, decimalToHex } from './util'; 

require('dotenv').config()

function convertLEtoBE(input) {
    let result = "";
    for (let i = Math.floor(input.length / 2) - 1; i >= 0; i--) {
        result += input[i * 2];
        if (input[i * 2 + 1]) {
            result += input[i * 2 + 1];
        } else {
            result += "0";
        }
    }

    return "0x" + result.replace(/^0+/, '');
}

async function main() {
    // const wssEndpoint = "wss://rpc.polkadot.io"; // polkadot relay chain
    // const wssEndpoint = "wss://kusama-rpc.polkadot.io"; // kusama relay chain
    // const wssEndpoint = "wss://wss-relay.testnet.moonbeam.network"; // moonbase alpha relay chain

    // const wssEndpoint = "wss://wss.moonriver.moonbeam.network"; // moonriver parachain
    // const wssEndpoint = "wss://wss.testnet.moonbeam.network"; // moonbase alpha parachain
    // const wssEndpoint = "wss://icon-btp.ecl.vn:34008/"; // lecle moonbase parachain

    const relayWssEndpoint = process.env.RELAY_ENDPOINT; // wss endpoint of relay chain
    const paraWssEndpoint = process.env.PARA_ENDPOINT; // wss endpoint of para chain

    const relayChainOffset = process.env.RELAY_CHAIN_OFFSET; // offset of relay chain
    const paraChainOffset = process.env.PARA_CHAIN_OFFSET; // offset of para chain

    const relayWsProvider = new WsProvider(relayWssEndpoint);
    const paraWsProvider = new WsProvider(paraWssEndpoint);
    const relayApi = await ApiPromise.create({
        provider: relayWsProvider,
        types: {
            GrandpaAuthorities: {
                version: "u8",
                authorityList: "AuthorityList",
            }
        }
    });

    const paraApi = await ApiPromise.create({ provider: paraWsProvider });

    // get relay genesis hash
    console.log("relay genesis hash: ", relayApi.genesisHash.toHex());
    // get relay chain name
    const relayChainName = await relayApi.rpc.system.chain();
    console.log("relay chain name: ", JSON.stringify(relayChainName));

    // get relay genesis hash
    console.log("para genesis hash: ", paraApi.genesisHash.toHex());
    // get relay chain name
    const paraChainName = await paraApi.rpc.system.chain();
    console.log("para chain name: ", JSON.stringify(paraChainName));

    /*
     * get meta data of relay chain
     */
    console.log(" Get metadata of relay chain...");
    const relayMetaData = await relayApi.rpc.state.getMetadata();
    fs.writeFileSync("./relayMetaData.json", JSON.stringify(relayMetaData, null, 2));

    const newAuthoritiesEventIndex = findEventIndex(relayMetaData, "Grandpa", "NewAuthorities");
    const candidateIncludedEventIndex = findEventIndex(relayMetaData, "ParasInclusion", "CandidateIncluded");

    /*
     * get meta data of para chain
     */
    console.log(" Get metadata of para chain...");
    const paraMetaData = await paraApi.rpc.state.getMetadata();
    fs.writeFileSync("./paraMetaData.json", JSON.stringify(paraMetaData, null, 2));

    const evmEventIndex = findEventIndex(paraMetaData, "EVM", "Log");

    const relayLastBlockHash = await relayApi.rpc.chain.getBlockHash(relayChainOffset);
    const paraLastBlockHash = await paraApi.rpc.chain.getBlockHash(paraChainOffset);

    const grandpaAuthoritiesEncoded = await relayApi.rpc.state.getStorage(":grandpa_authorities", relayLastBlockHash.toHex());
    // @ts-ignore
    const grandpaAuthorities = await relayApi.createType("GrandpaAuthorities", String(grandpaAuthoritiesEncoded));
    // @ts-ignore
    const validatorList = grandpaAuthorities.authorityList.map(item => item[0].toHex());
    const encoded = RLP.encode(validatorList);
    const validatorListBase64Encoded = URLSafeBase64.encode(encoded);

    const grandpaPrefixHash = xxhashAsHex("Grandpa", 128);
    const curentSetIdKeyHash = xxhashAsHex("CurrentSetId", 128);
    const grandpaCurrentSetIdStorageKey = grandpaPrefixHash + curentSetIdKeyHash.replace("0x", "");
    const grandPaCurrentSetIdEndcoded = await relayApi.rpc.state.getStorage(grandpaCurrentSetIdStorageKey, relayLastBlockHash.toHex());
    const grandPaCurrentSetId = await relayApi.createType("SetId", String(grandPaCurrentSetIdEndcoded));

    const parachainInfoPrefixHash = xxhashAsHex("ParachainInfo", 128);
    const parachainIdKeyHash = xxhashAsHex("ParachainId", 128);
    const parachainIdStorageKey = parachainInfoPrefixHash + parachainIdKeyHash.replace("0x", "");
    const paraChainIdEncoded = await paraApi.rpc.state.getStorage(parachainIdStorageKey, paraLastBlockHash.toHex());
    const paraChainId = await paraApi.createType("u32", String(paraChainIdEncoded));

    const result = {
        relayMtaOffset: "0x" + parseInt(relayChainOffset, 10).toString(16),
        paraMtaOffset: "0x" + parseInt(paraChainOffset, 10).toString(16),
        relayLastBlockHash: relayLastBlockHash.toHex(),
        paraLastBlockHash: paraLastBlockHash.toHex(),
        relayCurrentSetId: convertLEtoBE(grandPaCurrentSetId.toHex().replace("0x", "")),
        paraChainId: convertLEtoBE(paraChainId.toHex().replace("0x", "")),
        evmEventIndex: "0x" + decimalToHex(evmEventIndex[0]).replace("0x", "") + decimalToHex(evmEventIndex[1]).replace("0x", ""),
        newAuthoritiesEventIndex: "0x" + decimalToHex(newAuthoritiesEventIndex[0]).replace("0x", "") + decimalToHex(newAuthoritiesEventIndex[1]).replace("0x", ""),
        candidateIncludedEventIndex: "0x" + decimalToHex(candidateIncludedEventIndex[0]).replace("0x", "") + decimalToHex(candidateIncludedEventIndex[1]).replace("0x", ""),
        encodedValidators: validatorListBase64Encoded,
    }

    fs.writeFileSync('./BMVInitializeData.json', JSON.stringify(result, null, 2));

    await relayApi.disconnect();
    await paraApi.disconnect();
}

main().catch(console.error);