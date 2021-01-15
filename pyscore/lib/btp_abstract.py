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

from iconservice import ABC, abstractmethod


class BTPServiceHandler(ABC):
    @abstractmethod
    def handleBTPMessage(self, _from: str, _svc: str, _sn: int, _msg: bytes):
        """
        Handles BTP Messages from other blockchains.
        Accepts messages only from BMC.
        If it fails, then BMC will generate a BTP Message that includes error information, then delivered to the source.

        :param _from: String ( Network Address of source network )
        :param _svc: String ( name of the service )
        :param _sn: Integer ( serial number of the message )
        :param _msg: Bytes ( serialized bytes of ServiceMessage )
        """
        pass

    @abstractmethod
    def handleBTPError(self, _src: str, _svc: str, _sn: int, _code: int, _msg: str):
        """
        Handle the error on delivering the message.
        Accept the error only from the BMC.

        :param _src: String ( BTP Address of BMC that generated the error )
        :param _svc: String ( name of the service )
        :param _sn: Integer ( serial number of the original message )
        :param _code: Integer ( code of the error )
        :param _msg: String ( message of the error )
        """
        pass
