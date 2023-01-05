import IconService from 'icon-sdk-js';
import Wallet from "icon-sdk-js/build/Wallet";

const {IconWallet, HttpProvider} = IconService;
const {E2E_DEMO_PATH} = process.env;

export class IconNetwork {
  iconService: IconService;
  nid: number;
  wallet: Wallet;
  private static instance: IconNetwork;

  constructor(_iconService: IconService, _nid: number, _wallet: Wallet) {
    this.iconService = _iconService;
    this.nid = _nid;
    this.wallet = _wallet;
  }

  public static getDefault() {
    if (!this.instance) {
      const httpProvider = new HttpProvider('http://localhost:9080/api/v3');
      const iconService = new IconService(httpProvider);
      const keystore = require(`${E2E_DEMO_PATH}/docker/icon/config/keystore.json`);
      const wallet = IconWallet.loadKeystore(keystore, 'gochain', false);
      this.instance = new this(iconService, 3, wallet);
    }
    return this.instance;
  }

  async getTotalSupply() {
    return this.iconService.getTotalSupply().execute();
  }

  async getLastBlock() {
    return this.iconService.getLastBlock().execute();
  }

  async request(method: string, params: any) {
    return fetch('http://localhost:9080/api/v3', {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        "id": 100,
        "jsonrpc": "2.0",
        "method": method,
        "params": params
      })
    })
    .then((response) => {
      if (response.ok) {
        return response.json();
      }
      throw new Error(`${response.status}: ${response.statusText}`);
    })
    .then((data) => data.result)
    .catch((error) => {
      console.error(error);
    })
  }

  async getBTPNetworkInfo(nid: string) {
    return this.request("btp_getNetworkInfo", {
      "id": nid
    })
  }

  async getBTPHeader(nid: string, height: string) {
    return this.request("btp_getHeader", {
      "networkID": nid,
      "height": height,
    })
  }
}
