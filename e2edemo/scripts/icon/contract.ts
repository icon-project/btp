import IconService from 'icon-sdk-js';
import Wallet from "icon-sdk-js/build/Wallet";
import {IconNetwork} from "./network";
import Block from "icon-sdk-js/build/data/Formatter/Block";
import BigNumber from "bignumber.js";

const {IconBuilder, IconConverter, SignedTransaction} = IconService;

class EventLog {
  scoreAddress: string | undefined
  indexed: string[] | undefined
  data: string[] | undefined
}

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
          if (error.includes("Pending")) {
            console.log("... pending");
          } else if (error.includes("Executing")) {
            console.log("... executing");
          } else {
            console.log(error);
          }
        });
      if (result) return result
      await sleep(2000);
    }
    throw new Error("Failed to get tx result");
  }

  async filterEvent(eventLogs: any, sig: string, address?: string) {
    const events = <EventLog[]> eventLogs
    for (let i = 0; i < events.length; i++) {
      const evt = events[i]
      if (evt.indexed != undefined && evt.indexed[0] == sig) {
        const _address = address ? address : ''
        if (_address == '' || _address == evt.scoreAddress) {
          return evt;
        }
      }
    }
    throw new Error(`Failed to get event: ${sig}`);
  }

  async getBlock(
      param: string | number | BigNumber | undefined
  ) : Promise<Block> {
    if (param === undefined || param === "latest") {
      return this.iconService.getLastBlock().execute();
    } else if (typeof param === "string") {
      return this.iconService.getBlockByHash(param).execute();
    } else {
      let height: BigNumber;
      if (typeof param === "number") {
        height = new BigNumber(param);
      } else {
        height = param;
      }
      // @ts-ignore
      return this.iconService.getBlockByHeight(height).execute();
    }
  }

  async filterEvents(
      block: string | number | BigNumber | undefined,
      sig: string,
      address?: string | undefined
  ) : Promise<EventLog[]> {
    const blk = await this.getBlock(block);
    return this.filterEventFromBlock(blk, sig, address);
  }

  async filterEventFromBlock(
      block: Block,
      sig: string,
      address?: string | undefined
  ) : Promise<EventLog[]> {
    return Promise.all(
      block.getTransactions()
        .map((tx) =>
            this.iconService.getTransactionResult(tx.txHash).execute()
        )
    ).then((results) => {
      return results.map((result) => {
        const eventLogs = <EventLog[]> result.eventLogs;
        return eventLogs.filter((eventLog) =>
            eventLog.indexed && eventLog.indexed[0] === sig &&
            (address == undefined || address === eventLog.scoreAddress)
        )
      }).flat();
    })
  }

  async queryFilter(
      sig: string,
      fromBlockOrBlockhash?: string | number | BigNumber | undefined,
      toBlock?: string | number | BigNumber | undefined
  ): Promise<EventLog[]> {
    if (fromBlockOrBlockhash === toBlock) {
      return this.filterEvents(fromBlockOrBlockhash, sig, this.address);
    } else {
      let from: Block;
      const to = await this.getBlock(toBlock);
      if (typeof fromBlockOrBlockhash === "number" && <number>fromBlockOrBlockhash < 0) {
        from = await this.getBlock(to.height + <number>fromBlockOrBlockhash);
      } else {
        from = await this.getBlock(fromBlockOrBlockhash);
      }
      let eventLogs = await this.filterEventFromBlock(from, sig, this.address);
      for (let height = from.height + 1; height < to.height; height++) {
        eventLogs = eventLogs.concat(await this.filterEvents(height, sig, this.address));
      }
      return new Promise((resolve, reject) => {
        resolve(eventLogs);
      });
    }
  }
}
