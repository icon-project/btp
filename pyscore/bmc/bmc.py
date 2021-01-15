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

from iconservice import *

from .exception import *
from .link import *
from .message import *
from .permission import *
from .relayer import *
from .route import *
from ..lib import *
from ..lib.iconee import AddressDictDB, PropertiesDB

TAG = 'BTPMessageCenter'
BLOCK_INTERVAL_MSEC = 1000


class BTPMessageCenter(IconScoreBase):
    _PROPS = 'props'
    _VERIFIERS = 'verifiers'
    _SERVICES = 'services'
    _LINKS = 'links'
    _ROUTES = 'routes'
    _PERMISSIONS = 'permissions'
    _RELAYERS = 'relayers'

    class Props(PropertiesDB):
        def __init__(self, prefix, db: IconScoreDatabase) -> None:
            self.btp_addr: BTPAddress = None
            self.access_control = False
            super().__init__(prefix, db)

    def __init__(self, db: IconScoreDatabase) -> None:
        super().__init__(db)

        self._props = BTPMessageCenter.Props(self._PROPS, db)
        self._verifiers = AddressDictDB(self._VERIFIERS, db)
        self._services = AddressDictDB(self._SERVICES, db)
        self._links = BTPLinks(self._LINKS, db)
        self._routes = BTPRoutes(self._ROUTES, db)
        self._relayers = BTPRelayers(self._RELAYERS, db)
        self._permissions = Permissions(self._PERMISSIONS, db)

    def on_install(self, _net: str) -> None:
        super().on_install()
        # TODO how to check mismatch chain?
        self._props.btp_addr = BTPAddress(BTPAddress.PROTOCOL_BTP, _net, str(self.address))
        self._props.access_control = False

    def on_update(self) -> None:
        super().on_update()

    @external(readonly=True)
    def btpAddress(self) -> str:
        return str(self._props.btp_addr)

    # ================================================
    #  Permission Management
    # ================================================
    @external
    def setAccessControl(self, enable: bool):
        if self.owner == self.msg.sender:
            self._props.access_control = enable
        else:
            raise UnauthorizedException("setAccessControl")

    @external
    def getAccessControl(self) -> bool:
        if self.owner == self.msg.sender:
            return self._props.access_control
        else:
            raise UnauthorizedException("getAccessControl")

    def _has_permission(self, api: str):
        if self._props.access_control:
            caller = self.msg.sender
            if caller not in self._permissions:
                if self.tx.origin in self._permissions:
                    caller = self.tx.origin
                else:
                    raise UnauthorizedException(api)

            permission = self._permissions[caller]
            if not permission.has(api):
                raise UnauthorizedException(api)

    @external
    def addPermission(self, _addr: Address, _apis: str):
        self._has_permission("addPermission")
        permission = Permission.from_string_with_validation(_apis)
        if _addr not in self._permissions:
            self._permissions[_addr] = permission
        else:
            org = self._permissions[_addr]
            org.merge(permission)
            self._permissions[_addr] = org

    @external
    def removePermission(self, _addr: Address, _apis: str):
        self._has_permission("removePermission")
        permission = Permission.from_string_with_validation(_apis)
        if _addr not in self._permissions:
            raise BMCException("not exists permission", BMCExceptionCode.NOT_EXISTS_PERMISSION)
        else:
            org = self._permissions[_addr]
            org.remove(permission)
            self._permissions[_addr] = org

    @external(readonly=True)
    def getPermissions(self) -> dict:
        d = {}
        try:
            for addr in self._permissions:
                d[addr] = self._permissions[addr]
        except UnauthorizedException as e:  # FIXME unreachable code
            if self.msg.sender in self._permissions:
                d[self.msg.sender] = self._permissions[self.msg.sender]
            else:
                raise e
        return d

    # ================================================
    #  Service Management
    # ================================================
    def _get_service(self, svc: str) -> BSHInterface:
        if svc not in self._services:
            raise BMCException("not exists service", BMCExceptionCode.NOT_EXISTS_BSH)
        address = self._services[svc]
        return self.create_interface_score(address, BSHInterface)

    @external
    def addService(self, _svc: str, _addr: Address):
        """
        Registers the smart contract for the service.
        Called by the operator to manage the BTP network.

        :param _svc: String (the name of the service)
        :param _addr: Address (the address of the smart contract handling the service)
        """
        self._has_permission("addService")
        if not _svc.isalnum():
            raise BMCException("invalid service name")

        if _svc in self._services:
            raise BMCException("already exists service", BMCExceptionCode.NOT_EXISTS_BSH)
        self._services[_svc] = _addr
        self.addPermission(_addr, "sendMessage")

    def _is_exists_service_by_addr(self, addr: Address) -> bool:
        for svc in self._services:
            if addr == self._services[svc]:
                return True
        return False

    @external
    def removeService(self, _svc: str):
        """
        Unregisters the smart contract for the service.
        Called by the operator to manage the BTP network.

        :param _svc: String (the name of the service)
        """
        self._has_permission("removeService")
        if _svc not in self._services:
            raise BMCException("not exists service", BMCExceptionCode.NOT_EXISTS_BSH)
        addr = self._services[_svc]
        self._services.remove(_svc)
        if not self._is_exists_service_by_addr(addr):
            self.removePermission(addr, "sendMessage")

    @external(readonly=True)
    def getServices(self) -> dict:
        """
        Get registered services.

        :return: A dictionary with the name of the service as key and address of the BSH related to the service as value.

        For example::

        {
            "token": "cx72eaed466599ca5ea377637c6fa2c5c0978537da"
        }
        """
        d = {}
        for svc in self._services:
            d[svc] = self._services[svc]
        return d

    # ================================================
    #  Verifier Management
    # ================================================
    def _get_verifier(self, net_addr: str) -> BMVInterface:
        if net_addr not in self._verifiers:
            raise BMCException("not exists verifier", BMCExceptionCode.NOT_EXISTS_BMV)
        address = self._verifiers[net_addr]
        return self.create_interface_score(address, BMVInterface)

    @external
    def addVerifier(self, _net: str, _addr: Address):
        """
        Registers BMV for the network.
        Called by the operator to manage the BTP network.

        :param _net: String (Network Address of the blockchain )
        :param _addr: Address (the address of BMV)
        """
        self._has_permission("addVerifier")
        # TODO validation _net_addr
        if _net == self._props.btp_addr.net:
            raise BMCException("invalid argument, net_addr")
        if _net in self._verifiers:
            raise BMCException("already exists verifier", BMCExceptionCode.ALREADY_EXISTS_BMV)
        self._verifiers[_net] = _addr

    @external
    def removeVerifier(self, _net: str):
        """
        Unregisters BMV for the network.
        May fail if it's referred by the link.
        Called by the operator to manage the BTP network.

        :param _net: String (Network Address of the blockchain )
        """
        self._has_permission("removeVerifier")
        if _net not in self._verifiers:
            raise BMCException("not exists verifier", BMCExceptionCode.NOT_EXISTS_BMV)
        if self._has_link(_net):
            raise BMCException("cannot remove verifier because exists link")
        self._verifiers.remove(_net)

    @external(readonly=True)
    def getVerifiers(self) -> dict:
        """
        Get registered verifiers.

        :return: A dictionary with the Network Address as a key and smart contract address of the BMV as a value.

        For Example::

        {
            "0x1.iconee": "cx72eaed466599ca5ea377637c6fa2c5c0978537da"
        }
        """
        d = {}
        for net_addr in self._verifiers.keys():
            d[net_addr] = self._verifiers[net_addr]
        return d

    # ================================================
    #  Link Management
    # ================================================
    def _add_link(self, target: BTPAddress):
        if target.net not in self._verifiers:
            raise BMCException("not exists verifier", BMCExceptionCode.NOT_EXISTS_BMV)
        if target in self._links:
            raise BMCException("already exists link", BMCExceptionCode.ALREADY_EXISTS_LINK)
        link = self._links.new_btp_link(target)
        link.block_interval_src = BLOCK_INTERVAL_MSEC
        self._links[target] = link
        evt_msg = EventMessage("Link", [str(self._props.btp_addr), str(target)])
        self._propagate_event(evt_msg)

    def _remove_link(self, target: BTPAddress):
        if target not in self._links:
            raise BMCException("not exists link", BMCExceptionCode.NOT_EXISTS_LINK)
        evt_msg = EventMessage("Unlink", [str(self._props.btp_addr), str(target)])
        self._propagate_event(evt_msg)
        self._links.remove(target)

    def _get_link(self, target: BTPAddress) -> BTPLink:
        if target in self._links:
            return self._links[target]
        else:
            raise BMCException("not exists link", BMCExceptionCode.NOT_EXISTS_LINK)

    def _has_link(self, dst_net: str) -> bool:
        keys = self._links.keys()
        for key in keys:
            addr = BTPAddress.from_string(key)
            if addr.net == dst_net:
                return True
        return False

    @external
    def addLink(self, _link: str):
        """
        If it generates the event related to the link, the relay shall handle the event to deliver BTP Message to the BMC.
        If the link is already registered, or its network is already registered then it fails.
        If there is no verifier related with the network of the link, then it fails.
        Initializes status information for the link.
        Called by the operator to manage the BTP network.

        :param _link: String (BTP Address of connected BMC)
        """
        self._has_permission("addLink")
        target = BTPAddress.from_string_with_validation(_link)
        self._add_link(target)

    @external
    def removeLink(self, _link: str):
        """
        Removes the link and status information.
        Called by the operator to manage the BTP network.

        :param _link: String (BTP Address of connected BMC)
        """
        self._has_permission("removeLink")
        target = BTPAddress.from_string_with_validation(_link)
        self._remove_link(target)

    @external
    def setLink(self, _link: str, _block_interval: int, _max_agg: int, _delay_limit: int):
        """
        Removes the link and status information.
        Called by the operator to manage the BTP network.

        :param _link: String (BTP Address of connected BMC)
        :param _block_interval: Integer
        :param _max_agg: Integer
        :param _delay_limit: Integer
        """
        self._has_permission("setLink")
        target = BTPAddress.from_string_with_validation(_link)
        link = self._get_link(target)
        if _max_agg < 1 or _delay_limit < 1:
            raise BMCException("invalid param")
        reset_rotate_height = True if link.rotate_term() == 0 else False
        link.block_interval_dst = _block_interval
        link.max_aggregation = _max_agg
        link.delay_limit = _delay_limit
        if reset_rotate_height and link.rotate_term() > 0:
            link.rotate_height = self.block_height + link.rotate_term()
            link.rx_height = self.block_height
            verifier = self._get_verifier(link.addr.net)
            link.rx_height_src = verifier.getStatus()["height"]

    @external(readonly=True)
    def getStatus(self, _link: str) -> dict:
        """
        Get status of BMC.
        Used by the relay to resolve next BTP Message to send.
        If target is not registered, it will fail.

        :param _link: String ( BTP Address of the connected BMC )
        :return: The object contains followings fields.

            +---------------+---------+--------------------------------------------------+
            | Field         | Type    | Description                                      |
            +===============+=========+==================================================+
            | tx_seq        | Integer | next sequence number of the next sending message |
            +---------------+---------+--------------------------------------------------+
            | rx_seq        | Integer | next sequence number of the message to receive   |
            +---------------+---------+--------------------------------------------------+
            | verifier      | Object  | status information of the BMV                    |
            +---------------+---------+--------------------------------------------------+
            | relays        | List    | list of status information of BMR                |
            +---------------+---------+--------------------------------------------------+
            | relay_idx     | Integer | |
            +---------------+---------+--------------------------------------------------+
            | rotate_height | Integer | |
            +---------------+---------+--------------------------------------------------+
            | rotate_term   | Integer | |
            +---------------+---------+--------------------------------------------------+
            | delay_limit   | Integer | |
            +---------------+---------+--------------------------------------------------+
            | max_agg       | Integer | |
            +---------------+---------+--------------------------------------------------+
            | rx_height_src | Integer | |
            +---------------+---------+--------------------------------------------------+
            | rx_height     | Integer | |
            +---------------+---------+--------------------------------------------------+
            | block_interval_dst | Integer | |
            +---------------+---------+--------------------------------------------------+
            | block_interval_src | Integer | |
            +---------------+---------+--------------------------------------------------+
            | cur_height    | Integer | |
            +---------------+---------+--------------------------------------------------+
        """
        target = BTPAddress.from_string_with_validation(_link)
        link = self._get_link(target)
        status = {}
        status["tx_seq"] = link.tx_seq
        status["rx_seq"] = link.rx_seq
        status["relay_idx"] = link.relay_idx
        status["rotate_height"] = link.rotate_height
        status["rotate_term"] = link.rotate_term()
        status["delay_limit"] = link.delay_limit
        status["max_agg"] = link.max_aggregation
        status["rx_height_src"] = link.rx_height_src
        status["rx_height"] = link.rx_height
        status["block_interval_dst"] = link.block_interval_dst
        status["block_interval_src"] = link.block_interval_src
        status["cur_height"] = self.block_height

        relays = []
        for relay_addr in link.relays:
            relay = link.relays[relay_addr]
            relay_status = {}
            relay_status["address"] = relay_addr
            relay_status["block_count"] = relay.block_count
            relay_status["msg_count"] = relay.msg_count
            relays.append(relay_status)
        status["relays"] = relays

        verifier = self._get_verifier(target.net)
        status["verifier"] = verifier.getStatus()
        return status

    @external(readonly=True)
    def getLinks(self) -> list:
        """
        Get registered links.

        :return: A list of links ( BTP Addresses of the BMCs )

        For Example::

            [
                "btp://0x1.iconee/cx9f8a75111fd611710702e76440ba9adaffef8656"
            ]
        """
        return self._links.keys()

    def _propagate_event(self, evt_msg: EventMessage):
        for target in self._links:
            src = self._props.btp_addr
            dst = BTPAddress.from_string(target)
            msg = BTPMessage(src, dst, "_event", 0, evt_msg.to_bytes())
            self._send_message(dst, msg.to_bytes())

    def _on_link(self, _from: str, _target: str):
        src = BTPAddress.from_string(_from)
        if src in self._links:
            link = self._links[src]
            if _target not in link.reachable:
                link.reachable.put(_target)
        # elif enable_link_by_event and _target == str(self._props.btp_addr):
        #     self._add_link(src)

    def _on_unlink(self, _from: str, _target: str):
        src = BTPAddress.from_string(_from)
        if src in self._links:
            link = self._links[src]
            remove_from_array_db(link.reachable, _target)
        # elif enable_link_by_event and _target == str(self._props.btp_addr):
        #     self._remove_link(src)

    # ================================================
    #  Route Management
    # ================================================
    @external
    def addRoute(self, _dst: str, _link: str):
        """
        Add route to the BMC.
        May fail if there more than one BMC for the network.
        Called by the operator to manage the BTP network.

        :param _dst: String ( BTP Address of the destination BMC )
        :param _link: String ( BTP Address of the next BMC for the destination )
        """
        self._has_permission("addRoute")
        if _dst in self._routes:
            raise BTPException("already exists route")
        target = BTPAddress.from_string_with_validation(_link)
        self._routes[_dst] = target

    @external
    def removeRoute(self, _dst: str):
        """
        Remove route to the BMC.
        Called by the operator to manage the BTP network.

        :param _dst: String ( BTP Address of the destination BMC )
        """
        self._has_permission("removeRoute")
        if _dst not in self._routes:
            raise BTPException("not found route")
        self._routes.remove(_dst)

    @external(readonly=True)
    def getRoutes(self) -> dict:
        """
        Get routing information.

        :return: A dictionary with the BTP Address of the destination BMC as key and the BTP Address of the next as value.

        For Example::

            {
                "btp://0x2.iconee/cx1d6e4decae8160386f4ecbfc7e97a1bc5f74d35b": "btp://0x1.iconee/cx9f8a75111fd611710702e76440ba9adaffef8656"
            }
        """
        d = {}
        for dst in self._routes:
            d[dst] = self._routes[dst]
        return d

    def _resolve_route(self, dst_net: str) -> tuple:
        # dst_net is BTPAddress.net
        route = self._routes.resolve(dst_net)
        if route is None:
            keys = self._links.keys()
            for key in keys:
                dst = BTPAddress.from_string(key)
                if dst.net == dst_net:
                    return self._links[key], dst
            for key in keys:
                link = self._links[key]
                for reachable in link.reachable:
                    addr = BTPAddress.from_string(reachable)
                    if addr.net == dst_net:
                        return link, link.addr
            raise BMCException(f"unreachable {dst_net}", BMCExceptionCode.UNREACHABLE)
        else:
            return self._links[route], route

    # ================================================
    #  BTP Relay
    # ================================================
    def _handle_msg(self, prev: BTPAddress, msg: BTPMessage) -> list:
        svc = msg.svc
        if svc == "_event":
            evt_msg = EventMessage.from_bytes(msg.payload)
            evt = evt_msg.evt
            values = evt_msg.values
            if evt == "Link":
                self._on_link(values[0], values[1])
            elif evt == "Unlink":
                self._on_unlink(values[0], values[1])
            else:
                raise BMCException("not exists event handler")
        elif svc == "_sack":
            sack = SACKMessage.from_bytes(msg.payload)
            link = self._links[prev]
            link.sack_height = sack.height
            link.sack_seq = sack.seq
        else:
            if msg.sn >= 0:
                try:
                    service = self._get_service(msg.svc)
                    service.handleBTPMessage(msg.src.net, msg.svc, msg.sn, msg.payload)
                except BTPException as e:
                    self._send_error(prev, msg, e)
            elif msg.sn < 0:
                err_msg = ErrorMessage.from_bytes(msg.payload)
                try:
                    service = self._get_service(msg.svc)
                    service.handleBTPError(str(msg.src), msg.svc, msg.sn * -1, err_msg.code, err_msg.msg)
                except BTPException as e:
                    # [TBD] revert or ignore?
                    self.ErrorOnBTPError(msg.svc, msg.sn * -1, err_msg.code, err_msg.msg, e.code, e.message)

    def _send_message(self, to: BTPAddress, serialized_msg: bytes):
        link = self._links[to]
        link.tx_seq = link.tx_seq + 1
        self.Message(str(to), link.tx_seq, serialized_msg)

    def _send_error(self, prev: BTPAddress, msg: BTPMessage, e: BTPException):
        if msg.sn != 0:
            err_msg = ErrorMessage(e.code, e.message)
            msg = BTPMessage(self._props.btp_addr, msg.src, msg.svc, msg.sn * -1,
                             err_msg.to_bytes())
            self._send_message(prev, msg.to_bytes())

    def _send_sack(self, _link: BTPAddress, height: int, seq: int):
        sack = SACKMessage(height, seq)
        msg = BTPMessage(self._props.btp_addr, _link, "_sack", 0, sack.to_bytes())
        self._send_message(_link, msg.to_bytes())

    @external
    def handleRelayMessage(self, _prev: str, _msg: str):
        """
        It verifies and decodes the Relay Message with BMV and dispatches BTP Messages to registered BSHs.
        It's allowed to be called by the BMC.

        :param _prev: String ( BTP Address of the previous BMC )
        :param _msg: String ( base64 encoded string of serialized bytes of Relay Message )
        """

        prev = BTPAddress.from_string(_prev)
        link = self._get_link(prev)
        verifier = self._get_verifier(link.addr.net)
        prev_status = verifier.getStatus()
        # decode and verify relay message
        serialized_msgs = verifier.handleRelayMessage(str(self._props.btp_addr), str(prev), link.rx_seq, _msg)

        # rotate and check valid relay
        status = verifier.getStatus()
        relay = link.rotate(self.block_height,
                            status["last_height"],
                            len(serialized_msgs) > 0)
        if relay is None:
            if self.tx.origin not in link.relays:
                raise UnauthorizedException("not registered relay")
            else:
                relay = link.relays[self.tx.origin]
        elif not relay.addr == self.tx.origin:
            raise UnauthorizedException("invalid relay")

        relay.block_count = relay.block_count + status["height"] - prev_status["height"]
        relay.msg_count = relay.msg_count + len(serialized_msgs)

        # dispatch BTPMessages
        for serialized_msg in serialized_msgs:
            try:
                msg = BTPMessage.from_bytes(serialized_msg)
            except BaseException as e:
                # TODO [TBD] ignore BTPMessage parse failure?
                Logger.warning(f"fail to parse BTPMessage err:{e}", TAG)
            else:
                if msg.dst == self._props.btp_addr:
                    self._handle_msg(prev, msg)
                else:
                    try:
                        next_link, dst = self._resolve_route(msg.dst.net)
                        self._send_message(next_link.addr, serialized_msg)
                    except BTPException as e:
                        self._send_error(prev, msg, e)
        link.rx_seq = link.rx_seq + len(serialized_msgs)
        if link.sack_term > 0 and link.sack_next <= self.block_height:
            self._send_sack(prev, status["height"], link.rx_seq)
            link.sack_next = link.sack_next + link.sack_term

    @external
    def sendMessage(self, _to: str, _svc: str, _sn: int, _msg: bytes):
        """
        Sends the message to a specific network.
        Only allowed to be called by registered BSHs.

        :param _to: String ( Network Address of destination network )
        :param _svc: String ( name of the service )
        :param _sn: Integer ( serial number of the message, must be positive )
        :param _msg: Bytes ( serialized bytes of Service Message )
        """
        self._has_permission("sendMessage")
        if _sn < 0:
            raise BMCException("invalid sn", BMCExceptionCode.INVALID_SN)
        link, dst = self._resolve_route(_to)
        msg = BTPMessage(self._props.btp_addr, dst, _svc, _sn, _msg)
        self._send_message(link.addr, msg.to_bytes())

    @eventlog(indexed=2)
    def Message(self, _next: str, _seq: int, _msg: bytes):
        """
        Sends the message to the next BMC.
        The relay monitors this event.

        indexed: 2

        :param _next: String ( BTP Address of the BMC to handle the message )
        :param _seq: Integer ( sequence number of the message from current BMC to the next )
        :param _msg: Bytes ( serialized bytes of BTP Message )
        """
        pass

    @eventlog(indexed=2)
    def ErrorOnBTPError(self, _svc: str, _sn: int, _code: int, _msg: str, _ecode: int, _emsg: str):
        """ TODO
        raised BTPException while BSH.handleBTPError

        :param _svc:  String ( name of the service )
        :param _sn: Integer ( serial number of the message, must be positive )
        :param _code: Integer (error code )
        :param _msg: String ( error message )
        :param _ecode: Integer ( BTPException code )
        :param _emsg: String ( BTPException message )
        """
        pass

    # ================================================
    #  Relayer Management
    # ================================================
    @external
    def addRelayer(self, _addr: Address, _desc: str):
        """
        Registers Relayer for the network.

        :param _addr: Address (the address of Relayer)
        :param _desc: String (description of Relayer)
        """
        # self._vote_request("addRelayer", _addr, _desc)
        if _addr in self._relayers:
            raise BTPException("already exists relayer")
        relayer = self._relayers.new_relayer(_addr)
        relayer.desc = _desc
        self._relayers[_addr] = relayer

    @external
    def removeRelayer(self, _addr: Address):
        """
        Unregisters Relayer for the network.
        May fail if it's referred by the BMR.

        :param _addr: Address (the address of Relayer)
        """
        # self._vote_request("unregisterRelayer", _addr)
        if _addr not in self._relayers:
            raise BTPException("not found relayer")
        self._relayers.remove(_addr)

    @external(readonly=True)
    def getRelayers(self) -> dict:
        """
        Get registered Relayer.

        :return: A dictionary with the address of relayer as key and description of relayer as value.

        For Example::

        {
            "hx..." : "description of relayer..."
            ...
        }
        """
        d = {}
        for addr in self._relayers:
            relayer = self._relayers[addr]
            d[addr] = relayer.to_dict()
        return d

    # ================================================
    #  Relay Management
    # ================================================
    @external
    def addRelay(self, _link: str, _addr: Address):
        """
        Registers relay for the network.
        Called by the Relay-Operator to manage the BTP network.

        :param _link: String (BTP Address of connected BMC)
        :param _addr: Address (the address of Relay)
        """
        if self.tx.origin not in self._relayers:
            raise UnauthorizedException("not registered relayer")
        target = BTPAddress.from_string_with_validation(_link)
        relayer = self._relayers[self.tx.origin]
        relayer.add_relay(target, _addr)
        link = self._get_link(target)
        link.add_relay(_addr)

    @external
    def removeRelay(self, _link: str, _addr: Address):
        """
        Unregisters relay for the network.
        Called by the Relay-Operator to manage the BTP network.

        :param _link: String (BTP Address of connected BMC)
        :param _addr: Address (the address of Relay)
        """
        if self.tx.origin not in self._relayers:
            raise UnauthorizedException("not registered relayer")
        target = BTPAddress.from_string_with_validation(_link)
        relayer = self._relayers[self.tx.origin]
        relayer.remove_relay(target)
        link = self._get_link(target)
        link.remove_relay(_addr, self.block_height)

    @external(readonly=True)
    def getRelays(self, _link: str) -> list:
        """
        Get registered relays.

        :return: A list of relays.

        For Example::

        [
           "hx..."
            ...
        ]
        """
        target = BTPAddress.from_string_with_validation(_link)
        link = self._get_link(target)
        return link.relays.keys()

    @external
    def nextRelays(self, _link: str, _term: int):
        self._has_permission("nextRelays")
        target = BTPAddress.from_string_with_validation(_link)
        link = self._get_link(target)
        if link.next_relays_term != _term:
            link.next_relays_term = _term
        link.next_relays(self.block_height)

    @external(readonly=True)
    def getNextRelays(self, _link: str) -> list:
        """
        Get registered next relays.

        :return: A list of next relays.

        For Example::

        [
            "hx..."
            ...
        ]
        """
        target = BTPAddress.from_string_with_validation(_link)
        link = self._get_link(target)
        return link.get_next_relays()
