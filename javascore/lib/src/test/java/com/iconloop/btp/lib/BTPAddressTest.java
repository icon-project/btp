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

package com.iconloop.btp.lib;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BTPAddressTest {

    @ParameterizedTest
    @MethodSource("parseParameters")
    void parse(String str, String repr, boolean valid) {
        BTPAddress address = BTPAddress.parse(str);
        System.out.println(
                "actual:" + String.format("[repr:%s, valid:%s]", address.toString(), address.isValid()) +
                        ", expected:" + String.format("[repr:%s, valid:%s]", repr, valid));
        Assertions.assertEquals(repr, address.toString());
        Assertions.assertEquals(valid, address.isValid());
    }

    private static Stream<Arguments> parseParameters() {
        return Stream.of(
                Arguments.of(
                        "btp://0xb34eca.icon/cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        "btp://0xb34eca.icon/cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        true),
                Arguments.of(
                        "0xb34eca.icon/cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        "://0xb34eca.icon/cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        false),
                Arguments.of(
                        "btp:///cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        "btp:///cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        false),
                Arguments.of(
                        "cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        ":///cx69d93f6fe1fe6e4b150fe80004d2d2ce7fc36173",
                        false)
                );
    }
}