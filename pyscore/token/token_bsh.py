from iconservice import *
from ..lib.btp_address import BTPAddress
from ..lib.btp_exception import BTPException, BTPExceptionCode, BSHException
from ..lib.btp_interface import BMCInterfaceForBSH

TAG = "TokenBSH"


class IRC2Interface(InterfaceScore):
    @interface
    def transfer(self, _from: Address, _value: int, _data: bytes = None):
        pass


def _to_obj(_data: bytes) -> any:
    return json_loads(_data.decode('utf-8'))


def _to_bytes(_data: any) -> bytes:
    return json_dumps(_data).encode('utf-8')


class BSHExceptionCode(BTPExceptionCode):
    UNAUTHORIZED = 1
    DUPLICATED_ITEM = 2
    NO_SUCH_ITEM = 3
    INVALID_VALUE_AMOUNT = 4
    OVERDRAWN = 5
    INVALID_BTP_ADDR = 6


class UnauthorizedException(BSHException):
    def __init__(self, message: str):
        super().__init__(message, BSHExceptionCode.UNAUTHORIZED)


class DuplicatedItemException(BSHException):
    def __init__(self, message: str):
        super().__init__(message, BSHExceptionCode.DUPLICATED_ITEM)


class NoSuchItemException(BSHException):
    def __init__(self, message: str):
        super().__init__(message, BSHExceptionCode.NO_SUCH_ITEM)


class InvalidValueAmountException(BSHException):
    def __init__(self, message: str):
        super().__init__(message, BSHExceptionCode.INVALID_VALUE_AMOUNT)


class OverdrawnException(BSHException):
    def __init__(self, message: str):
        super().__init__(message, BSHExceptionCode.OVERDRAWN)


