module.exports = {
    ZB32: '0x0000000000000000000000000000000000000000000000000000000000000000',
    toBytesString: (s) => {
        return '0x' + Buffer.from(s).toString('hex');
    },
    toStr: (h) => {
        h = h.slice(0, 2) == '0x' ? h.slice(2, h.length) : h;
        return Buffer.from(h, 'hex').toString();
    },
}
