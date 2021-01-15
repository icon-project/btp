#  Copyright 2020 ICON Foundation
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

from ..lib import BTPExceptionCode, BMVException


class BMVExceptionCode(BTPExceptionCode):
    INVALID_MPT = 1
    INVALID_VOTES = 2
    INVALID_SEQUENCE = 3
    INVALID_BLOCK_UPDATE = 4
    INVALID_BLOCK_PROOF = 5
    INVALID_BLOCK_WITNESS = 6
    INVALID_SEQUENCE_HIGHER = 7
    INVALID_BLOCK_UPDATE_HEIGHT_HIGHER = 8
    INVALID_BLOCK_UPDATE_HEIGHT_LOWER = 9
    INVALID_BLOCK_PROOF_HEIGHT_HIGHER = 10
    INVALID_BLOCK_WITNESS_OLD = 11


class InvalidMPTException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_MPT)


class InvalidVotesException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_VOTES)


class InvalidBlockUpdateException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_BLOCK_UPDATE)


class InvalidBlockProofException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_BLOCK_PROOF)


class InvalidBlockWitnessException(BMVException):
    def __init__(self, message: str):
        super().__init__(message, BMVExceptionCode.INVALID_BLOCK_WITNESS)

