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

class ConstMeta(type):
    def __new__(metacls, cls, bases, clsdict):
        clsconst = super().__new__(metacls, cls, bases, clsdict)
        clsconst.__slots__ = ['_Const__name']
        clsconst._Const__names = _names = {}
        clsconst._Const__values = _values = {}
        for base in reversed(bases):
            if hasattr(base, '_Const__names'):
                _names.update(base._Const__names)
            if hasattr(base, '_Const__values'):
                _values.update(base._Const__values)
        for name in clsdict.keys():
            value = clsdict[name]
            if isinstance(value, int):
                member = int.__new__(clsconst, value)
                member._Const__name = name
                setattr(clsconst, name, member)
                _names[name] = member
                _values[value] = member
        return clsconst

    def __setattr__(cls, name, value):
        names = cls.__dict__.get('_Const__names', {})
        if name in names:
            raise Exception("cannot set member")
        super().__setattr__(name, value)

    def __delattr__(cls, attr):
        raise Exception("cannot delete member")


class Const(int, metaclass=ConstMeta):
    def __new__(cls, value):
        if isinstance(value, str):
            return getattr(cls, value)
        elif isinstance(value, int):
            return cls.__values[value]

    def __str__(self):
        return self.__name

    def __repr__(self):
        return "%s.%s" % (type(self).__name__, self.__name)

    def __bytes__(self):
        return int(self).to_bytes(1, byteorder="big")

    @property
    def name(self):
        return self.__name

    @property
    def names(self):
        return self.__names

