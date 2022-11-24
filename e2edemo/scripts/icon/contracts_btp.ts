import {Contract} from "./contract";
import {IconNetwork} from "./network";

export class BMC extends Contract {
  constructor(_iconNetwork: IconNetwork, _address: string) {
    super(_iconNetwork, _address)
  }

  getBtpAddress() {
    return this.call({
      method: 'getBtpAddress'
    })
  }

  addVerifier(network: string, address: string) {
    return this.invoke({
      method: 'addVerifier',
      params: {
        _net: network,
        _addr: address
      }
    })
  }

  addLink(link: string) {
    return this.invoke({
      method: 'addLink',
      params: {
        _link: link
      }
    })
  }

  addRelay(link: string, address: string) {
    return this.invoke({
      method: 'addRelay',
      params: {
        _link: link,
        _addr: address
      }
    })
  }

  addService(service: string, address: string) {
    return this.invoke({
      method: 'addService',
      params: {
        _svc: service,
        _addr: address
      }
    })
  }
}

export class BMV extends Contract {
  constructor(_iconNetwork: IconNetwork, _address: string) {
    super(_iconNetwork, _address)
  }
}
