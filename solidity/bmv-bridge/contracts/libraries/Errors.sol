// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;

library Errors {
    string internal constant BMV_REVERT_NOT_VERIFIABLE = "26:NotVerifiable";
    string internal constant BMV_REVERT_ALREADY_VERIFIED = "27:AlreadyVerified";
    string internal constant BMV_REVERT_UNAUTHORIZED = "25:Unauthorized";
    string internal constant BMV_REVERT_INVALID_BMC_ADDR = "25:InvalidBMCAddr";
    string internal constant BMV_REVERT_INVALID_PREV_ADDR = "25:InvalidPrevAddr";
}
