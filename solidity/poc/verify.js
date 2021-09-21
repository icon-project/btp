
const Rpc = require('isomorphic-rpc')
const rlp = require('rlp')

const { Account, Receipt, Transaction, Header, Proof } = require('eth-object')
const { toBuffer, sha256, keccak, keccakFromHexString } = require('ethereumjs-util')
const {CheckpointTrie} = require("merkle-patricia-tree")

let localURL = "http://localhost:8545"
let infuraURL = "https://mainnet.infura.io/v3/890f208d900d46a39f20a38ce7b09f3b"

const rpc = new Rpc(infuraURL)

const encode = input => (input === '0x0')
    ? rlp.encode(Buffer.alloc(0))
    : rlp.encode(input);

function getBlockHashFromHeader(header){
    return keccak(rlp.encode(header))
}

function proofRoot(proof){
    return keccak(rlp.encode(proof[0]))
}

async function transactionProof(txHash){
    let targetTx = await rpc.eth_getTransactionByHash(txHash)
    if(!targetTx){ throw new Error("Tx not found. Use archive node")}

    let rpcBlock = await rpc.eth_getBlockByHash(targetTx.blockHash, true)

    let tree = new CheckpointTrie()

    await Promise.all(rpcBlock.transactions.map((siblingTx, index) => {
        let siblingPath = rlp.encode(index)
        let serializedSiblingTx = Transaction.fromRpc(siblingTx).serialize()
        tree.put(siblingPath, serializedSiblingTx)
        tree.checkpoint()
    }))

    await tree.commit()

    let result = await tree.findPath(rlp.encode(targetTx.transactionIndex))

    return {
        header:  Header.fromRpc(rpcBlock),
        txProof:  new Proof(result.stack.map((trieNode)=>{ return trieNode.raw() })),
        txIndex: targetTx.transactionIndex
    }
}

async function receiptProof(txHash){
    let targetReceipt = await rpc.eth_getTransactionReceipt(txHash)
    if(!targetReceipt){ throw new Error("txhash/targetReceipt not found. (use Archive node)")}

    let rpcBlock = await rpc.eth_getBlockByHash(targetReceipt.blockHash, false)

    let receipts = await Promise.all(rpcBlock.transactions.map((siblingTxHash) => {
        return rpc.eth_getTransactionReceipt(siblingTxHash)
    }))

    let tree = new CheckpointTrie()

    await Promise.all(receipts.map((siblingReceipt, index) => {
        let siblingPath = rlp.encode(index)
        let serializedReceipt = Receipt.fromRpc(siblingReceipt).serialize()
        tree.put(siblingPath, serializedReceipt)
        tree.checkpoint()
    }))

    await tree.commit()

    let result = await tree.findPath(rlp.encode(targetReceipt.transactionIndex))

    return {
        header:  Header.fromRpc(rpcBlock),
        receiptProof:  new Proof(result.stack.map((trieNode)=>{ return trieNode.raw() })),
        txIndex: targetReceipt.transactionIndex
    }
}

async function trieVerify(proof, path){
    let encodedProof = []
    for (let i = 0; i < proof.length; i++) {
        encodedProof.push(rlp.encode(proof[i]))
    }
    return await CheckpointTrie.verifyProof(toBuffer(proofRoot(proof)), path, encodedProof)
}

async function verifyTx(txHash, blockHash) {
    let resp = await transactionProof(txHash)
    let blockHashFromHeader = getBlockHashFromHeader(resp.header)
    if(!toBuffer(blockHash).equals(blockHashFromHeader)) throw new Error('BlockHash mismatch')
    let txRootFromHeader = resp.header[4]
    let txRootFromProof  = proofRoot(resp.txProof)
    if(!txRootFromHeader.equals(txRootFromProof)) throw new Error('TxRoot mismatch')
}

async function verifyReceipt(txHash, trustedBlockHash){
    let resp = await receiptProof(txHash)
    let blockHashFromHeader = getBlockHashFromHeader(resp.header)
    if(!toBuffer(trustedBlockHash).equals(blockHashFromHeader)) throw new Error('BlockHash mismatch')
    let receiptsRoot = resp.header[5]
    let receiptsRootFromProof = proofRoot(resp.receiptProof)
    if(!receiptsRoot.equals(receiptsRootFromProof)) throw new Error('ReceiptsRoot mismatch')
    let receiptBuffer = await trieVerify(resp.receiptProof, encode(resp.txIndex))
    return Receipt.fromBuffer(receiptBuffer)
    //return getReceiptFromReceiptProofAt(resp.receiptProof, resp.txIndex)
}

async function getAccountFromProofAt(proof, address){
    let accountBuffer = await trieVerify(proof, keccakFromHexString(address))
    return Account.fromBuffer(accountBuffer) // null returned as Account.NULL
}

async function verifyAccount(accountAddress, trustedBlockHash){
    let resp = await accountProof(accountAddress, trustedBlockHash)
    let blockHashFromHeader = getBlockHashFromHeader(resp.header)
    if(!toBuffer(trustedBlockHash).equals(blockHashFromHeader)) throw new Error('BlockHash mismatch')
    let stateRoot = resp.header[3]
    let stateRootFromProof = proofRoot(resp.accountProof)
    if(!stateRoot.equals(stateRootFromProof)) throw new Error('StateRoot mismatch')
    return getAccountFromProofAt(resp.accountProof, accountAddress)
}

async function accountProof(address, blockHash = null){
    let rpcBlock, rpcProof

    if(blockHash){
        rpcBlock = await rpc.eth_getBlockByHash(blockHash, false)
    }else{
        rpcBlock = await rpc.eth_getBlockByNumber('latest', false)
    }

    rpcProof = await rpc.eth_getProof(address, [], rpcBlock.number)

    return {
        header: Header.fromRpc(rpcBlock),
        accountProof: Proof.fromRpc(rpcProof.accountProof),
    }
}

async function main() {
    try {

        //let txHash = '0xb3de0442d7566ee983671fd6b73a6376afea815dec29db0cfcd29e2f9da9343c'
        //let blockHash  = '0x459a385f49ac781f2eb42732aa41d9df961d80f990485ec0fb472e431441dd86'


        //let txHash = '0xb3de0442d7566ee983671fd6b73a6376afea815dec29db0cfcd29e2f9da9343c'
        //let blockHash  = '0x459a385f49ac781f2eb42732aa41d9df961d80f990485ec0fb472e431441dd86'
        let blockHash = '0xc32470c2459fd607246412e23b4b4d19781c1fa24a603d47a5bc066be3b5c0af'
        let txHash    = '0xacb81623523bbabccb1638a907686bc2f3229c70e3ab51777bef0a635f3ac03f'

        let res = await verifyReceipt(txHash, blockHash)
        console.log(res)
    } catch (err) {
        console.log(err)
    } finally {
    }
}

main()
    .then(r => "Main started")