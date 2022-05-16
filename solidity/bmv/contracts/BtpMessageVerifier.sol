// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./interfaces/IBtpMessageVerifier.sol";
import "./RLPEncode.sol";
import "./RLPReader.sol";
import "solidity-bytes-utils/contracts/BytesLib.sol";

contract BtpMessageVerifier is IBtpMessageVerifier {
    using RLPReader for bytes;
    using RLPReader for RLPReader.RLPItem;
    using RLPReader for RLPReader.Iterator;

    using BytesLib for bytes;
    /*
       int immutable srcNetworkId;
       int immutable networkType;
       int immutable networkId;
     */

    address public immutable bmc;
    address[] private validators;
    bytes private prevNetworkSectionHash;

    constructor(address _bmc, address[] memory _validators) {
        bmc = _bmc;
        for (uint i = 0; i < _validators.length; i++) {
            validators.push(_validators[i]);
        }
    }

    modifier onlyBmc() {
        require(
            msg.sender == bmc,
            "Function must be called through known bmc"
        );
        _;
    }

    function getStatus() external pure returns (uint256) {
        return 1;
    }

    function handleRelayMessage(
        string memory _bmc,
        string memory _prev,
        uint256 _seq,
        bytes memory _msg
    ) external onlyBmc returns (bytes[] memory) {
    }

    function decodeProofContext(bytes memory src) external pure returns (address[] memory) {
        RLPReader.RLPItem memory item = src.toRlpItem();

        // RLP([[address, ...]])
        require(item.isList(), "unexpected proof context format - typeof(src) != array");
        RLPReader.RLPItem[] memory addrs = item.toList();
        require(addrs.length == 1, "unexpected proof context format - length != 1");
        require(addrs[0].isList(), "unexpected proof context format - typeof(src[0]) != array");
        addrs = addrs[0].toList();
        address[] memory ret = new address[](addrs.length);
        for (uint i = 0; i < addrs.length; i++) {
            ret[i] = addrs[i].toAddress();
        }

        return ret;
    }

    function getValidators() external view returns (address[] memory) {
        return validators;
    }

    function quorum(uint nverified, uint nvalidators) external pure returns (bool) {
        return nvalidators * 3 <= nverified * 4;
    }

    function makeNS(
        uint networkId,
        uint rootNumber,
        bytes memory prev,
        uint cnt,
        bytes memory rootHash
    ) external pure returns (bytes32 nsr) {
        bytes[] memory ns = new bytes[](5);
        ns[0] = RLPEncode.encodeUint(networkId);
        ns[1] = RLPEncode.encodeUint(rootNumber);
        ns[2] = RLPEncode.encodeBytes(prev);
        ns[3] = RLPEncode.encodeUint(cnt);
        ns[4] = RLPEncode.encodeBytes(rootHash);
        return keccak256(RLPEncode.encodeList(ns));
    }

    function makeNetworkTypeSectionHash(
        uint srcNetId,
        uint dstNetTypeId,
        uint height,
        uint round,
        bytes memory ntsh
    ) external pure returns (bytes32) {
        bytes[] memory nts = new bytes[](5);
        nts[0] = RLPEncode.encodeUint(srcNetId);
        nts[1] = RLPEncode.encodeUint(dstNetTypeId);
        nts[2] = RLPEncode.encodeUint(height);
        nts[3] = RLPEncode.encodeUint(round);
        nts[4] = RLPEncode.encodeBytes(ntsh);
        return keccak256(RLPEncode.encodeList(nts));
    }


    function makeNetworkTypeSectionHash(
        bytes memory _nsr
    ) external pure returns (bytes32 ntsh) {
        // NetworkTypeSectionHash = RLP.encode([ProofContextHash, NetworkSectionRoot]);
        return keccak256("todo implementation");
    }

    function makeNetworkTypeSectionDecisionHash(
    ) external pure returns (bytes32 hash) {
        return keccak256("todo implementation");
    }

    struct MerklePath {
        uint direction;
        bytes32 value;
    }

    // decode `NetworkSectionToRoot`
    // RLP([[${direction}, ${value}], ...])
    function decodeMerklePath(bytes memory pathes) internal pure returns (MerklePath[] memory) {
        RLPReader.RLPItem[] memory items = pathes.toRlpItem().toList();
        MerklePath[] memory mps = new MerklePath[](items.length);

        for (uint i = 0; i < items.length; i++) {
            RLPReader.RLPItem[] memory item = items[i].toList();
            uint dir = item[0].toUint();
            bytes32 hash = bytes32(item[1].toBytes());
            MerklePath memory mp = MerklePath(dir, hash);
            mps[i] = mp;
        }
        return mps;
    }

    // verify network section
    function calculateMerkleRoot(
        bytes memory _leaf,
        bytes memory _pathes
    ) external pure returns (bytes memory) {
        MerklePath[] memory pathes = decodeMerklePath(_pathes);
        bytes memory temp = abi.encodePacked(keccak256(_leaf));
        for (uint i = 0; i < pathes.length; i++) {
            temp = pathes[i].direction == 0
                ? abi.encodePacked(keccak256(abi.encodePacked(pathes[i].value).concat(temp)))
                : abi.encodePacked(keccak256(temp.concat(abi.encodePacked(pathes[i].value))));
        }
        return temp;
    }

    function recoverSigner(bytes32 message, bytes memory signature)
    public
    pure
    returns (address signer)
    {
            (uint8 v, bytes32 r, bytes32 s) = splitSignature(signature);
            signer = ecrecover(message, v, r, s);
            if (signer == address(0)) {
                revert("fail to recover signer from signature");
            }
    }

    // verify NetworkTypeSectionDecisionProof
    function verifySignature(bytes32 message, bytes[] memory signatures)
    public
    view
    returns (address[] memory) {
        uint nverified = 0;
        require(validators.length == signatures.length,
                "require signing results as many as the number of validators");
        for (uint i = 0; i < validators.length; i++) {
            if (validators[i] == recoverSigner(message, signatures[i])) {
                nverified++;
            }
        }
        require(validators.length * 3 <= nverified * 4, "fail to meet the quorum");
    }

    function splitSignature(bytes memory signature)
    internal
    pure
    returns (uint8, bytes32, bytes32)
    {
        require(signature.length == 65);

        uint8 v;
        bytes32 r;
        bytes32 s;
        assembly {
            v := byte(0, mload(add(signature, 96)))
            r := mload(add(signature, 32))
            s := mload(add(signature, 64))
        }
        return (v, r, s);
    }
}

