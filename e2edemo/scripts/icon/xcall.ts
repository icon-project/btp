import {Contract} from "./contract";
import {IconNetwork} from "./network";

export class XCall extends Contract {
  constructor(iconNetwork: IconNetwork, address: string) {
    super(iconNetwork, address)
  }

  getFee(network: string, rollback: boolean) {
    return this.call({
      method: 'getFee',
      params: {
        _net: network,
        _rollback: rollback ? '0x1' : '0x0'
      }
    })
  }

  executeCall(reqId: string) {
    return this.invoke({
      method: 'executeCall',
      params: {
        _reqId: reqId
      }
    })
  }

  executeRollback(sn: string) {
    return this.invoke({
      method: 'executeRollback',
      params: {
        _sn: sn
      }
    })
  }
}
