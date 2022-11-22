import IconService from 'icon-sdk-js';
import Wallet from "icon-sdk-js/build/Wallet";
import {IconNetwork} from "./network";

const {IconBuilder, IconConverter, SignedTransaction} = IconService;

export class Contract {
  protected iconService: IconService;
  protected nid: number;
  protected wallet: Wallet;
  public address: string = '';

  constructor(iconNetwork: IconNetwork, _address?: string) {
    this.iconService = iconNetwork.iconService;
    this.nid = iconNetwork.nid;
    this.wallet = iconNetwork.wallet;
    this.address = _address || '';
  }

  call({method, params}: {
    method: string;
    params?: {
      [key: string]: any;
    };
  }) {
    const payload =  new IconBuilder.CallBuilder()
      .to(this.address)
      .method(method)
      .params(params)
      .build();
    return this.iconService.call(payload).execute();
  }

  invoke({method, params, value = '0'}: {
    method: string;
    value?: string;
    params?: {
      [key: string]: any;
    };
  }) {
    const payload = new IconBuilder.CallTransactionBuilder()
      .method(method)
      .params(params)
      .from(this.wallet.getAddress())
      .to(this.address)
      .nid(this.nid)
      .version(3)
      .timestamp(new Date().getTime() * 1000)
      .stepLimit(IconConverter.toBigNumber(80000000))
      .value(value)
      .build();

    const signedTx = new SignedTransaction(payload, this.wallet);
    return this.iconService.sendTransaction(signedTx).execute();
  }

  deploy({content, params}: {
    content: string;
    params?: {
      [key: string]: any;
    };
  }) {
    const payload = new IconBuilder.DeployTransactionBuilder()
      .contentType('application/java')
      .content(`0x${content}`)
      .params(params)
      .from(this.wallet.getAddress())
      .to('cx0000000000000000000000000000000000000000')
      .nid(this.nid)
      .version(3)
      .timestamp((new Date()).getTime() * 1000)
      .stepLimit(IconConverter.toBigNumber(2500000000))
      .build();

    const signedTx = new SignedTransaction(payload, this.wallet);
    return this.iconService.sendTransaction(signedTx).execute();
  }

  async getTxResult(txHash: string) {
    function sleep(millis: number) {
      return new Promise(resolve => setTimeout(resolve, millis));
    }
    for (let i = 0; i < 5; i++) {
      const result = await this.iconService.getTransactionResult(txHash).execute()
        .then((res) => {
          if (res.status == 1 && res.scoreAddress) {
            this.address = res.scoreAddress;
          }
          return res;
        })
        .catch((error) => {
          console.log(error);
        });
      if (result) return result
      await sleep(2000);
    }
    throw new Error("Failed to get tx result");
  }
}
