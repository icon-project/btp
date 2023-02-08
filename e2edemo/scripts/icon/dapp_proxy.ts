import {Contract} from "./contract";
import {IconNetwork} from "./network";

export class DAppProxy extends Contract {
  constructor(iconNetwork: IconNetwork, address: string) {
    super(iconNetwork, address)
  }

  sendMessage(to: string, data: string, rollback?: string, value?: string) {
    const _params = rollback
      ? {_to: to, _data: data, _rollback: rollback}
      : {_to: to, _data: data}
    return this.invoke({
      method: 'sendMessage',
      value: value ? value : '0x0',
      params: _params
    })
  }
}
