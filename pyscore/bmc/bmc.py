from iconservice import *
from ..lib import BTPAddress
from ..lib.btp_interface import BMVInterface, BSHInterface
from ..lib.btp_exception import BTPException, BTPExceptionCode, BMCException
from ..lib.iconee import rlp, BytesDictDB, StringDictDB, AddressDictDB

TAG = 'BTPMessageCenter'


class BMCExceptionCode(BTPExceptionCode):
    UNAUTHORIZED = 1
    INVALID_SN = 2
    ALREADY_EXISTS_BMV = 3
    NOT_EXISTS_BMV = 4
    ALREADY_EXISTS_BSH = 5
    NOT_EXISTS_BSH = 6
    ALREADY_EXISTS_LINK = 7
    NOT_EXISTS_LINK = 8
    UNREACHABLE = 9
    NOT_EXISTS_PERMISSION = 10


class UnauthorizedException(BMCException):
    def __init__(self, message: str):
        super().__init__(message, BMCExceptionCode.UNAUTHORIZED)


class BTPLink(object):
    def __init__(self, addr: BTPAddress, rx_seq: int = 0, reachable: list = None) -> None:
        self.addr = addr
        self.rx_seq = rx_seq
        if reachable is None:
            self.reachable = []
        else:
            self.reachable = reachable

    def __bytes__(self) -> bytes:
        msg = [str(self.addr), self.rx_seq, self.reachable]
        return rlp.rlp_encode(msg)

    def to_bytes(self) -> bytes:
        return bytes(self)

    @staticmethod
    def from_bytes(serialized: bytes) -> 'BTPLink':
        if not isinstance(serialized, bytes) or len(serialized) < 1:
            return None
        unpacked = rlp.rlp_decode(serialized, [str, int, {list: str}])
        return BTPLink(BTPAddress.from_string(unpacked[0]), unpacked[1], unpacked[2])


class BTPLinks(BytesDictDB):

    def __getitem__(self, key: object) -> BTPLink:
        return super().__getitem__(key)

    def bytes_to_object(self, bs: bytes):
        return BTPLink.from_bytes(bs)


class BTPRoutes(StringDictDB):
    def __getitem__(self, key) -> str:
        return super().__getitem__(key)

    def str_to_object(self, s: str):
        return s


class Permission(object):
    _APIS = [
        "addPermission", "removePermission",
        "addService", "removeService",
        "addVerifier", "removeVerifier",
        "addLink", "removeLink",
        "addRoute", "removeRoute",
        "handleRelayMessage", "sendMessage"
    ]

    def __init__(self, apis: str = None):
        if apis is None:
            self.apis = []
        else:
            self.apis = apis.split(",")

    def __str__(self):
        return ",".join(self.apis)

    def has(self, api: str) -> bool:
        if api in self.apis:
            return True
        else:
            return False

    def is_valid(self) -> bool:
        if len(self.apis) == 0:
            return False
        for api in self.apis:
            if api not in self._APIS:
                return False
        return True

    def merge(self, other: 'Permission'):
        for api in other.apis:
            if api not in self.apis:
                self.apis.append(api)

    def remove(self, other: 'Permission'):
        for api in other.apis:
            if api in self.apis:
                self.apis.remove(api)

    def __iter__(self):
        return self.apis.__iter__()

    def __len__(self):
        return len(self.apis)

    @staticmethod
    def from_string(s: str) -> 'Permission':
        if s is None or len(s) == 0:
            return None
        return Permission(s)

    @staticmethod
    def from_string_with_validation(s: str) -> 'Permission':
        v = Permission.from_string(s)
        if v is None or not v.is_valid():
            raise BMCException("invalid Permission")
        return v


class Permissions(StringDictDB):
    def __getitem__(self, key: object) -> Permission:
        return super().__getitem__(key)

    def str_to_object(self, s: str):
        return Permission.from_string(s)


