import {Contract} from "./contract";
import {IconNetwork} from "./network";

export class DAppProxy extends Contract {
  constructor(iconNetwork: IconNetwork, address: string) {
    super(iconNetwork, address)
  }

  sendMessage(to: string, data: string, value?: string) {
    return this.invoke({
      method: 'sendMessage',
      value: value ? value : '0x0',
      params: {
        _to: to,
        _data: data
      }
    })
  }
}
