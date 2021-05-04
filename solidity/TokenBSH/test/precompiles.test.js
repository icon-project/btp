const Precompiles = artifacts.require("PrecompilesMock");
contract('Precompiles', function (accounts) {

    it("should execute sha3fips Precompiled fucntion", function () {
        input = "0x0448250ebe88d77e0a12bcf530fe6a2cf1ac176945638d309b840d631940c93b78c2bd6d16f227a8877e3f1604cd75b9c5a8ab0cac95174a8a0a0f8ea9e4c10bca"
        return Precompiles.deployed().then(function (instance) {
            return instance.sha3fips.call(input);
        }).then(function (hash) {
            assert.equal(hash, 0xc7647f7e251bf1bd70863c8693e93a4e77dd0c9a689073e987d51254317dc704, "The output value is different than expected");
        });
    });

    it("should execute ecrecoverPublicKey Precompiled fucntion", function () {
        hash = "0xc5d6c454e4d7a8e8a654f5ef96e8efe41d21a65b171b298925414aa3dc061e37"
        v = "0x00"
        r = "0x4011de30c04302a2352400df3d1459d6d8799580dceb259f45db1d99243a8d0c"
        s = "0x64f548b7776cb93e37579b830fc3efce41e12e0958cda9f8c5fcad682c610795"
        return Precompiles.deployed().then(function (instance) {
            return instance.ecrecoverPublicKey.call(hash, v, r, s);
        }).then(function (pubkey) {
            assert.equal(pubkey, 0x0448250ebe88d77e0a12bcf530fe6a2cf1ac176945638d309b840d631940c93b78c2bd6d16f227a8877e3f1604cd75b9c5a8ab0cac95174a8a0a0f8ea9e4c10bca, "The output value is different than expected");
        });

    });
});