class BTPMessage(object):
    def __init__(self, src: BTPAddress, dst: BTPAddress, svc: str, sn: int, payload: bytes) -> None:
        self.src = src
        self.dst = dst
        self.svc = svc
        self.sn = sn
        self.payload = payload

    def to_bytes(self) -> bytes:
        msg = [str(self.src), str(self.dst), self.svc, self.sn, self.payload]
        return rlp.rlp_encode(msg)

    @staticmethod
    def from_bytes(bs: bytes) -> 'BTPMessage':
        unpacked = rlp.rlp_decode(bs, [str, str, str, int, bytes])
        src = BTPAddress.from_string(unpacked[0])
        dst = BTPAddress.from_string(unpacked[1])
        svc = unpacked[2]
        sn = unpacked[3]
        payload = unpacked[4]
        return BTPMessage(src, dst, svc, sn, payload)


class ErrorMessage(object):
    def __init__(self, code: int, msg: str) -> None:
        self.code = code
        self.msg = msg

    def to_bytes(self) -> bytes:
        msg = [self.code, self.msg]
        return rlp.rlp_encode(msg)

    @staticmethod
    def from_bytes(bs: bytes) -> 'ErrorMessage':
        unpacked = rlp.rlp_decode(bs, [int, str])
        code = unpacked[0]
        msg = unpacked[1]
        return ErrorMessage(code, msg)


class EventMessage(object):
    def __init__(self, evt: str, values: list) -> None:
        self.evt = evt
        self.values = values

    def to_bytes(self) -> bytes:
        msg = [self.evt, self.values]
        return rlp.rlp_encode(msg)

    @staticmethod
    def from_bytes(bs: bytes) -> 'EventMessage':
        unpacked = rlp.rlp_decode(bs, [str, {list: str}])
        evt = unpacked[0]
        values = unpacked[1]
        return EventMessage(evt, values)


class Properties(object):
    _PROPERTIES = 'properties'
    _BTP_ADDR = "btp_addr"
    _ACCESS_CONTROL = "access_control"

    def __init__(self, db: IconScoreDatabase, key: str = _PROPERTIES) -> None:
        self.__db = DictDB(key, db, value_type=bytes)
        self.__btp_addr = None
        self.__access_control = None

    @property
    def btp_addr(self) -> BTPAddress:
        if self.__btp_addr is None:
            self.__btp_addr = BTPAddress.from_bytes(self.__db[self._BTP_ADDR])
        return self.__btp_addr

    @btp_addr.setter
    def btp_addr(self, btp_addr: BTPAddress) -> None:
        self.__db[self._BTP_ADDR] = bytes(btp_addr)
        self.__btp_addr = btp_addr

    @property
    def access_control(self) -> bool:
        if self.__access_control is None:
            if str(self.__db[self._ACCESS_CONTROL]) == "1":
                self.__access_control = True
            else:
                self.__access_control = False
        return self.__access_control

    @access_control.setter
    def access_control(self, access_control: bool) -> None:
        if access_control:
            self.__db[self._ACCESS_CONTROL] = bytes("1", 'utf-8')
        else:
            self.__db[self._ACCESS_CONTROL] = bytes("0", 'utf-8')
        self.__access_control = access_control


