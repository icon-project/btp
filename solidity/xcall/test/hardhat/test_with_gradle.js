const { expect } = require("chai");
const hre = require("hardhat");
const { spawn } = require("child_process");

function jsonFilePath(contract, dir) {
    if (dir && !dir.endsWith("/")) {
        dir = dir+"/";
    }
    return `${hre.config.artifactsDir}/contracts/${dir || ''}${contract}.sol/${contract}.json`;
}

function run(command, args) {
   return new Promise((resolve,reject) => {
      const p = spawn(command,args);
      p.stdout.pipe(process.stdout);
      p.stderr.pipe(process.stderr);
      p.on('error', (err) => {
        console.log(`error: ${err}`);
        reject(err);
      });
      /*p.on('exit', (code) => {
        console.log(`exit: ${code}`);
        resolve(code);
      });*/
      p.on('close', (code) => {
        if (code === 0) {
            resolve(code);
        } else {
            reject(`close: ${code}`);
        }
      });
  });
}

describe("Test with gradle", function () {
    it("check balance", async function () {
        const signers = await ethers.getSigners();
        for await (const signer of signers) {
            const bal = await ethers.provider.getBalance(signer.address);
            console.log(`addr:${signer.address} balance:${bal}`)
        }
    });
    it("run test with gradle", async () => {
        console.log(`-Dxcall.jsonFilePath=${jsonFilePath('CallService')}`)
        console.log(`-Dsample.jsonFilePath=${jsonFilePath('DAppProxySample','test')}`)
        await run("../gradlew",[
            'cleanTest',
            ':xcall:test',
            `-Durl=${hre.config.networks[hre.config.defaultNetwork].url}`,
            `-Dxcall.jsonFilePath=${jsonFilePath('CallService')}`,
            `-Dsample.jsonFilePath=${jsonFilePath('DAppProxySample', 'test')}`
        ])
    });
});

