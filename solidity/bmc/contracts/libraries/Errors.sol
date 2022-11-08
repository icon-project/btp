// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0 <0.8.5;

import "../interfaces/IBMV.sol";

library Errors {
    string internal constant BMC_REVERT_UNAUTHORIZED = "11:Unauthorized";
    string internal constant BMC_REVERT_INVALID_SN = "12:InvalidSn";
    string internal constant BMC_REVERT_ALREADY_EXISTS_BMV = "13:AlreadyExistsBMV";
    string internal constant BMC_REVERT_NOT_EXISTS_BMV = "14:NotExistsBMV";
    string internal constant BMC_REVERT_ALREADY_EXISTS_BSH = "15:AlreadyExistsBSH";
    string internal constant BMC_REVERT_NOT_EXISTS_BSH = "16:NotExistsBSH";
    string internal constant BMC_REVERT_ALREADY_EXISTS_LINK = "17:AlreadyExistsLink";
    string internal constant BMC_REVERT_NOT_EXISTS_LINK = "18:NotExistsLink";
    string internal constant BMC_REVERT_ALREADY_EXISTS_BMR = "19:AlreadyExistsBMR";
    string internal constant BMC_REVERT_NOT_EXISTS_BMR = "20:NotExistsBMR";
    string internal constant BMC_REVERT_UNREACHABLE = "21:Unreachable";
    string internal constant BMC_REVERT_DROP = "22:Drop";
    string internal constant BMC_REVERT_INVALID_ARGUMENT = "10:InvalidArgument";
    string internal constant BMC_REVERT_ALREADY_EXISTS_OWNER = "10:AlreadyExistsOwner";
    string internal constant BMC_REVERT_NOT_EXISTS_OWNER = "10:NotExistsOwner";
    string internal constant BMC_REVERT_LAST_OWNER = "10:LastOwner";
    string internal constant BMC_REVERT_ALREADY_EXISTS_ROUTE = "10:AlreadyExistRoute";
    string internal constant BMC_REVERT_NOT_EXISTS_ROUTE = "10:NotExistsRoute";
    string internal constant BMC_REVERT_REFERRED_BY_ROUTE = "10:ReferredByRoute";
    string internal constant BMC_REVERT_PARSE_FAILURE = "10:ParseFailure";
    string internal constant BMC_REVERT_NOT_EXISTS_INTERNAL = "10:NotExistsInternal";
    string internal constant BMC_REVERT_INVALID_SEQ = "10:InvalidSeq";
    string internal constant BMC_REVERT_LENGTH_MUST_BE_EVEN = "10:LengthMustBeEven";
    string internal constant BMC_REVERT_MUST_BE_POSITIVE = "10:MustBePositive";
    string internal constant BMC_REVERT_NOT_ENOUGH_FEE = "10:NotEnoughFee";
    string internal constant BMC_REVERT_NOT_EXISTS_REWARD = "10:NotExistsReward";
    string internal constant BMC_REVERT_NOT_EXISTS_REQUEST = "10:NotExistsRequest";
    string internal constant BMC_REVERT_NOT_EXISTS_RESPONSE = "10:NotExistsResponse";
}
