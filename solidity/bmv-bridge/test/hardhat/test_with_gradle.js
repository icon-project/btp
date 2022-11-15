const { expect } = require("chai");
const hre = require("hardhat");
const { spawn } = require("child_process");

function jsonFilePath(contract) {
    return `${hre.config.artifactsDir}/contracts/${contract}.sol/${contract}.json`;
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
        console.log(`-Dbmv.jsonFilePath=${jsonFilePath('BMV')}`)
        await run("../gradlew",[
            'cleanTest',
            ':bmv-bridge:test',
            `-Durl=${hre.config.networks[hre.config.defaultNetwork].url}`,
            `-Dbmv.jsonFilePath=${jsonFilePath('BMV')}`
        ])
    });
});

