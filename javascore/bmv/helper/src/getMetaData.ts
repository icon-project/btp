import { ApiPromise, WsProvider } from '@polkadot/api';
import * as fs from 'fs';

async function main () {
    // const wssEndpoint = "wss://rpc.polkadot.io"; // polkadot relay chain
    // const wssEndpoint = "wss://kusama-rpc.polkadot.io"; // kusama relay chain
    // const wssEndpoint = "wss://wss.moonriver.moonbeam.network"; // moonriver parachain
    // const wssEndpoint = "wss://wss-relay.testnet.moonbeam.network"; // moonbase alpha relay chain
    // const wssEndpoint = "wss://wss.testnet.moonbeam.network"; // moonbase alpha parachain
    const wssEndpoint = "CHAIN_ENDPOINT"; // wss endpoint of chain
    const wsProvider = new WsProvider(wssEndpoint);
    const api = await ApiPromise.create({ provider: wsProvider });

    // get genesis hash
    console.log("genesis hash: ", api.genesisHash.toHex());
    // get chain name
    const chain = await api.rpc.system.chain();
    console.log("chain: ", JSON.stringify(chain));

    /*
     * get meta data
     */
    const metaData = await api.rpc.state.getMetadata();
    fs.writeFileSync('./metaData.json', JSON.stringify(metaData, null, 2));
    console.log("----- done ----- ");

    await api.disconnect();
}

main().catch(console.error);