class TokenBSH(IconScoreBase):
    # BSH service name
    SERVICE_NAME = 'token'

    # For service message type
    REQ_TOKEN_TRANSFER = "req_token_transfer"
    RES_TOKEN_TRANSFER = "res_token_transfer"
    RES_UNKNOWN_TYPE = "res_unknown_type"

    # For indexes of message in `pending_db`
    # For common prop of service message
    PD_TYPE_IDX = 0
    # For prop of `req_token_transfer` message
    PD_FROM_IDX = 1
    PD_TO_IDX = 2
    PD_ARGS_NAME_IDX = 3
    PD_ARGS_VALUE_IDX = 4
    # For prop of `res_token_transfer` message
    PD_CODE_IDX = 1

    # For result code of handled service
    RC_OK = 0
    RC_ERR_UNREGISTERED_TOKEN = -1

    # For indexes of `_balance_db`
    BD_USABLE_IDX = 0
    BD_LOCKED_IDX = 1

    @eventlog(indexed=2)
    def TransferStart(self, _from: Address, _tokenName: str, _sn: int, _value: int, _to: str):
        pass

    @eventlog(indexed=1)
    def TransferEnd(self, _sn: int, _code: int, _msg: bytes):
        pass

    def __init__(self, db: IconScoreDatabase) -> None:
        super().__init__(db)
        self._bmc_db = VarDB('bmc', db, value_type=Address)
        self._sn_db = VarDB('sn', db, value_type=int)
        # _pending_db[sn<int>] = b'[type, ...(vargs of dependent on `type`)]
        self._pending_db = DictDB('pending', db, value_type=bytes)
        self._token_addr_db = DictDB('token_addr', db, value_type=Address)
        self._token_name_db = ArrayDB('token_name', db, value_type=str)

        # balance_db[owner<Address>][tokenName<str>] = [usable<str>, locked<str>]
        self._balance_db = DictDB('balance', db, value_type=bytes, depth=2)

    def on_install(self, _bmc: Address) -> None:
        super().on_install()
        self._bmc_db.set(_bmc)

    def on_update(self) -> None:
        super().on_update()

    @external(readonly=True)
    def tokenNames(self) -> dict:
        names = {}
        for name in self._token_name_db:
            names[name] = self._token_addr_db[name]
        return names

    @external(readonly=True)
    def addressOf(self, _tokenName: str) -> Address:
        addr = None
        for name in self._token_name_db:
            if name == _tokenName:
                addr = self._token_addr_db[name]
                break
        return addr

    @external(readonly=True)
    def balanceOf(self, _owner: Address) -> dict:
        res = {}
        for name in self._token_name_db:
            balance = self._get_balance(_owner, name)
            usable = balance['usable']
            locked = balance['locked']
            if usable or locked:
                res[name] = {
                    'usable': usable,
                    'locked': locked
                }
        return res

    @external(readonly=True)
    def isPending(self, _sn: int) -> bool:
        return self._pending_db[_sn] is not None

    @external
    def register(self, _name: str, _addr: Address):
        if self.owner != self.msg.sender:
            raise UnauthorizedException("Not allowed caller")
        if self._token_addr_db[_name] is not None:
            raise DuplicatedItemException("Duplicated token name")

        self._token_addr_db[_name] = _addr
        self._token_name_db.put(_name)

    @external
    def unregister(self, _name: str):
        if self.owner != self.msg.sender:
            raise UnauthorizedException("Not allowed caller")
        if self._token_addr_db[_name] is None:
            raise NoSuchItemException("Not registered token")

        # remove item and rearrange db of `array` type
        found = False
        count = len(self._token_name_db)
        for i in range(count):
            if not found and _name == self._token_name_db[i]:
                found = True
            if found:
                if i < count - 1:
                    self._token_name_db[i] = self._token_name_db[i + 1]
                else:
                    self._token_name_db.pop()

    @external
    def tokenFallback(self, _from: Address, _value: int, _data: bytes):
        if _value < 0:
            raise InvalidValueAmountException("Not allowed value amount")

        target_name = ""
        for name in self._token_name_db:
            if self.msg.sender == self._token_addr_db[name]:
                target_name = name
                break

        if len(target_name) <= 0:
            raise NoSuchItemException("Not registered token")

        self._update_balance_by(_from, target_name, _value, 0)

    @external
    def reclaim(self, _tokenName: str, _value: int):
        if _value < 0:
            raise InvalidValueAmountException("Not allowed value amount")

        token_addr = self._token_addr_db[_tokenName]
        if token_addr is None:
            raise NoSuchItemException("Not registered token")

        sender = self.msg.sender
        balance = self._get_balance(sender, _tokenName)
        if balance['usable'] < _value:
            raise OverdrawnException("Overdrawn")

        self._update_balance_by(sender, _tokenName, -_value, 0)
        score = self.create_interface_score(token_addr, IRC2Interface)
        score.transfer(sender, _value)

    @external
    def transfer(self, _tokenName: str, _to: str, _value: int):
        token_addr = self._token_addr_db[_tokenName]
        if token_addr is None:
            raise NoSuchItemException("Not registered token")
        if _value < 0:
            raise InvalidValueAmountException("Not allowed value amount")

        to = {}
        try:
            to = BTPAddress.from_string_with_validation(_to)
        except BTPException:
            raise BSHException("Invalid BTPAddress format", BSHExceptionCode.INVALID_BTP_ADDR)

        sender = self.msg.sender
        balance = self._get_balance(sender, _tokenName)
        if balance['usable'] < _value:
            raise OverdrawnException("Overdrawn")

        self._update_balance_by(sender, _tokenName, -_value, _value)

        sn = self._generate_serial_number()
        msg = TokenBSH._create_message(self.REQ_TOKEN_TRANSFER,
                                       sender, to.contract, _tokenName, _value)

        self._put_pending(sn, msg)
        self._send_message(sn, to.net, msg)
        self.TransferStart(sender, _tokenName, sn, _value, to.contract)

    @external
    def handleBTPMessage(self, _from: str, _svc: str, _sn: int, _msg: bytes):
        if self._bmc_db.get() != self.msg.sender:
            raise UnauthorizedException("Not allowed caller")
        Logger.info(f'_from:{_from}, _svc:{_svc}, _sn:{_sn}', TAG)

        req = _to_obj(_msg)
        if req['type'] == self.REQ_TOKEN_TRANSFER:
            user = Address.from_string(req['to'])
            value = int(req['args']['value'], 16)
            token_name = req['args']['name']
            Logger.info(f'user:{user}:{isinstance(user,Address)}, value:{value}, name:{token_name}', TAG)

            if self._token_addr_db[token_name] is not None:
                self._update_balance_by(user, token_name, value, 0)
                code = self.RC_OK
            else:
                code = self.RC_ERR_UNREGISTERED_TOKEN

            # send response message for `req_token_transfer`
            res = TokenBSH._create_message(self.RES_TOKEN_TRANSFER, code)
            self._send_message(_sn, _from, res)

        elif req['type'] == self.RES_TOKEN_TRANSFER:
            if self._has_pending(_sn) is False:
                raise NoSuchItemException("No pending message")

            pmsg = self._get_pending(_sn)
            user = pmsg['from']
            value = pmsg['args']['value']
            token_name = pmsg['args']['name']

            if req['code'] is self.RC_OK:
                self._update_balance_by(user, token_name, 0, -value)
            else:
                self._update_balance_by(user, token_name, value, -value)
            self._del_pending(_sn)
            self.TransferEnd(_sn, req['code'], _msg)

        else:
            res = TokenBSH._create_message(self.RES_UNKNOWN_TYPE)
            self._send_message(_sn, _from, res)

    @external
    def handleBTPError(self, _src: str, _svc: str, _sn: int, _code: int, _msg: str):
        if self._bmc_db.get() != self.msg.sender:
            raise UnauthorizedException("Not allowed caller")
        if self._has_pending(_sn) is False:
            raise NoSuchItemException("No pending message")

        # rollback `token transfer`
        pmsg = self._get_pending(_sn)
        if pmsg['type'] == self.REQ_TOKEN_TRANSFER:
            user = pmsg['from']
            value = pmsg['args']['value']
            token_name = pmsg['args']['name']
            self._update_balance_by(user, token_name, value, -value)

        # delete pending message
        self._del_pending(_sn)

    @staticmethod
    def _create_message(_type: str, *_args) -> dict:
        if _type == TokenBSH.REQ_TOKEN_TRANSFER:
            return {
                'type': _type,
                'from': str(_args[0]),
                'to': _args[1],
                'args': {
                    'name': _args[2],
                    'value': hex(_args[3])
                }
            }

        elif _type == TokenBSH.RES_TOKEN_TRANSFER:
            return {
                'type': _type,
                'code': _args[0]
            }

        elif _type == TokenBSH.RES_UNKNOWN_TYPE:
            return {
                'type': _type
            }

    def _send_message(self, _sn: int, _to: str, _msg: any):
        bmc_addr = self._bmc_db.get()
        score = self.create_interface_score(bmc_addr, BMCInterfaceForBSH)
        score.sendMessage(_to, self.SERVICE_NAME, _sn, _to_bytes(_msg))

    def _generate_serial_number(self):
        sn = self._sn_db.get() + 1
        self._sn_db.set(sn)
        return sn

    def _put_pending(self, _sn: int, _msg: dict):
        pmsg = self._pending_db[_sn]
        if _msg['type'] == self.REQ_TOKEN_TRANSFER:
            pmsg = [_msg['type'], _msg['from'], _msg['to'],
                    _msg['args']['name'], _msg['args']['value']]
        elif _msg['type'] == self.RES_TOKEN_TRANSFER:
            pmsg = [_msg['type'], _msg['code']]
        self._pending_db[_sn] = _to_bytes(pmsg)

    def _get_pending(self, _sn: int) -> dict:
        pmsg = self._pending_db[_sn]
        pmsg = _to_obj(pmsg)
        if pmsg[self.PD_TYPE_IDX] == self.REQ_TOKEN_TRANSFER:
            return {
                'type': pmsg[self.PD_TYPE_IDX],
                'from': Address.from_string(pmsg[self.PD_FROM_IDX]),
                'to': pmsg[self.PD_TO_IDX],
                'args': {
                    'name': pmsg[self.PD_ARGS_NAME_IDX],
                    'value': int(pmsg[self.PD_ARGS_VALUE_IDX], 16)
                }
            }
        elif pmsg[self.PD_TYPE_IDX] == self.RES_TOKEN_TRANSFER:
            return {
                'type': pmsg[self.PD_TYPE_IDX],
                'code': pmsg[self.PD_CODE_IDX]
            }
        elif pmsg[self.PD_TYPE_IDX] == self.RES_UNKNOWN_TYPE:
            return {
                'type': pmsg[self.PD_TYPE_IDX]
            }

    def _del_pending(self, _sn):
        self._pending_db.remove(_sn)

    def _has_pending(self, _sn: int) -> bool:
        return self._pending_db[_sn] is not None

    def _update_balance_by(self, _user: Address, _tokenName: str, _usable: int, _locked: int):
        """
        update balance as much as delta `_usable` and `_locked`
        """
        balance = self._get_balance(_user, _tokenName)
        usable = balance['usable'] + _usable
        locked = balance['locked'] + _locked
        if usable == 0 and locked == 0:
            self._balance_db[_user].remove(_tokenName)
        else:
            self._balance_db[_user][_tokenName] = \
                _to_bytes([hex(usable), hex(locked)])

    def _get_balance(self, _user: Address, _tokenName: str) -> dict:
        balance = self._balance_db[_user][_tokenName]
        if balance is None:
            return {'usable': 0, 'locked': 0}
        else:
            balance = _to_obj(balance)
            return {
                'usable': int(balance[self.BD_USABLE_IDX], 16),
                'locked': int(balance[self.BD_LOCKED_IDX], 16)
            }
