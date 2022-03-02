const rlp = require('rlp');
const _ = require('lodash');
const assert = require('chai').assert;
const truffleAssert = require('truffle-assertions');
const { sha3_256 } = require('js-sha3')

let testMta = artifacts.require('TestLibMTA');

const nullHash = '0x0000000000000000000000000000000000000000000000000000000000000000';

let sha3FIPS256 = (input) => {
    return '0x' + sha3_256.update(input).hex();
}

let sha3FIPS256Packed = (inputA, inputB) => {
    return '0x' + sha3_256.update(Buffer.from(inputA.slice(2), 'hex')).update(Buffer.from(inputB.slice(2), 'hex')).toString();
}

let toHex = (buf) => { 
    buf = buf.toString('hex');
    if (buf.substring(0, 2) == '0x')
        return buf;
    return '0x' + buf.toString('hex');
};

let toRLPBytes = (data) => {
    return rlp.encode([
        data.height,
        data.roots,
        data.offset,
        data.rootsSize,
        data.cacheSize,
        data.cache,
        data.isAllowNewerWitness
    ]);
};

let rootHashes = [
    sha3FIPS256('root 1'),
    sha3FIPS256('root 2'),
    sha3FIPS256('root 3'),
    sha3FIPS256('root 4')
];

let rootCaches = [
    sha3FIPS256('cache 1'),
    sha3FIPS256('cache 2'),
    sha3FIPS256('cache 3'),
    sha3FIPS256('cache 4'),
    sha3FIPS256('cache 5')
];

let data = {
    height: 10,
    roots: rootHashes,
    offset: 10,
    rootsSize: 4,
    cacheSize: 5,
    cache: rootCaches,
    isAllowNewerWitness: 0
};

let initData = {
    height: 2,
    roots: [],
    offset: 2,
    rootsSize: 4,
    cacheSize: 5,
    cache: [],
    isAllowNewerWitness: 1
};

let expected = {
    height: data.height.toString(),
    roots: data.roots,
    offset: data.offset.toString(),
    rootsSize: data.rootsSize.toString(),
    cacheSize: data.cacheSize.toString(),
    cache: data.cache,
    isAllowNewerWitness: (data.isAllowNewerWitness === 1) ? true : false
}

