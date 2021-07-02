/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iconloop.score.util;

import score.Address;
import score.ArrayDB;
import score.Context;
import score.DictDB;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class EnumerableDictDB<K, V> {
    private static final Logger defaultLogger = Logger.getLogger(EnumerableDictDB.class);

    protected final String id;
    private final DictDB<Object, Integer> indexes;
    private final DictDB<Integer, K> keys;
    private final ArrayDB<Object> values;
    private final boolean supportedKeyType;
    private final Logger logger;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public EnumerableDictDB(String id, Class<K> keyClass, Class<? extends V> valueClass, Logger logger) {
        this.id = id;
        supportedKeyType = isSupportedKeyType(keyClass);
        // key => array index
        this.indexes = Context.newDictDB(id, Integer.class);
        // array index => key
        this.keys = Context.newDictDB(concatId("keys"), keyClass);
        // array of valueClass
        this.values = Context.newArrayDB(id, (Class) valueClass);
        this.logger = logger == null ? defaultLogger : logger;
    }

    protected String concatId(Object id) {
        return concatId(this.id, id);
    }

    public int size() {
        return values.size();
    }

    private Object ensusreKeyType(K key) {
        return supportedKeyType ? key : key.toString();
    }

    private Integer getIndex(K key) {
        return indexes.get(ensusreKeyType(key));
    }

    private void setIndex(K key, Integer i) {
        indexes.set(ensusreKeyType(key), i);
    }

    private K getKey(Integer i) {
        return (i != null) ? keys.get(i) : null;
    }

    private void setKey(Integer i, K key) {
        keys.set(i, key);
    }

    @SuppressWarnings("unchecked")
    private V getValue(Integer i) {
        return (i != null) ? (V) values.get(i) : null;
    }

    private V putValue(Integer i, V value) {
        V old = getValue(i);
        if (old == null) {
            values.add(value);
        } else {
            values.set(i, value);
        }
        return old;
    }

    @SuppressWarnings("unchecked")
    private V removeValue(Integer i) {
        V old = getValue(i);
        if (old != null) {
            V last = (V) values.pop();
            if (i != values.size()) {
                values.set(i, last);
            }
        }
        return old;
    }

    public boolean containsKey(K key) {
        return getIndex(key) != null;
    }

    public boolean containsValue(V value) {
        int size = size();
        for (int i = 0; i < size; i++) {
            if (getValue(i).equals(value)) {
                return true;
            }
        }
        return false;
    }

    public V getByIndex(int i) {
        return getValue(i);
    }

    public V get(K key) {
        logger.println("get", key);
        Integer i = getIndex(key);
        V value =  getValue(i);
        logger.println("get returns", value);
        return value;
    }

    public V put(K key, V value) {
        logger.printKeyValue("put", key, value);
        Integer i = getIndex(key);
        V old = putValue(i, value);
        if (old == null) {
            i = values.size() - 1;
            setIndex(key, i);
            setKey(i, key);
        }
        logger.println("put returns", old);
        return old;
    }

    public V remove(K key) {
        logger.println("remove", key);
        Integer i = getIndex(key);
        V old = removeValue(i);
        if (old != null) {
            setIndex(key, null);
            Integer lastIdx = values.size();
            if (i.equals(lastIdx)) {
                //remove lastKey
                setKey(i, null);
            } else {
                //update lastKey
                K lastKey = getKey(lastIdx);
                setIndex(lastKey, i);
                setKey(i, lastKey);
            }
        }
        logger.println("remove returns", old);
        return old;
    }

    public List<K> keySet() {
        ArrayList<K> keySet = new ArrayList<>();
        int size = size();
        for (int i = 0; i < size; i++) {
            keySet.add(getKey(i));
        }
        return keySet;
    }

    public List<Object> supportedKeySet() {
        ArrayList<Object> keySet = new ArrayList<>();
        int size = size();
        for (int i = 0; i < size; i++) {
            keySet.add(ensusreKeyType(getKey(i)));
        }
        return keySet;
    }

    public List<V> values() {
        ArrayList<V> values = new ArrayList<>();
        int size = size();
        for (int i = 0; i < size; i++) {
            values.add(getValue(i));
        }
        return values;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map<K, V> toMap() {
        int size = size();
        Map.Entry[] entries = new Map.Entry[size];
        for (int i = 0; i < size; i++) {
            entries[i] = Map.entry(getKey(i), getValue(i));
        }
        return Map.ofEntries(entries);
    }

    public Map<String, V> toMapWithKeyToString() {
        int size = size();
        Map.Entry[] entries = new Map.Entry[size];
        for (int i = 0; i < size; i++) {
            K key = getKey(i);
            if (key instanceof String){
                entries[i] = Map.entry(key, getValue(i));
            } else {
                entries[i] = Map.entry(key.toString(), getValue(i));
            }
        }
        return Map.ofEntries(entries);
    }

    public static String concatId(String id, Object sub) {
        return id + "|" + sub.toString();
    }

    static boolean isSupportedKeyType(Class<?> clazz) {
        for (Class<?> type : supportedKeyTypes) {
            if (type.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    static Class<?>[] supportedKeyTypes = new Class<?>[]{
            String.class,
            byte[].class,
            Address.class,
            BigInteger.class,
            Byte.class,
            Short.class,
            Integer.class,
            Long.class,
            Character.class
    };
}
