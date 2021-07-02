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

from .. import Const, BTPAddress


class IterableDictDB(ABC):

    def __init__(self, key, db: IconScoreDatabase, value_type: type, with_flush: bool = False) -> None:
        self.__base_db = db
        self.__prefix = str(key)
        self.__value_type = value_type

        if not (issubclass(value_type, IterableDictDB) or issubclass(value_type, PropertiesDB)):
            self.__db = DictDB(self._sub_prefix("_dict"), db, value_type)

        # TODO using AutoExpand for __db_keys
        self.__db_keys = VarDB(self._sub_prefix("_keys"), db, value_type=str)
        self.__with_flush = with_flush
        self.__cache = {}  # cache of db
        self.__keys = []
        self.__is_loaded_keys = False
        self.__add = {}
        self.__remove = {}
        self.__update = {}

    @property
    def base_db(self) -> IconScoreDatabase:
        return self.__base_db

    @abstractmethod
    def raw_to_object(self, raw):
        pass

    @abstractmethod
    def object_to_raw(self, obj):
        pass

    @property
    def _base_db(self) -> IconScoreDatabase:
        return self.__base_db

    @property
    def _prefix(self) -> str:
        return self.__prefix

    def _sub_prefix(self, prefix: str) -> str:
        if self.__prefix is None:
            raise Exception("not initialized")
        if prefix is None:
            raise Exception("invalid prefix")
        return '|'.join([self.__prefix, prefix])

    def flush(self):
        if self.__with_flush:
            if issubclass(self.__value_type, IterableDictDB) or issubclass(self.__value_type, PropertiesDB):
                for key in self.__add:
                    v = self.__add[key]
                    v.flush()
                for key in self.__update:
                    v = self.__update[key]
                    v.flush()
                for key in self.__remove:
                    v = self.__remove[key]
                    v.flush()
            else:
                for key in self.__add:
                    self.__db[key] = self.object_to_raw(self.__add[key])
                for key in self.__update:
                    self.__db[key] = self.object_to_raw(self.__update[key])
                for key in self.__remove:
                    self.__db.remove(key)

            if len(self.__add) + len(self.__remove) > 0:
                self._flush_keys()

    def _flush_keys(self) -> None:
        serialized = ",".join(self.__keys)
        self.__db_keys.set(serialized)

    def _load_keys(self) -> list:
        if not self.__is_loaded_keys:
            serialized = self.__db_keys.get()
            if len(serialized) > 1:
                self.__keys = serialized.split(",")
            self.__is_loaded_keys = True
        return self.__keys

    def _add_key(self, key: str) -> None:
        # TODO [TBD] encode with escape for delimiter(',')
        if "," in key:
            raise Exception('key could not contains comma')
        self._load_keys()
        self.__keys.append(key)
        if not self.__with_flush:
            self._flush_keys()

    def _remove_key(self, key: str) -> int:
        self._load_keys()
        index = self._index_key(key)
        self.__keys.pop(index)
        if not self.__with_flush:
            self._flush_keys()
        return index

    def _index_key(self, key: str) -> int:
        self._load_keys()
        for i in range(len(self.__keys)):
            if self.__keys[i] == key:
                return i
        return -1

    def keys(self) -> list:
        self._load_keys()
        return self.__keys

    def at(self, index: int):
        keys = self.keys()
        if len(keys) > index:
            return self._get(keys[index])
        else:
            raise Exception("out of range")

    def _cache(self, key: str):
        if key in self.__cache:
            return self.__cache[key]
        else:
            if self._contains(key):
                if issubclass(self.__value_type, IterableDictDB):
                    raw = self.raw_to_object(key)
                elif issubclass(self.__value_type, PropertiesDB):
                    raw = self.raw_to_object(key)
                else:
                    raw = self.__db[key]
                self.__cache[key] = raw
                return raw
            else:
                return None

    def _set(self, key: str, value) -> None:
        if key in self.__add:
            self.__add[key] = value
        elif key in self.__update:
            self.__update[key] = value
        elif key in self.__remove:
            self.__remove.pop(key)
            self.__update[key] = value
            self._add_key(key)
        else:
            raw = self._cache(key)
            if raw is None:
                self.__add[key] = value
                self._add_key(key)
            else:
                self.__update[key] = value
        if not self.__with_flush:
            if not (issubclass(self.__value_type, IterableDictDB) or issubclass(self.__value_type, PropertiesDB)):
                self.__db[key] = self.object_to_raw(value)

    def _get(self, key: str):
        if key in self.__add:
            return self.__add[key]
        elif key in self.__update:
            return self.__update[key]
        elif key in self.__remove:
            return None
        else:
            raw = self._cache(key)
            if raw is None:
                return None
            else:
                if not (issubclass(self.__value_type, IterableDictDB) or issubclass(self.__value_type, PropertiesDB)):
                    return self.raw_to_object(raw)
                else:
                    return raw

    def remove(self, key) -> None:
        key = str(key)
        if key in self.__add:
            self.__add.pop(key)
            return
        elif key in self.__remove:
            return
        elif key in self.__update:
            self.__update.pop(key)

        v = self._cache(key)
        if v is None:
            return

        self._remove_key(key)
        self.__remove[key] = v
        self.__cache.pop(key)

        if isinstance(v, IterableDictDB) or isinstance(v, PropertiesDB):
            v.remove_all()
        else:
            if not self.__with_flush:
                self.__db.remove(key)

    def remove_all(self):
        for k in self.keys():
            self.remove(k)

    def _contains(self, key: str) -> bool:
        index = self._index_key(key)
        if index < 0:
            return False
        else:
            return True

    def __setitem__(self, key, value) -> None:
        key = str(key)
        if value is None:
            self.remove(key)
        else:
            self._set(key, value)

    def __getitem__(self, key):
        key = str(key)
        return self._get(key)

    def __delitem__(self, key):
        key = str(key)
        return self.remove(key)

    def __contains__(self, key):
        key = str(key)
        return self._contains(key)

    def __iter__(self):
        return self.keys().__iter__()

    def __len__(self):
        return self.keys().__len__()

    def to_dict(self) -> dict:
        d = {}
        for k in self.keys():
            v = self._get(k)
            d[k] = v
        return d

    def __repr__(self):
        return self.dump()

    def dump(self, depth: int = 0):
        t1 = '\t' * (depth * 2)
        s = f'{t1}{self.__prefix}[\n'
        t = '\t' * (depth * 2 + 1)
        for k in self.keys():
            el = self._get(k)
            v = ''
            if isinstance(el, IterableDictDB):
                v += el.dump(depth + 1)
                s += f'{t}[key:{k}, value:\n{v}{t}]\n'
            elif isinstance(el, PropertiesDB):
                v += el.dump(depth + 1)
                s += f'{t}[key:{k}, value:\n{v}{t}]\n'
            else:
                s += f'{t}[key:{k}, value:{v}]\n'
        s += f'{t1}]\n'
        return s


