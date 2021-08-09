
const Rpc = require('isomorphic-rpc')
const rlp = require('rlp')

const { Account, Receipt, Transaction, Header, Proof, Log } = require('eth-object')
const { toBuffer, sha256, keccak, keccakFromHexString } = require('ethereumjs-util')
const { CheckpointTrie } = require("merkle-patricia-tree")

//let localURL = "http://localhost:8545"
let localURL = "http://0.0.0.0:8545"
let testnetURL = "https://data-seed-prebsc-1-s1.binance.org:8545/"

let infuraURL = "https://mainnet.infura.io/v3/890f208d900d46a39f20a38ce7b09f3b"

const rpc = new Rpc(localURL)

let bmv_headerBytes;
let bmv_witness;
let bmv_rp;

const encode = input => (input === '0x0')
    ? rlp.encode(Buffer.alloc(0))
    : rlp.encode(input);

function getBlockHashFromHeader(header) {
    return keccak(rlp.encode(header))
}

function proofRoot(proof) {
    return keccak(rlp.encode(proof[0]))
}



async function receiptProof(txHash) {
    let targetReceipt = await rpc.eth_getTransactionReceipt(txHash)
    if (!targetReceipt) { throw new Error("txhash/targetReceipt not found. (use Archive node)") }

    let rpcBlock = await rpc.eth_getBlockByHash(targetReceipt.blockHash, false)

    let receipts = await Promise.all(rpcBlock.transactions.map((siblingTxHash) => {
        return rpc.eth_getTransactionReceipt(siblingTxHash)
    }))

    let tree = new CheckpointTrie()
    let siblingPath;
    await Promise.all(receipts.map((siblingReceipt, index) => {
        siblingPath = rlp.encode(index)
        let serializedReceipt = Receipt.fromRpc(siblingReceipt).serialize()
        tree.put(siblingPath, serializedReceipt)
        tree.checkpoint()
    }))

    await tree.commit()
    let witness = await tree.get(siblingPath);
    let result = await tree.findPath(rlp.encode(targetReceipt.transactionIndex))

    return {
        header: Header.fromRpc(rpcBlock),
        receiptProof: new Proof(result.stack.map((trieNode) => { return trieNode.raw() })),
        txIndex: targetReceipt.transactionIndex
    }
}

async function trieVerify(proof, path) {
    let encodedProof = []
    for (let i = 0; i < proof.length; i++) {
        encodedProof.push(rlp.encode(proof[i]))
    }
    bmv_rp = rlp.encode(encodedProof);
    let verifyResult=await CheckpointTrie.verifyProof(toBuffer(proofRoot(proof)), path, encodedProof)
    return verifyResult;
}


async function verifyReceipt(txHash, trustedBlockHash) {
    let resp = await receiptProof(txHash)
    let headerEncoded = rlp.encode(resp.header);
    bmv_headerBytes = headerEncoded;
    let blockHashFromHeader = getBlockHashFromHeader(resp.header)
    console.log(resp.header.timestamp.readInt32BE(0));
    console.log(resp.header.parentHash.toString('hex'));
    //console.log(resp.header.number.readBigUInt64BE(0,16));
    if (!toBuffer(trustedBlockHash).equals(blockHashFromHeader)) throw new Error('BlockHash mismatch')
    let receiptsRoot = resp.header[5]
    let receiptsRootFromProof = proofRoot(resp.receiptProof)
    if (!receiptsRoot.equals(receiptsRootFromProof)) throw new Error('ReceiptsRoot mismatch')
    let receiptBuffer = await trieVerify(resp.receiptProof, encode(resp.txIndex))// or leaf
    //return Receipt.fromBuffer(receiptBuffer)
    //return getReceiptFromReceiptProofAt(resp.receiptProof, resp.txIndex)
}


/* 

async function getAccountFromProofAt(proof, address) {
    let accountBuffer = await trieVerify(proof, keccakFromHexString(address))
    return Account.fromBuffer(accountBuffer) // null returned as Account.NULL
}

async function verifyAccount(accountAddress, trustedBlockHash) {
    let resp = await accountProof(accountAddress, trustedBlockHash)
    let blockHashFromHeader = getBlockHashFromHeader(resp.header)
    if (!toBuffer(trustedBlockHash).equals(blockHashFromHeader)) throw new Error('BlockHash mismatch')
    let stateRoot = resp.header[3]
    let stateRootFromProof = proofRoot(resp.accountProof)
    if (!stateRoot.equals(stateRootFromProof)) throw new Error('StateRoot mismatch')
    return getAccountFromProofAt(resp.accountProof, accountAddress)
}

async function accountProof(address, blockHash = null) {
    let rpcBlock, rpcProof

    if (blockHash) {
        rpcBlock = await rpc.eth_getBlockByHash(blockHash, false)
    } else {
        rpcBlock = await rpc.eth_getBlockByNumber('latest', false)
    }

    rpcProof = await rpc.eth_getProof(address, [], rpcBlock.number)

    return {
        header: Header.fromRpc(rpcBlock),
        accountProof: Proof.fromRpc(rpcProof.accountProof),
    }
}


 */




