const ethutils = require('ethereumjs-util');
if (typeof(web3) === undefined) {
    web3 = require('web3');
}

const toHexstr = (buffer) => {
    if (!(buffer instanceof Buffer)) {
        throw new Error('Invalid argument type');
    }
    buffer = buffer.toString('hex');
    return buffer.substr(0, 2) == '0x' ? buffer : '0x' + buffer;
}

const toBuffer = (hexstr) => {
    return Buffer.from(hexstr.substr(
        (hexstr.substr(0, 2) == '0x' ? 2 : 0),
        hexstr.length), 'hex');
}

const sha3 = (msg) => {
    let hash = web3.utils.sha3(msg);
    return hash.substr(2, hash.length);
}

const keyToAccount = (key) => {
    let account = web3.eth.accounts.privateKeyToAccount(key);
    return {
        address: account.address,
        key,
        sign: (m) => {
            let t = account.privateKey;
            let s = ethutils.ecsign(
                Buffer.from(m.substr(2, m.length), 'hex'),
                Buffer.from(t.substr(2, t.length), 'hex')
            );
            return Buffer.concat([s.r, s.s, Buffer.from(s.v.toString(16), 'hex')]);
        }
    }
}


module.exports = {
    ecsign: ethutils.ecsign,
    sha3,
    keyToAccount,
    toBuffer,
    toHexstr,
}
