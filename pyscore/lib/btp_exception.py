#  Copyright 2021 ICON Foundation
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

from iconservice import IconScoreException
from .const import Const


# iconservice.base.exception.ExceptionCode.SCORE_ERROR = 32
# iconservice.base.exception.ExceptionCode.END = 99
class BTPExceptionCode(Const):
    UNKNOWN = 0
    BMC = 10
    BMV = 25
    BSH = 40
    RESERVED = 55


class BTPException(IconScoreException):
    def __init__(self, message: str, code: int = BTPExceptionCode.UNKNOWN):
        if message is None:
            message = str(code)
        super().__init__(message, code)


class BMCException(BTPException):
    def __init__(self, message: str, code: int = 0):
        super().__init__(message, code + BTPExceptionCode.BMC)


class BMVException(BTPException):
    def __init__(self, message: str, code: int = 0):
        super().__init__(message, code + BTPExceptionCode.BMV)


class BSHException(BTPException):
    def __init__(self, message: str, code: int = 0):
        super().__init__(message, code + BTPExceptionCode.BSH)

