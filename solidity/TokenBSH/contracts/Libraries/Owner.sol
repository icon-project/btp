// SPDX-License-Identifier: Apache-2.0

/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pragma solidity >=0.5.0 <=0.8.5;

contract Owner {
    mapping(address => bool) private _owners;
    uint256 private numOfOwner;

    /**
     * @dev Initializes the contract setting the deployer as the initial owner, admin, and operator
     */
    constructor() {
        _owners[msg.sender] = true;
        numOfOwner++;
    }

    modifier owner {
        require(_owners[msg.sender] == true, "No permission");
        _;
    }

    /**
       @notice Adding another Onwer.
       @dev Caller must be an Onwer of BTP network
       @param _owner    Address of a new Onwer.
   */
    function addOwner(address _owner) external owner {
        _owners[_owner] = true;
        numOfOwner++;
    }

    /**
       @notice Removing an existing Owner.
       @dev Caller must be an Owner of BTP network
       @dev If only one Owner left, unable to remove the last Owner
       @param _owner    Address of an Owner to be removed.
   */
    function removeOwner(address _owner) external owner {
        require(numOfOwner > 1, "Unable to remove last Owner");
        delete _owners[_owner];
        numOfOwner--;
    }

    /**
       @notice Checking whether one specific address has Owner role.
       @dev Caller can be ANY
       @param _owner    Address needs to verify.
    */
    function isOwner(address _owner) external view returns (bool) {
        return _owners[_owner];
    }

    /**
       @notice Return a list of addresses of Operators.
       @dev Caller can be ANY
    */
    // function getOperators() external view returns (address[] memory) {
    //     address[] memory res = new address[](numOfOperator);
    //     uint temp;
    //     for (uint i = 0; i < list_addresses.length; i++) {
    //         if (operators[list_addresses[i]]) {
    //             res[temp] = list_addresses[i];
    //             temp++;
    //         }
    //     }
    //     return res;
    // }
}