class BytesDictDB(IterableDictDB):
    def __init__(self, key, db: IconScoreDatabase, with_flush: bool = False) -> None:
        super().__init__(key, db, value_type=bytes, with_flush=with_flush)

    def raw_to_object(self, raw):
        return self.bytes_to_object(raw)

    def object_to_raw(self, obj):
        return bytes(obj)

    def bytes_to_object(self, bs: bytes) -> object:
        pass


class StringDictDB(IterableDictDB):
    def __init__(self, key, db: IconScoreDatabase, with_flush: bool = False) -> None:
        super().__init__(key, db, value_type=str, with_flush=with_flush)

    def raw_to_object(self, raw):
        return self.str_to_object(raw)

    def object_to_raw(self, obj):
        return str(obj)

    def str_to_object(self, s: str) -> object:
        pass


class AddressDictDB(IterableDictDB):
    def raw_to_object(self, raw):
        return raw

    def object_to_raw(self, obj):
        return obj

    def __init__(self, key, db: IconScoreDatabase, with_flush: bool = False) -> None:
        super().__init__(key, db, value_type=Address, with_flush=with_flush)

    def __setitem__(self, key, value: Address):
        super().__setitem__(key, value)

    def __getitem__(self, key) -> Address:
        return super().__getitem__(key)


