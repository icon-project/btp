pragma solidity >=0.5.0 <=0.8.0;

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
  
}
