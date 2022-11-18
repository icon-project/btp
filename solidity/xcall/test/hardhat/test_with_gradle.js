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

async function runGradleTest(projectName, contractNameMap) {
    let url = hre.config.networks[hre.config.defaultNetwork].url || 'http://localhost:8545';
    let accounts = hre.config.networks[hre.config.defaultNetwork].accounts;
    if (!Array.isArray(accounts)) {
        accounts = hre.userConfig.networks.hardhat.accounts.map((v) => {
            return v.privateKey;
        });
    } else if (typeof accounts[0] === 'object'){
        accounts = accounts.map((v) => {
            return v.privateKey;
        });
    }
    let args = [
        'cleanTest',
        `:${projectName}:test`,
        `-Durl=${url}`,
        `-DprivateKey=${accounts[0]}`,
        `-Dtester.privateKey=${accounts[1]}`
    ]
    if (contractNameMap) {
        for (let [prefix, contractName] of contractNameMap) {
            if (Array.isArray(contractName)) {
                args.push(`-D${prefix}.jsonFilePath=${jsonFilePath(contractName[0], contractName[1])}`)
            } else {
                args.push(`-D${prefix}.jsonFilePath=${jsonFilePath(contractName)}`)
            }
        }
    }
    console.log(args)
    await run("../gradlew",args)
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
        await runGradleTest('xcall', [
            ['xcall', 'CallService'],
            ['sample', ['DAppProxySample', 'test']]
        ]);
    });
});