class PropertiesDB(ABC):
    class Type(Const):
        INT = 1
        STR = 2
        BOOL = 3
        BYTES = 4
        ADDRESS = 5
        BTPADDRESS = 6
        SERIALIZABLE = 7

    class TypeException(Exception):
        "Object type not supported for convert."
        pass

    def __new__(cls, *args, **kwargs):
        v = super().__new__(cls)
        v.__default = {}
        v.__db = None
        v.__prefix = None
        return v

    def __init__(self, prefix, db: IconScoreDatabase, with_flush: bool = False) -> None:
        self.__base_db = db
        if prefix is None:
            raise Exception("invalid prefix")
        self.__prefix = prefix
        self.__db = DictDB(prefix, db, value_type=bytes)
        self.__with_flush = with_flush
        self.__cache = {}
        self.__update = {}
        self.__remove = {}

    @property
    def _base_db(self) -> IconScoreDatabase:
        return self.__base_db

    @property
    def _prefix(self) -> str:
        return self.__prefix

    @staticmethod
    def iscontainer(v) -> bool:
        return isinstance(v, PropertiesDB) or isinstance(v, IterableDictDB) or isinstance(v, ArrayDB)

    def flush(self):
        if self.__with_flush:
            for k in self.__update:
                self.__db[k] = self.__update[k]
            for k in self.__remove:
                el = self.__remove[k]
                if isinstance(el, PropertiesDB) or isinstance(el, IterableDictDB):
                    el.flush()
                elif isinstance(el, ArrayDB):
                    for i in range(len(el)):
                        el.pop()
                else:
                    self.__db.remove(k)

    def remove_all(self):
        for k in self.__dict__:
            if not k.startswith('_') and k not in self.__remove:
                el = self.__dict__[k]
                if isinstance(el, PropertiesDB) or isinstance(el, IterableDictDB):
                    el.remove_all()
                elif isinstance(el, ArrayDB):
                    if not self.__with_flush:
                        for i in range(len(el)):
                            el.pop()
                self.__remove[k] = el

        for k in self.__default:
            getattr(self, k)

        for k in self.__cache:
            self.__remove[k] = self.__cache[k]
            if not self.__with_flush:
                self.__db.remove(k)
        self.__cache.clear()

    def _sub_prefix(self, prefix: str) -> str:
        if self.__prefix is None:
            raise Exception("not initialized")
        if prefix is None:
            raise Exception("invalid prefix")
        return '|'.join([self.__prefix, prefix])

    def _array_db(self, name: str, value_type: type) -> ArrayDB:
        return ArrayDB(self._sub_prefix(name), self.__base_db, value_type)

    def _address_dict_db(self, name: str) -> AddressDictDB:
        return AddressDictDB(self._sub_prefix(name), self.__base_db)

    def get(self, k):
        if k in self.__remove:
            return None
        elif k in self.__cache:
            return self.__cache[k]
        else:
            b = self.__db[k]
            if b is None:
                if k in self.__default:
                    v = self.__default[k]
                    if isinstance(v, type) and issubclass(v, Serializable):
                        v = None
                else:
                    v = None
            elif b[0] == PropertiesDB.Type.INT:
                v = int.from_bytes(b[1:], "big", signed=True)
            elif b[0] == PropertiesDB.Type.STR:
                v = b[1:].decode('utf-8')
            elif b[0] == PropertiesDB.Type.BOOL:
                v = True if b[1:] == b'\x01' else False
            elif b[0] == PropertiesDB.Type.BYTES:
                v = b[1:]
            elif b[0] == PropertiesDB.Type.ADDRESS:
                v = Address.from_bytes(b[1:])
            elif b[0] == PropertiesDB.Type.BTPADDRESS:
                v = BTPAddress.from_bytes(b[1:])
            elif b[0] == PropertiesDB.Type.SERIALIZABLE:
                v = self.__default[k]
                if isinstance(v, type):
                    v = v.__new__(v)
                if isinstance(v, Serializable):
                    v = v.from_bytes(b[1:])
                else:
                    raise PropertiesDB.TypeException(
                        f"{type(v)} is not supported")
            else:
                raise PropertiesDB.TypeException(
                    f"{b[0]} is not supported type")
            self.__cache[k] = v
            return v

    def __getattribute__(self, k):
        try:
            attr = super().__getattribute__(k)
            if callable(attr) or k.startswith('_') or PropertiesDB.iscontainer(attr) \
                    or hasattr(self.__class__, k):
                return attr
        except AttributeError:
            pass

        if not (k in self.__slots__ or k in self.__default):
            raise AttributeError(f"{type(self)} has no attribute '{k}'")

        if self.__db is None:
            return self.__default[k]
        else:
            return self.get(k)

    def set(self, k, v):
        if v is None:
            cache = self.__getattribute__(k)
            if cache is None:
                return
            else:
                self.__remove[k] = cache
                self.__cache[k] = None
            if not self.__with_flush:
                self.__db.remove(k)
        else:
            if k in self.__remove:
                self.__remove.pop(k)
            self.__cache[k] = v
            if isinstance(v, int):
                n_bytes = ((v + (v < 0)).bit_length() + 8) // 8
                b = bytes(PropertiesDB.Type.INT) + v.to_bytes(n_bytes, byteorder="big", signed=True)
            elif isinstance(v, str):
                b = bytes(PropertiesDB.Type.STR) + v.encode('utf-8')
            elif isinstance(v, bool):
                b = b'\x01' if v else b'\x00'
                b = bytes(PropertiesDB.Type.BOOL) + b
            elif isinstance(v, bytes):
                b = bytes(PropertiesDB.Type.BYTES) + v
            elif isinstance(v, Address):
                b = bytes(PropertiesDB.Type.ADDRESS) + v.to_bytes()
            elif isinstance(v, BTPAddress):
                b = bytes(PropertiesDB.Type.BTPADDRESS) + bytes(v)
            elif isinstance(v, Serializable):
                b = bytes(PropertiesDB.Type.SERIALIZABLE) + bytes(v)
            else:
                raise PropertiesDB.TypeException(
                    f"{v.__class__.__name__} is not supported type")
            self.__update[k] = b
            if not self.__with_flush:
                self.__db[k] = b

    def __setattr__(self, k, v):
        if k.startswith('_'):
            super().__setattr__(k, v)
        elif PropertiesDB.iscontainer(v):
            super().__setattr__(k, v)
        else:
            try:
                attr = super().__getattribute__(k)
                if attr is not None and PropertiesDB.iscontainer(attr):
                    raise Exception(f"not allowed re-assign container value {k}")
            except AttributeError:
                pass

            if self.__db is None:
                self.__default[k] = v
            else:
                if not (k in self.__slots__ or k in self.__default or hasattr(self.__class__, k)):
                    raise AttributeError(f"{type(self)} has no attribute '{k}'")
                self.set(k, v)

    def to_dict(self) -> dict:
        d = {}
        for k in self.__dict__:
            if not k.startswith('_'):
                el = self.__dict__[k]
                if isinstance(el, PropertiesDB):
                    d[k] = el.to_dict()
                elif isinstance(el, IterableDictDB):
                    d[k] = el.to_dict()
                elif isinstance(el, ArrayDB):
                    l = []
                    for el in el:
                        l.append(el)
                    d[k] = l
        for k in self.__default:
            getattr(self, k)
        for k in self.__cache:
            d[k] = self.__cache[k]
        return d

    def __repr__(self):
        return self.dump()

    def dump(self, depth: int = 0):
        t1 = '\t' * (depth * 2)
        s = f'{t1}{self.__prefix}[\n'
        t = '\t' * (depth * 2 + 1)
        for k in self.__dict__:
            if not k.startswith('_'):
                el = self.__dict__[k]
                v = ''
                if isinstance(el, PropertiesDB):
                    v += el.dump(depth + 1)
                    s += f'{t}[key:{k}, value:\n{v}{t}]\n'
                elif isinstance(el, IterableDictDB):
                    v += el.dump(depth + 1)
                    s += f'{t}[key:{k}, value:\n{v}{t}]\n'
                elif isinstance(el, ArrayDB):
                    v += f'{self.__prefix} ['
                    for el in el:
                        v += f'{el},'
                    v += ']'
                    s += f'{t}[key:{k}, value:{v}{t}]\n'
        for k in self.__default:
            getattr(self, k)
        for k in self.__cache:
            v = self.__cache[k]
            s += f'{t}[key:{k}, value:{v}]\n'
        s += f'{t1}]\n'
        return s


class Serializable(ABC):

    @abstractmethod
    def from_bytes(self, serialized: bytes):
        pass

    @abstractmethod
    def to_bytes(self) -> bytes:
        pass

    def __bytes__(self) -> bytes:
        return self.to_bytes()

    def __repr__(self) -> str:
        return self.to_bytes().hex()


def remove_from_array_db(array_db: ArrayDB, value) -> int:
    last_idx = len(array_db) - 1
    for i in range(last_idx+1):
        if array_db[i] == value:
            last = array_db.pop()
            if i < last_idx:
                array_db[i] = last
            return i
    return -1
