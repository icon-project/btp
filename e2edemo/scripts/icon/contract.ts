import IconService from 'icon-sdk-js';
import Wallet from "icon-sdk-js/build/Wallet";
import {IconNetwork} from "./network";
import Block from "icon-sdk-js/build/data/Formatter/Block";
import BigNumber from "bignumber.js";
import TransactionResult from "icon-sdk-js/build/data/Formatter/TransactionResult";
import ConfirmedTransaction from "icon-sdk-js/build/data/Formatter/ConfirmedTransaction";

const {IconBuilder, IconConverter, SignedTransaction} = IconService;

export class EventLog {
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

  filterEvent(eventLogs: any, sig: string, address?: string) : Array<EventLog> {
    return (<Array<EventLog>>eventLogs).filter((eventLog) =>
        eventLog.indexed && eventLog.indexed[0] === sig &&
        (!address || address === eventLog.scoreAddress)
    )
  }

  async getBlock(
      param: string | number | BigNumber | undefined
  ) : Promise<Block> {
    if (param === undefined || param === "latest") {
      return this.iconService.getLastBlock().execute();
    } else if (typeof param === "string") {
      return this.iconService.getBlockByHash(param).execute();
    } else {
      const height = BigNumber.isBigNumber(param) ? param : new BigNumber(param as number);
      // @ts-ignore
      return this.iconService.getBlockByHeight(height).execute();
    }
  }

  async filterEventFromBlock(
      block: Block,
      sig: string,
      address?: string | undefined
  ) : Promise<EventLog[]> {
    return Promise.all(
      block.getTransactions()
        .map((tx : ConfirmedTransaction) =>
            this.iconService.getTransactionResult(tx.txHash).execute()
        )
    ).then((results) => {
      return results.map((result: TransactionResult) =>
          this.filterEvent(result.eventLogs as Array<EventLog>, sig, address)
      ).flat();
    })
  }

  async queryFilter(
      sig: string,
      fromBlockOrBlockhash?: string | number | BigNumber | undefined,
      toBlock?: string | number | BigNumber | undefined
  ): Promise<Array<EventLog>> {
    if (fromBlockOrBlockhash === toBlock || !toBlock) {
      const block = await this.getBlock(fromBlockOrBlockhash);
      return this.filterEventFromBlock(block, sig, this.address);
    } else {
      let from: Block;
      const to = await this.getBlock(toBlock);
      if (typeof fromBlockOrBlockhash === "number" && fromBlockOrBlockhash < 0) {
        from = await this.getBlock(to.height + fromBlockOrBlockhash);
      } else if (BigNumber.isBigNumber(fromBlockOrBlockhash) && fromBlockOrBlockhash.toNumber() < 0) {
        from = await this.getBlock(to.height + fromBlockOrBlockhash.toNumber());
      } else {
        from = await this.getBlock(fromBlockOrBlockhash);
      }

      let ret = await this.filterEventFromBlock(from, sig, this.address);
      for (let height = from.height + 1; height < to.height; height++) {
        const block = await this.getBlock(height);
        const eventLogs = await this.filterEventFromBlock(block, sig, this.address);
        ret = ret.concat(eventLogs);
      }
      ret = ret.concat(await this.filterEventFromBlock(to, sig, this.address));
      return new Promise((resolve, reject) => {
        resolve(ret);
      });
    }
  }

  async waitEvent(
      sig: string,
  ) {
    let latest = await this.getBlock("latest");
    let height = latest.height -1;
    let block = await this.getBlock(height);
    while (true) {
      while (height < latest.height){
        const events = await this.filterEventFromBlock(block, sig, this.address);
        if (events.length > 0) {
          return events;
        }
        height++;
        if (height == latest.height) {
          block = latest;
        } else {
          block = await this.getBlock(height);
        }
      }
      await new Promise((resolve) => setTimeout(resolve, 1000));
      latest = await this.getBlock("latest");
    }
  }
}