class BTPMessageCenter(IconScoreBase):
    _VERIFIERS = 'verifiers'
    _SERVICES = 'services'
    _SEQUENCES = 'sequences'
    _LINKS = 'links'
    _ROUTES = 'routes'
    _PERMISSIONS = 'permissions'

    def __init__(self, db: IconScoreDatabase) -> None:
        super().__init__(db)

        # Initialize here
        self.__properties = Properties(db)

        self._verifiers = AddressDictDB(self._VERIFIERS, db)
        self._services = AddressDictDB(self._SERVICES, db)
        # TODO support BigInt
        self._sequences = DictDB(self._SEQUENCES, db, value_type=int)
        self._links = BTPLinks(self._LINKS, db)
        self._routes = BTPRoutes(self._ROUTES, db)
        self._permissions = Permissions(self._PERMISSIONS, db)

    def on_install(self, _net: str) -> None:
        super().on_install()
        # TODO how to check mismatch chain?
        self.__properties.btp_addr = BTPAddress("btp", _net, str(self.address))
        self.__properties.access_control = False

    def on_update(self) -> None:
        super().on_update()

    @external(readonly=True)
    def btpAddress(self) -> str:
        return str(self.__properties.btp_addr)

    # ================================================
    #  Permission Management
    # ================================================
    @external
    def setAccessControl(self, enable: bool):
        if self.owner == self.msg.sender:
            self.__properties.access_control = enable
        else:
            raise UnauthorizedException

    @external
    def getAccessControl(self) -> bool:
        if self.owner == self.msg.sender:
            return self.__properties.access_control
        else:
            raise UnauthorizedException

    def _has_permission(self, api: str):
        if self.__properties.access_control:
            caller = self.msg.sender
            if caller not in self._permissions:
                if self.tx.origin in self._permissions:
                    caller = self.tx.origin
                else:
                    raise UnauthorizedException

            permission = self._permissions[caller]
            if not permission.has(api):
                raise UnauthorizedException

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
        # FIXME return self._permissions[sender]
        d = {}
        try:
            # FIXME self._has_permission("getPermissions")
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
        De-registers the smart contract for the service.
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
        # FIXME self._has_permission("getServices")
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
        if _net == self.__properties.btp_addr.net:
            raise BMCException("invalid argument, net_addr")
        if _net in self._verifiers:
            raise BMCException("already exists verifier", BMCExceptionCode.ALREADY_EXISTS_BMV)
        self._verifiers[_net] = _addr

    @external
    def removeVerifier(self, _net: str):
        """
        De-registers BMV for the network.
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
        # FIXME self._has_permission("getVerifiers")
        d = {}
        for net_addr in self._verifiers.keys():
            d[net_addr] = self._verifiers[net_addr]
        return d

    # ================================================
    #  Link Management
    # ================================================
    def _add_link(self, _target: str, _reachable: str):
        target = BTPAddress.from_string_with_validation(_target)
        if target.net not in self._verifiers:
            raise BMCException("not exists verifier", BMCExceptionCode.NOT_EXISTS_BMV)

        if len(_reachable) > 0:
            reachable = _reachable.split(",")
        else:
            reachable = []

        if target in self._links:
            link = self._links[target]
            # TODO [TBD] overwrite reachable?
            link.reachable = reachable
        else:
            link = BTPLink(target, 0, reachable)
            self._sequences[_target] = 0

        self._links[target] = link
        evt_msg = EventMessage("Link", [str(self.__properties.btp_addr), _target, _reachable])
        self._propagate_event(evt_msg)

    def _remove_link(self, _target: str):
        target = BTPAddress.from_string_with_validation(_target)
        if target not in self._links:
            raise BMCException("not exists link", BMCExceptionCode.NOT_EXISTS_LINK)
        evt_msg = EventMessage("Unlink", [str(self.__properties.btp_addr), _target, self._links.keys()])
        self._propagate_event(evt_msg)
        self._links.remove(target)
        self._sequences.remove(_target)

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
    def addLink(self, _link: str, _reachable: str):
        """
        If it generates the event related to the link, the relay shall handle the event to deliver BTP Message to the BMC.
        If the link is already registered, or its network is already registered then it fails.
        If there is no verifier related with the network of the link, then it fails.
        Initializes status information for the link.
        Called by the operator to manage the BTP network.

        :param _link: String (BTP Address of connected BMC)
        :param _reachable:
        """
        self._has_permission("addLink")
        if _link in self._links:
            raise BMCException("already exists link", BMCExceptionCode.ALREADY_EXISTS_LINK)
        self._add_link(_link, _reachable)

    @external
    def removeLink(self, _link: str):
        """
        Removes the link and status information.
        Called by the operator to manage the BTP network.

        :param _link: String (BTP Address of connected BMC)
        """
        self._has_permission("removeLink")
        self._remove_link(_link)

    @external(readonly=True)
    def getStatus(self, _link: str) -> dict:
        """
        Get status of BMC.
        Used by the relay to resolve next BTP Message to send.
        If target is not registered, it will fail.

        :param _link: String ( BTP Address of the connected BMC )
        :return: The object contains followings fields.

            +----------+---------+--------------------------------------------------+
            | Field    | Type    | Description                                      |
            +==========+=========+==================================================+
            | tx_seq   | Integer | next sequence number of the next sending message |
            +----------+---------+--------------------------------------------------+
            | rx_seq   | Integer | next sequence number of the message to receive   |
            +----------+---------+--------------------------------------------------+
            | verifier | Object  | status information of the BMV                    |
            +----------+---------+--------------------------------------------------+
        """
        # FIXME self._has_permission("getStatus")
        target = BTPAddress.from_string_with_validation(_link)
        link = self._get_link(target)
        status = {"tx_seq": self._sequences[_link], "rx_seq": link.rx_seq}
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
        # FIXME self._has_permission("getLinks")
        return self._links.keys()

    def _propagate_event(self, evt_msg: EventMessage):
        for target in self._links:
            src = self.__properties.btp_addr
            dst = BTPAddress.from_string(target)
            btp_msg = BTPMessage(src, dst, "_event", 0, evt_msg.to_bytes())
            seq = self._increase_seq(dst)
            self.Message(str(dst), seq, btp_msg.to_bytes())

    def _on_link(self, _from: str, _target: str, _reachable: str):
        src = BTPAddress.from_string(_from)
        link = self._links[src]
        if link is None:
            self._add_link(_from, _target)
        else:
            link.reachable.append(_target)
            self._links[src] = link

    def _on_unlink(self, _from: str, _target: str, _reachable: str):
        src = BTPAddress.from_string(_from)
        link = self._links[src]
        if _target == str(self.__properties.btp_addr):
            self._remove_link(_from)
        else:
            link.reachable = _reachable.split(",")
            self._links[src] = link

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
        self._routes[_dst] = _link

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
        # FIXME self._has_permission("getRoutes")
        d = {}
        for dst in self._routes:
            d[dst] = self._routes[dst]
        return d

    def _resolve_route(self, dst_net: str) -> tuple:
        # dst is BTPAddress.net
        route = self._routes[dst_net]
        if route is None:
            keys = self._links.keys()
            for key in keys:
                addr = BTPAddress.from_string(key)
                if addr.net == dst_net:
                    return self._links[key], addr
            for key in keys:
                link = self._links[key]
                for reachable in link.reachable:
                    addr = BTPAddress.from_string(reachable)
                    if addr.net == dst_net:
                        return self._links[key], link.addr
            raise BMCException(f"unreachable {dst_net}", BMCExceptionCode.UNREACHABLE)
        else:
            return self._links[route], BTPAddress.from_string(route)

    # ================================================
    #  BTP Relay
    # ================================================
    def _get_btp_messages(self, prev: BTPAddress, _msg: str):
        seq = self._get_rx_seq(prev)
        verifier = self._get_verifier(prev.net)
        return verifier.handleRelayMessage(str(self.__properties.btp_addr), str(prev), seq, _msg)

    def _get_rx_seq(self, target: BTPAddress) -> int:
        if target in self._links:
            return self._links[target].rx_seq
        else:
            return 0

    def _increase_seq(self, target: BTPAddress, value: int = 1, rx: bool = False) -> int:
        if value < 1:
            return -1
        if rx:
            link = self._links[target]
            link.rx_seq = link.rx_seq + value
            self._links[target] = link
            return link.rx_seq
        else:
            key = str(target)
            tx_seq = self._sequences[key] + value
            self._sequences[key] = tx_seq
            return tx_seq

    def _handle_btp_message(self, prev: BTPAddress, btp_msg: BTPMessage) -> list:
        svc = btp_msg.svc
        if svc == "_event":
            evt_msg = EventMessage.from_bytes(btp_msg.payload)
            evt = evt_msg.evt
            values = evt_msg.values
            if evt == "Link":
                self._on_link(values[0], values[1], values[2])
            elif evt == "Unlink":
                self._on_unlink(values[0], values[1], values[2])
            else:
                raise BMCException("not exists event handler")
        else:
            if btp_msg.sn >= 0:
                try:
                    service = self._get_service(btp_msg.svc)
                    service.handleBTPMessage(btp_msg.src.net, btp_msg.svc, btp_msg.sn, btp_msg.payload)
                except BTPException as e:
                    self._send_error(prev, btp_msg, e)
            elif btp_msg.sn < 0:
                err_msg = ErrorMessage.from_bytes(btp_msg.payload)
                try:
                    service = self._get_service(btp_msg.svc)
                    service.handleBTPError(str(btp_msg.src), btp_msg.svc, btp_msg.sn * -1, err_msg.code, err_msg.msg)
                except BTPException as e:
                    # [TBD] revert or ignore?
                    self.ErrorOnBTPError(btp_msg.svc, btp_msg.sn * -1, err_msg.code, err_msg.msg, e.code, e.message)

    def _send_error(self, prev: BTPAddress, btp_msg: BTPMessage, e: BTPException):
        if btp_msg.sn != 0:
            err_msg = ErrorMessage(e.code, e.message)
            btp_msg = BTPMessage(self.__properties.btp_addr, btp_msg.src, btp_msg.svc, btp_msg.sn * -1,
                                 err_msg.to_bytes())
            link = self._get_link(prev)
            seq = self._increase_seq(link.addr)
            self.Message(str(link.addr), seq, btp_msg.to_bytes())

    @external
    def handleRelayMessage(self, _prev: str, _msg: str):
        """
        It verifies and decodes the Relay Message with BMV and dispatches BTP Messages to registered BSHs.
        It's allowed to be called by the BMC.

        :param _prev: String ( BTP Address of the previous BMC )
        :param _msg: String ( base64 encoded string of serialized bytes of Relay Message )
        """
        self._has_permission("handleRelayMessage")
        prev = BTPAddress.from_string(_prev)
        serialized_btp_msgs = self._get_btp_messages(prev, _msg)

        # dispatch BTPMessages
        for serialized_btp_msg in serialized_btp_msgs:
            try:
                btp_msg = BTPMessage.from_bytes(serialized_btp_msg)
            except BaseException as e:
                # TODO [TBD] ignore BTPMessage parse failure?
                Logger.warning(f"fail to parse BTPMessage err:{e}", TAG)
            else:
                if btp_msg.dst == self.__properties.btp_addr:
                    self._handle_btp_message(prev, btp_msg)
                else:
                    try:
                        link, dst = self._resolve_route(btp_msg.dst.net)
                        seq = self._increase_seq(link.addr)
                        self.Message(str(link.addr), seq, serialized_btp_msg)
                    except BTPException as e:
                        self._send_error(prev, btp_msg, e)
        self._increase_seq(prev, len(serialized_btp_msgs), rx=True)

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
        link, dst = self._resolve_route(_to)
        if _sn < 0:
            raise BMCException("invalid sn", BMCExceptionCode.INVALID_SN)
        seq = self._increase_seq(link.addr)
        btp_msg = BTPMessage(self.__properties.btp_addr, dst, _svc, _sn, _msg)
        self.Message(str(link.addr), seq, btp_msg.to_bytes())

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
