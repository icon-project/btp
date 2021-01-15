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

from iconservice import InterfaceScore, interface, Address


class BMCInterfaceForBSH(InterfaceScore):
    @interface
    def sendMessage(self, _to: str, _svc: str, _sn: int, _msg: bytes):
        """
        Sends the message to a specific network.
        Only allowed to be called by registered BSHs.

        :param _to: String ( Network Address of destination network )
        :param _svc: String ( name of the service )
        :param _sn: Integer ( serial number of the message, must be positive )
        :param _msg: Bytes ( serialized bytes of Service Message )
        """
        pass

    @interface
    def addService(self, _svc: str, _addr: Address):
        """
        Registers the smart contract for the service.
        Called by the operator to manage the BTP network.

        :param _svc: String (the name of the service)
        :param _addr: Address (the address of the smart contract handling the service)
        """
        pass


class BMCInterfaceForBMV(InterfaceScore):
    @interface
    def addVerifier(self, _net: str, _addr: Address):
        """
        Registers BMV for the network.
        Called by the operator to manage the BTP network.

        :param _net: String (Network Address of the blockchain )
        :param _addr: Address (the address of BMV)
        """
        pass


class BMVInterface(InterfaceScore):
    @interface
    def handleRelayMessage(self, _bmc: str, _prev: str, _seq: int, _msg: str) -> list:
        """
        - Decodes Relay Messages and process BTP Messages
        - If there is an error, then it sends a BTP Message containing the Error Message
        - BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.

        :param _bmc: String ( BTP Address of the BMC handling the message )
        :param _prev: String ( BTP Address of the previous BMC )
        :param _seq: Integer ( next sequence number to get a message )
        :param _msg: Bytes ( serialized bytes of Relay Message )
        :return: List of serialized bytes of a BTP Message
        """
        pass

    @interface
    def getStatus(self) -> dict:
        """
        Get status of BMV.
        Used by the relay to resolve next BTP Message to send.
        Called by BMC.

        :return: The object contains followings fields.

            +-------------+---------+--------------------------------------------------+
            | Field       | Type    | Description                                      |
            +=============+=========+==================================================+
            | height      | Integer | height of MerkleTreeAccumulator                  |
            +-------------+---------+--------------------------------------------------+
            | offset      | Integer | offset of MerkleTreeAccumulator                  |
            +-------------+---------+--------------------------------------------------+
            | last_height | Integer | block height of last relayed BTP Message         |
            +-------------+---------+--------------------------------------------------+
        """
        pass


class BSHInterface(InterfaceScore):
    @interface
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

    @interface
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
