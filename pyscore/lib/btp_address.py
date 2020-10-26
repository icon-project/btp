from .btp_exception import BTPException


class BTPAddress(object):
    """BTPAddress class
    """
    PROTOCOL_BTP = "btp"

    def __init__(self, protocol: str, net: str, contract: str) -> None:
        if protocol is None:
            protocol = ""
        self._protocol = protocol

        if net is None:
            net = ""
        nid = ""
        chain = net
        idx = net.rfind(".")
        if idx == 0:
            raise BTPException("fail to parse, net_address")
        elif idx > 0:
            nid = net[0:idx]
            chain = net[idx + 1:]
        self._nid = nid
        self._chain = chain

        if contract is None:
            contract = ""
        self._contract = contract

    def __eq__(self, other) -> bool:
        """operator == overriding

        :return: bool
        """
        return \
            isinstance(other, BTPAddress) \
            and str(self) == str(other)

    def __ne__(self, other) -> bool:
        """operator != overriding

        :return: (bool)
        """
        return not self.__eq__(other)

    def __str__(self) -> str:
        """operator str() overriding

        returns
        """
        return f'{self._protocol}://{self.net}/{self._contract}'

    def __bytes__(self) -> bytes:
        return bytes(self.__str__(), 'utf-8')

    def __repr__(self) -> str:
        return self.__str__()

    def __hash__(self) -> int:
        """Returns a hash value for this object

        :return: hash value
        """
        return hash(self.__bytes__())

    @property
    def protocol(self) -> str:
        return self._protocol

    @property
    def nid(self) -> str:
        return self._nid

    @property
    def chain(self) -> str:
        return self._chain

    @property
    def contract(self) -> str:
        return self._contract

    @property
    def net(self) -> str:
        if len(self._nid) == 0:
            return self._chain
        else:
            return f'{self._nid}.{self._chain}'

    def replace_contract(self, contract: str) -> 'BTPAddress':
        return BTPAddress(self.protocol, self.net, contract)

    def is_valid(self) -> bool:
        return not (len(self.protocol) == 0 or len(self.net) == 0 or len(self.contract) == 0)

    @staticmethod
    def from_bytes(serialized: bytes) -> 'BTPAddress':
        return BTPAddress.from_string(serialized.decode('utf-8'))

    @staticmethod
    def from_string(address: str) -> 'BTPAddress':
        if address is None:
            return None
        split = address.split("://")
        protocol = split[0]
        split = split[1].split("/")
        net = split[0]
        contract = ""
        if len(split) > 1:
            contract = split[1]
        return BTPAddress(protocol, net, contract)

    @staticmethod
    def from_string_with_validation(address: str) -> 'BTPAddress':
        v = BTPAddress.from_string(address)
        if v is None or not v.is_valid():
            raise BTPException("invalid BTPAddress")
        return v

    @staticmethod
    def from_net_address(net_address: str) -> 'BTPAddress':
        return BTPAddress(BTPAddress.PROTOCOL_BTP, net_address, "")
