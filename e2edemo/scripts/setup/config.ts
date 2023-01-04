import fs from 'fs';
const {E2E_DEMO_PATH} = process.env
const DEPLOYMENTS_PATH = `${E2E_DEMO_PATH}/deployments.json`

export class Deployments {
  map: Map<string, any>;
  private static instance: Deployments;

  constructor() {
    this.map = new Map();
  }

  public get(target: string) {
    return this.map.get(target);
  }

  public set(target: string, data: any) {
    this.map.set(target, data);
  }

  public static getDefault() {
    if (!this.instance) {
      const data = fs.readFileSync(DEPLOYMENTS_PATH);
      const json = JSON.parse(data.toString());
      this.instance = new this();
      this.instance.set('icon', json.icon);
      this.instance.set('hardhat', json.hardhat);
    }
    return this.instance;
  }

  public save() {
    fs.writeFileSync(DEPLOYMENTS_PATH, JSON.stringify(Object.fromEntries(this.map)), 'utf-8')
  }
}