contract('MTA library unit tests', async () => {
    let testLibMta;

    beforeEach(async () => {
        testLibMta = await testMta.new();
    });

    it('should initialize Merkel Tree Accumulator with given RLP bytes', async () => {
        await testLibMta.initFromSerialized(toRLPBytes(data));

        const mta = await testLibMta.getMTA();
        assert.deepEqual(mta, Object.values(expected), 'mta data should match initial data');
    });

    it('should update offset of MTA', async () => {
        await testLibMta.setOffset(4);

        const mta = await testLibMta.getMTA();
        assert.equal(mta.offset, 4, 'failed to update offset in MTA');
    });

    it('should get root hash by index', async () => {
        await testLibMta.initFromSerialized(toRLPBytes(data));

        const rootHash = await testLibMta.getRoot(1);
        assert.equal(rootHash, rootHashes[1], 'return incorrect root hash');

        await truffleAssert.reverts(testLibMta.getRoot(4), 'root index is out of range');
    });

    it('should check whether a root is in cache', async () => {
        await testLibMta.initFromSerialized(toRLPBytes(data));

        let checkCache = await testLibMta.doesIncludeCache('0x');
        assert.equal(checkCache, false, 'invalid empty hash param');

        checkCache = await testLibMta.doesIncludeCache(rootCaches[4]);
        assert.equal(checkCache, true, 'root is not in cache');

        checkCache = await testLibMta.doesIncludeCache('0x12345');
        assert.equal(checkCache, false, 'root is in cache');
    });

    it('should push a cache into caches list in MTA', async () => {
        await testLibMta.initFromSerialized(toRLPBytes(data));

        const newCacheRoot = sha3FIPS256('new cache');
        await testLibMta.putCache(newCacheRoot);

        const mta = await testLibMta.getMTA();
        
        let expected = _.drop([...rootCaches, newCacheRoot], 1);

        assert.include(mta.cache, newCacheRoot, 'failed to add cache');
        assert.deepEqual(mta.cache, expected, 'failed to update cache list in MTA');
    });

    it('should add hash roots to MTA', async () => {
        await testLibMta.initFromSerialized(toRLPBytes(initData)); // rootsSize = 4, cacheSize = 5

        /* add first item 'dog'
         * root    [   hash(dog)   ]
         * data    [     dog       ]
         * cache   [     dog       ]
         */
        const root1 = sha3FIPS256('dog');
        await testLibMta.add(root1);

        let mta = await testLibMta.getMTA();
        let h = mta[0]; // height
        let r = mta[1]; // roots
        let c = mta[5]; // cache

        assert.equal(h, initData.height + 1);
        assert.equal(r[0], root1);
        assert.deepEqual(c, [root1]);

        /* add second item 'dog'
         * root (higher item first)   [     0x0      hash(dog, cat)   ]
         * data                       [     dog          cat          ]
         * cache                      [     dog          cat          ]
         */
        const root2 = sha3FIPS256('cat'); 
        const root12 = sha3FIPS256Packed(root1, root2);
        await testLibMta.add(root2);

        mta = await testLibMta.getMTA();
        h = mta[0]; // height
        r = mta[1]; // roots
        c = mta[5]; // cache

        assert.equal(h, initData.height + 2);
        assert.equal(r[0], nullHash);
        assert.equal(r[1], root12);
        assert.deepEqual(c, [root1, root2]);

        /* add third item 'snake'
         * root       [   hash(snake)     hash(dog, cat)  ]
         * data       [      dog               cat           snake    ]
         * cache      [      dog               cat           snake    ]
         */
        const root3 = sha3FIPS256('snake');
        await testLibMta.add(root3);

        mta = await testLibMta.getMTA();
        h = mta[0]; // height
        r = mta[1]; // roots
        c = mta[5]; // cache

        assert.equal(h, initData.height + 3);
        assert.equal(r[0], root3);
        assert.equal(r[1], root12);
        assert.deepEqual(c, [root1, root2, root3]);

        /* add 4th item 'pig'
         * root       [    0x0    0x0    hash(hash(dog, cat), hash(snake, pig))]
         * data       [    dog    cat    snake    pig  ]
         * cache      [    dog    cat    snake    pig  ]
         */
        const root4 = sha3FIPS256('pig');
        const root34 = sha3FIPS256Packed(root3, root4);
        const root1234 = sha3FIPS256Packed(root12, root34);
        await testLibMta.add(root4);

        mta = await testLibMta.getMTA();
        h = mta[0]; // height
        r = mta[1]; // roots
        c = mta[5]; // cache

        assert.equal(h, initData.height + 4);
        assert.equal(r[0], nullHash);
        assert.equal(r[1], nullHash);
        assert.equal(r[2], root1234);
        assert.deepEqual(c, [root1, root2, root3, root4]);

        /* add 5th item 'chicken'
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * cache   [     dog     cat     snake     pig     chicken     ]
         */
        const root5 = sha3FIPS256('chicken');
        await testLibMta.add(root5);

        mta = await testLibMta.getMTA();
        h = mta[0]; // height
        r = mta[1]; // roots
        c = mta[5]; // cache

        assert.equal(h, initData.height + 5);
        assert.equal(r[0], root5);
        assert.equal(r[1], nullHash);
        assert.equal(r[2], root1234);
        assert.deepEqual(c, [root1, root2, root3, root4, root5]);

        /* add 6th item 'cow'
         * root    [     0x0     hash(chicken, cow)      hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     cow     ]
         * cache   [             cat     snake     pig     chicken     cow     ]
         */
        const root6 = sha3FIPS256('chicken');
        const root56 = sha3FIPS256Packed(root5, root6);
        await testLibMta.add(root6);

        mta = await testLibMta.getMTA();
        h = mta[0]; // height
        r = mta[1]; // roots
        c = mta[5]; // cache

        assert.equal(h, initData.height + 6);
        assert.equal(r[0], nullHash);
        assert.equal(r[1], root56);
        assert.equal(r[2], root1234);
        assert.deepEqual(c, [root2, root3, root4, root5, root6]);

        /* add 7th item 'fish'
         * root    [     hash(fish)     hash(chicken, cow)      hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     cow     fish     ]
         * cache   [                     snake     pig     chicken     cow     fish     ]
         */
        const root7 = sha3FIPS256('fish');
        await testLibMta.add(root7);

        mta = await testLibMta.getMTA();
        h = mta[0]; // height
        r = mta[1]; // roots
        c = mta[5]; // cache

        assert.equal(h, initData.height + 7);
        assert.equal(r[0], root7);
        assert.equal(r[1], root56);
        assert.equal(r[2], root1234);
        assert.deepEqual(c, [root3, root4, root5, root6, root7]);

        /* add 8th item 'wolf'
         * root    [     0x0     0x0     0x0     hash(hash(hash(dog, cat), hash(snake, pig)),hash(hash(chicken, cow), hash(fish, wolf))  ]
         * data    [     dog     cat     snake     pig     chicken     cow     fish     wolf     ]
         * cache   [                               pig     chicken     cow     fish     wolf     ]
         */
        const root8 = sha3FIPS256('wolf');
        const root78 = sha3FIPS256Packed(root7, root8);
        const root5678 = sha3FIPS256Packed(root56, root78);
        const root12345678 = sha3FIPS256Packed(root1234, root5678);
        await testLibMta.add(root8);

        mta = await testLibMta.getMTA();
        h = mta[0]; // height
        r = mta[1]; // roots
        c = mta[5]; // cache

        assert.equal(h, initData.height + 8);
        assert.equal(r[0], nullHash);
        assert.equal(r[1], nullHash);
        assert.equal(r[2], nullHash);
        assert.equal(r[3], root12345678);
        assert.deepEqual(c, [root4, root5, root6, root7, root8]);
    });

    it('should get root index by block height', async () => {
        /*
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */
        await testLibMta.initFromSerialized(toRLPBytes(initData));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');

        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);


        // find item with height = 3 (snake)
        let idx = await testLibMta.getRootIndexByHeight(5);
        assert.equal(idx, 2);

        // find item with height = 5 (chicken)
        idx = await testLibMta.getRootIndexByHeight(7);
        assert.equal(idx, 0);
    });

    it('should verify leaf if height of BMV\'s MTA  is equal to relay ones', async () => {
        /* BMV's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */

        /* Relay's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */

        await testLibMta.initFromSerialized(toRLPBytes(initData));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');

        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);

        const root34 = sha3FIPS256Packed(root3, root4);
        const witness = [root6, root34];
        
        // prove item 5 (snake)
        await testLibMta.verify(witness, root5, 5, 7);
    });

    it('should verify leaf if height of BMV\'s MTA  is less than relay ones', async () => {
        /* BMV's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */

        /* Relay's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     cow     fish      ]
         * height  [     3       4       5         6       7           8       9         ]
         * offset  = 2
         */

        await testLibMta.initFromSerialized(toRLPBytes(initData));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');

        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);

        const root34 = sha3FIPS256Packed(root3, root4);
        const witness = [root6, root34];
        
        // prove item 5 (snake)
        await testLibMta.verify(witness, root5, 5, 9);
    });

    it('should verify leaf if height of BMV\'s MTA  is less than relay ones with different offset', async () => {
        /* BMV's MTA
         * rootIdx [     0              1                       2                                          ]
         * root    [     hash(fish)     hash(chicken, cow)      hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     cow     fish     ]
         * height  [     3       4       5         6       7           8       9        ]
         * offset  = 2
         */

        /* Relay's MTA
         * rootIdx [     0              1       2                                              ]
         * root    [     hash(lion)     0x0     hash(hash(chicken, cow), hash(fish, wolf))     ]
         * data    [     chicken     cow     fish     wolf     lion     ]
         * height  [     7           8       9        10       11       ]
         * offset  = 6
         */

        await testLibMta.initFromSerialized(toRLPBytes(initData));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');
        const root8 = sha3FIPS256('cow');
        const root9 = sha3FIPS256('fish');
        const root10 = sha3FIPS256('wolf');


        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);
        await testLibMta.add(root8);
        await testLibMta.add(root9);

        const root78 = sha3FIPS256Packed(root7, root8);
        const witness = [root10, root78];
        
        // prove item 9 (fish)
        await testLibMta.verify(witness, root9, 9, 11);
    });

    it('should fail to verify leaf if proofs (witness) are modified and revert', async () => {
        /* BMV's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */

        /* Relay's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */

        await testLibMta.initFromSerialized(toRLPBytes(initData));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');

        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);

        const root34 = sha3FIPS256Packed(root3, root4);
        const witness = [root6, root34];

        // modify witness
        witness[1] = sha3FIPS256('fake pig');

        await truffleAssert.reverts(testLibMta.verify.call(witness, root5, 5, 7), 'BMVRevertInvalidBlockWitness: invalid witness');
    });

    it('should fail to verify leaf if newer witnesses are not allowed ', async () => {
        /* BMV's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */

        /* Relay's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     cow     fish      ]
         * height  [     3       4       5         6       7           8       9         ]
         * offset  = 2
         */

        await testLibMta.initFromSerialized(toRLPBytes({ ...initData, isAllowNewerWitness: 0 }));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');

        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);

        const root34 = sha3FIPS256Packed(root3, root4);
        const witness = [root6, root34];
        
        await truffleAssert.reverts(testLibMta.verify.call(witness, root5, 5, 9), 'BMVRevertInvalidBlockWitness: not allowed newer witness');
    });

    it('should fail to verify leaf that haven\'t synced yet in BMV MTA', async () => {
        /* BMV's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         */

        /* Relay's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * data    [     dog     cat     snake     pig     chicken     cow     fish      ]
         * height  [     3       4       5         6       7           8       9         ]
         * offset  = 2
         */

        await testLibMta.initFromSerialized(toRLPBytes(initData));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');
        const root8 = sha3FIPS256('cow');
        const root9 = sha3FIPS256('fish');
        const root10 = sha3FIPS256('wolf');


        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);

        const root78 = sha3FIPS256Packed(root7, root8);
        const witness = [root10, root78];
        
        await truffleAssert.reverts(testLibMta.verify.call(witness, root9, 9, 9), 'BMVRevertInvalidBlockWitness: given witness for newer node');
    });

    it('should verify by cache', async () => {
        /* BMV's MTA
         * root idx   [     0                   1                    2                                             ]
         * root       [     hash(bird)          hash(tiger, lion)       hash(hash(chicken, cow), hash(fish, wolf)) ]
         * data       [     chicken     cow     fish     wolf     tiger     lion     bird    ]
         * cache      [                 cow     fish     wolf     tiger     lion     bird    ]
         * height     [     10          11      12       13       14        15       16      ]
         * offset = 9
         */

        /* Relay's MTA
         * root idx   [     0                   1                       2                                          ]
         * root       [     hash(fish)          hash(chicken, cow)      hash(hash(dog, cat), hash(snake, pig))     ]
         * data       [     dog     cat      snake     pig     chicken     cow     fish      ]
         * height     [     6       7        8         9       10          11      12        ]
         * offset = 5
         */
        await testLibMta.initFromSerialized(toRLPBytes({ ...initData, height: 9, offset: 9, cacheSize: 6 }));

        const root10 = sha3FIPS256('chicken');
        const root11 = sha3FIPS256('cow');
        const root12 = sha3FIPS256('fish');
        const root13 = sha3FIPS256('wolf');
        const root14 = sha3FIPS256('tiger');
        const root15 = sha3FIPS256('lion');
        const root16 = sha3FIPS256('bird');

        await testLibMta.add(root10);
        await testLibMta.add(root11);
        await testLibMta.add(root12);
        await testLibMta.add(root13);
        await testLibMta.add(root14);
        await testLibMta.add(root15);
        await testLibMta.add(root16);

        const witness = [root10];

        // prove item 11 (cow)
        await testLibMta.verify(witness, root11, 11, 12);
    });

    it('should fail to verify by cache and revert', async () => {
        /* BMV's MTA
         * root idx   [     0                   1                    2                                             ]
         * root       [     hash(bird)          hash(tiger, lion)       hash(hash(chicken, cow), hash(fish, wolf)) ]
         * data       [     chicken     cow     fish     wolf     tiger     lion     bird    ]
         * cache      [                         fish     wolf     tiger     lion     bird    ]
         * height     [     10          11      12       13       14        15       16      ]
         * offset = 9
         */

        /* Relay's MTA
         * root idx   [     0                   1                       2                                          ]
         * root       [     hash(fish)          hash(chicken, cow)      hash(hash(dog, cat), hash(snake, pig))     ]
         * data       [     dog     cat      snake     pig     chicken     cow     fish      ]
         * height     [     6       7        8         9       10          11      12        ]
         * offset = 5
         */
        await testLibMta.initFromSerialized(toRLPBytes({ ...initData, height: 9, offset: 9 }));

        const root10 = sha3FIPS256('chicken');
        const root11 = sha3FIPS256('cow');
        const root12 = sha3FIPS256('fish');
        const root13 = sha3FIPS256('wolf');
        const root14 = sha3FIPS256('tiger');
        const root15 = sha3FIPS256('lion');
        const root16 = sha3FIPS256('bird');

        await testLibMta.add(root10);
        await testLibMta.add(root11);
        await testLibMta.add(root12);
        await testLibMta.add(root13);
        await testLibMta.add(root14);
        await testLibMta.add(root15);
        await testLibMta.add(root16);

        const witness = [root10];

        // prove item 11 (cow)
        await truffleAssert.reverts(testLibMta.verify.call(witness, root11, 11, 12), 'BMVRevertInvalidBlockWitness: invalid old witness');
    });

    it('should fail to verify by old witness cache', async () => {
        /* BMV's MTA
         * root idx   [     0                   1                    2                                             ]
         * root       [     hash(bird)          hash(tiger, lion)       hash(hash(chicken, cow), hash(fish, wolf)) ]
         * data       [     chicken     cow     fish     wolf     tiger     lion     bird    ]
         * cache      [                         fish     wolf     tiger     lion     bird    ]
         * height     [     10          11      12       13       14        15       16      ]
         * offset = 9
         */

        /* Relay's MTA
         * root idx   [     0                   1                       2                                          ]
         * root       [     hash(fish)          hash(chicken, cow)      hash(hash(dog, cat), hash(snake, pig))     ]
         * data       [     dog     cat      snake     pig     chicken     cow     fish      ]
         * height     [     6       7        8         9       10          11      12        ]
         * offset = 5
         */
        await testLibMta.initFromSerialized(toRLPBytes({ ...initData, height: 9, offset: 9 }));

        const root10 = sha3FIPS256('chicken');
        const root11 = sha3FIPS256('cow');
        const root12 = sha3FIPS256('fish');
        const root13 = sha3FIPS256('wolf');
        const root14 = sha3FIPS256('tiger');
        const root15 = sha3FIPS256('lion');
        const root16 = sha3FIPS256('bird');

        await testLibMta.add(root10);
        await testLibMta.add(root11);
        await testLibMta.add(root12);
        await testLibMta.add(root13);
        await testLibMta.add(root14);
        await testLibMta.add(root15);
        await testLibMta.add(root16);

        const witness = [root11];

        // prove item 10 (cow)
        await truffleAssert.reverts(testLibMta.verify.call(witness, root11, 10, 12), 'BMVRevertInvalidBlockWitnessOld');
    });

    it('should get bytes encoding of MTA', async () => {
        /* BMV's MTA
         * rootIdx [     0                 1       2                                          ]
         * root    [     hash(chicken)     0x0     hash(hash(dog, cat), hash(snake, pig))     ]
         * cache   [     dog     cat     snake     pig     chicken     ]
         * height  [     3       4       5         6       7           ]
         * offset  = 2
         * isAllowNewerWitness = true
         */
        await testLibMta.initFromSerialized(toRLPBytes(initData));
        const root3 = sha3FIPS256('dog');
        const root4 = sha3FIPS256('cat');
        const root5 = sha3FIPS256('snake');
        const root6 = sha3FIPS256('pig');
        const root7 = sha3FIPS256('chicken');
        const root3456 = sha3FIPS256Packed(
            sha3FIPS256Packed(root3, root4),
            sha3FIPS256Packed(root5, root6)
        );

        await testLibMta.add(root3);
        await testLibMta.add(root4);
        await testLibMta.add(root5);
        await testLibMta.add(root6);
        await testLibMta.add(root7);

        let expected = rlp.encode([
            initData.height + 5,
            [root7, nullHash, root3456],
            initData.offset,
            initData.rootsSize,
            initData.cacheSize,
            [root3, root4, root5, root6, root7],
            initData.isAllowNewerWitness
        ]);

        const rlpBytes = await testLibMta.toRlpBytes();
        assert.equal(rlpBytes, toHex(expected));
    });
});