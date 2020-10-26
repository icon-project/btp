from iconservice import IconScoreDatabase, DictDB, VarDB, ABC, abstractmethod, Address


class IterableDictDB(ABC):
    def __init__(self, key, db: IconScoreDatabase, value_type: type, with_flush: bool = False) -> None:
        self.__db_key = str(key)
        self.__db = DictDB(self.__db_key, db, value_type)
        # TODO using AutoExpand for __db_keys
        self.__db_keys = VarDB(self.__db_key, db, value_type=str)
        self.__with_flush = with_flush
        self.__cache = {}  # cache of db
        self.__keys = []
        self.__is_loaded_keys = False
        self.__add = {}
        self.__remove = {}
        self.__update = {}

    @abstractmethod
    def raw_to_object(self, raw):
        pass

    @abstractmethod
    def object_to_raw(self, obj):
        pass

    def flush(self):
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
        # TODO encode or validation digit_alpha
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

    def _cache(self, key: str):
        if key in self.__cache:
            return self.__cache[key]
        else:
            if key in self.__db:
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
            self.__db[key] = self.object_to_raw(value)

    def _get(self, key: str):
        if key in self.__add:
            return self.__add[key]
        elif key in self.__update:
            return self.__update[key]
        else:
            raw = self._cache(key)
            if raw is None:
                return None
            else:
                return self.raw_to_object(raw)

    def remove(self, key) -> None:
        key = str(key)
        if key in self.__add:
            self.__add.pop(key)
            return
        elif key in self.__remove:
            return
        elif key in self.__update:
            self.__update.pop(key)
        else:
            bs = self._cache(key)
            if bs is None:
                return

        index = self._remove_key(key)
        self.__remove[key] = index
        if not self.__with_flush:
            self.__db.remove(key)

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


class BytesDictDB(IterableDictDB):
    def __init__(self, key, db: IconScoreDatabase, with_flush: bool = False) -> None:
        super().__init__(key, db, value_type=bytes, with_flush=with_flush)

    def raw_to_object(self, raw):
        return self.bytes_to_object(raw)

    def object_to_raw(self, obj):
        return bytes(obj)

    @abstractmethod
    def bytes_to_object(self, bs: bytes) -> object:
        pass


class StringDictDB(IterableDictDB):
    def __init__(self, key, db: IconScoreDatabase, with_flush: bool = False) -> None:
        super().__init__(key, db, value_type=str, with_flush=with_flush)

    def raw_to_object(self, raw):
        return self.str_to_object(raw)

    def object_to_raw(self, obj):
        return str(obj)

    @abstractmethod
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

