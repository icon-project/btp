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

  async getLastBlock() {
    return this.iconService.getLastBlock().execute();
  }
}
