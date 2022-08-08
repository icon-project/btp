// SPDX-License-Identifier: MIT
pragma solidity >=0.4.22 <0.9.0;

import "./strings.sol";

library BTPLib {

    using strings for *;

    struct BTPAddress {
        string net;
        string account;
    }

    function toBTPAddress(string memory self) internal pure returns (BTPAddress memory) {
        strings.slice memory scheme = string("btp://").toSlice();
        strings.slice memory addr = self.toSlice();
        require(addr.startsWith(scheme), "Invalid BTP Scheme");

        addr = addr.beyond(scheme);
        strings.slice memory net = addr.split("/".toSlice());
        require(!net.empty(), "No btp network id");
        require(!addr.empty(), "No btp account id");

        return BTPAddress(
            net.toString(),
            addr.toString()
        );
    }

    // TODO delete
    function beyondScheme(string memory self) internal pure returns (string memory) {
        strings.slice memory nid = self.toSlice();
        strings.slice memory scheme = string("btp://").toSlice();
        if (nid.startsWith(scheme)) {
            return nid.beyond(scheme).toString();
        } else {
            return self;
        }
    }
}
