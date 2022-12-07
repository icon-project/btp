import IconService from 'icon-sdk-js';
import {Contract} from "./contract";
import {IconNetwork} from "./network";

const {IconAmount} = IconService;

export class Chain extends Contract {
  constructor(iconNetwork: IconNetwork) {
    super(iconNetwork, 'cx0000000000000000000000000000000000000000')
  }

  getRevision() {
    return this.call({
      method: 'getRevision'
    })
  }

  getMainPReps() {
    return this.call({
      method: 'getMainPReps'
    })
  }

  getPRep(address: string) {
    return this.call({
      method: 'getPRep',
      params: {
        address: address
      }
    })
  }

  getPRepNodePublicKey(address: string) {
    return this.call({
      method: 'getPRepNodePublicKey',
      params: {
        address: address
      }
    })
  }

  registerPRep(name: string) {
    return this.invoke({
      method: 'registerPRep',
      value: '0x' + IconAmount.of(2000, IconAmount.Unit.ICX).toLoop().toString(16),
      params: {
        name: name,
        country: "KOR",
        city: "Seoul",
        email: "test@example.com",
        website: "https://test.example.com",
        details: "https://test.example.com/details",
        p2pEndpoint: "test.example.com:7100"
      }
    })
  }

  setStake(amount: any) {
    return this.invoke({
      method: 'setStake',
      params: {
        value: '0x' + amount.toString(16)
      }
    })
  }

  setDelegation(address: string, amount: any) {
    return this.invoke({
      method: 'setDelegation',
      params: {
        delegations: [{
          address: address,
          value: '0x' + amount.toString(16)
        }]
      }
    })
  }

  setBonderList(address: string) {
    return this.invoke({
      method: 'setBonderList',
      params: {
        bonderList: [address]
      }
    })
  }

  setBond(address: string, amount: any) {
    return this.invoke({
      method: 'setBond',
      params: {
        bonds: [{
          address: address,
          value: '0x' + amount.toString(16)
        }]
      }
    })
  }

  registerPRepNodePublicKey(address: string, pubkey: string) {
    return this.invoke({
      method: 'registerPRepNodePublicKey',
      params: {
        address: address,
        pubKey: pubkey
      }
    })
  }
}

export class Gov extends Contract {
  constructor(iconNetwork: IconNetwork) {
    super(iconNetwork, 'cx0000000000000000000000000000000000000001')
  }

  setRevision(code: number) {
    return this.invoke({
      method: 'setRevision',
      params: {
        code: '0x' + Number(code).toString(16)
      }
    })
  }

  openBTPNetwork(name: string, owner: string) {
    return this.invoke({
      method: 'openBTPNetwork',
      params: {
        networkTypeName: 'eth',
        name: name,
        owner: owner
      }
    })
  }
}