async function transactionProof(txHash) {
    let targetTx = await rpc.eth_getTransactionByHash(txHash)
    if (!targetTx) { throw new Error("Tx not found. Use archive node") }

    let rpcBlock = await rpc.eth_getBlockByHash(targetTx.blockHash, true)

    let tree = new CheckpointTrie()
    let siblingPath;
    await Promise.all(rpcBlock.transactions.map((siblingTx, index) => {
        siblingPath = rlp.encode(index)
        let serializedSiblingTx = Transaction.fromRpc(siblingTx).serialize()
        tree.put(siblingPath, serializedSiblingTx)
        tree.checkpoint()
    }))

    await tree.commit()
    //CheckpointTrie.createProof(tree, 1);
    bmv_witness = await tree.get(siblingPath);
    let result = await tree.findPath(rlp.encode(targetTx.transactionIndex))

    return {
        header: Header.fromRpc(rpcBlock),
        txProof: new Proof(result.stack.map((trieNode) => { return trieNode.raw() })),
        txIndex: targetTx.transactionIndex
    }
}


async function verifyTx(txHash, blockHash) {
    let resp = await transactionProof(txHash)
    let blockHashFromHeader = getBlockHashFromHeader(resp.header)
    if (!toBuffer(blockHash).equals(blockHashFromHeader)) throw new Error('BlockHash mismatch')
    let txRootFromHeader = resp.header[4]
    let txRootFromProof = proofRoot(resp.txProof)
    if (!txRootFromHeader.equals(txRootFromProof)) throw new Error('TxRoot mismatch')
}


async function printBMVParamsForTesting() {
    console.log("###############################- Header Bytes - ########################################")
    console.log(bmv_headerBytes.toString('hex'))
    console.log("----------------------------------------------------------------------------------------")
    console.log("###############################- Witness - ########################################")
    console.log(bmv_witness.toString('hex'))
    console.log("----------------------------------------------------------------------------------------")
    console.log("###############################- Receipt Proof - ########################################")
    console.log(bmv_rp.toString('hex'))
    console.log("----------------------------------------------------------------------------------------")
}

async function main() {
    try {
      /*   
      //to filter the block number which has atleast one transactions to get the latest block number
      for(i = 400; i<= 500; i++){
            let b = await rpc.eth_getBlockByNumber('0x'+i.toString(16), false)
        let blockHash = b.hash;
        let txHash = b.transactions[0];
        if(b.transactions.length>0){
            console.log("block Number"+i)
            console.log(b.number);
            console.log(b.transactions.length);
        }
        } */
        //let b = await rpc.eth_getBlockByNumber('0xA10041', false) //bsc testnet
        // let b = await rpc.eth_getBlockByNumber('0x82A', false)
        //let b = await rpc.eth_getBlockByNumber('0x151', false) // local bsc
        //let b = await rpc.eth_getBlockByNumber('0x38', false)- ganache
        let b = await rpc.eth_getBlockByNumber('0x72', false)//- ganache-latest
        
        let blockHash = b.hash;
        let txHash = b.transactions[0];
        
       // rpcProof = await rpc.eth_getProof(address, [], b.number)
        // /console.log(rlp.encode(b));
        //block 3 in the truffle
        //let blockHash = '0x85642acbc5db1ab1dc8b1478f16e91176477e7aa124ddde45f72bc09e774fb7d'
        //let txHash    = '0x99a6963b4cc21d6e4f1ab64ca4454151ab386fa557ca0000955307e726df1d85'

        // let blockHash = '0xc32470c2459fd607246412e23b4b4d19781c1fa24a603d47a5bc066be3b5c0af'
        // let txHash    = '0xacb81623523bbabccb1638a907686bc2f3229c70e3ab51777bef0a635f3ac03f'
        // let b = await rpc.eth_getBlockByNumber('latest', false)
        //let accountAddress = '0xcd660bc4fcc8b625cff0170c6f6373c275fe8943'
        //let res = await verifyAccount(accountAddress, b.hash)

        await verifyTx(txHash, blockHash)
        let res2 = await verifyReceipt(txHash, blockHash)


        // log index is 4 , change it to 3 after removing the Debug log event in the BMC
        //console.log(res2)
        //console.log(rlp.encode(res.setOfLogs[4]).toString('hex'))
        await printBMVParamsForTesting()
        //res2.setOfLogs[4][1][0].toString('hex') = keccak256(Message(string,uint256,bytes))
        //to try https://emn178.github.io/online-tools/keccak_256.html
        // console.log(Log.fromBuffer(res2.setOfLogs[4]))
        //res2.setOfLogs[1][0].toString('hex')
    } catch (err) {
        console.log(err)
    } finally {
    }
}


main()
    .then(r => "Main started")

/*
const ACCOUNTS_ROOT_INDEX = 3 // within header
const TXS_ROOT_INDEX      = 4 // within header
const RECEIPTS_ROOT_INDEX = 5 // within header

const STORAGE_ROOT_INDEX  = 2 // within account
const SET_OF_LOGS_INDEX   = 3 // within receipt*/