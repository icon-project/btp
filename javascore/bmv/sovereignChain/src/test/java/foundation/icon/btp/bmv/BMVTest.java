/*
 * Copyright 2020 ICONLOOP Inc.
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

package com.iconloop.score.bmv;

import foundation.icon.btp.lib.BMVStatus;

import com.iconloop.testsvc.Account;
import com.iconloop.testsvc.Score;
import com.iconloop.testsvc.ServiceManager;
import com.iconloop.testsvc.TestBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import score.Context;
import score.Address;

import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BMVTest extends TestBase {
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();

    // TODO deploy contract bmc
    private static final Account bmc = sm.createAccount();
    private static final String network = "sovereignchain";
    // TODO create list edgeware validator address
    private static final String validators = "hxc00a6d2d1e9ee0686704e0b6eec75d0f2c095b39,hx3e9be7c57c769adb06dd0b4943aab9222c30d825";
    private static Score bmvScore;

    // @BeforeAll
    // public static void setup() throws Exception {
    //     bmvScore = sm.deploy(owner, BMV.class, bmc.getAddress(), network, validators, 10, 10, 10, true);
    // }

    // @Test
    // void mta() {
    //     // TODO implement this
    //     assertEquals("pending", bmvScore.call("mta"));
    // }

    // @Test
    // void getBmcAddress() {
    //     assertEquals(bmc.getAddress(), bmvScore.call("bmc"));
    // }

    // @Test
    // void getNetAddress() {
    //     assertEquals(network, bmvScore.call("netAddress"));
    // }

    // @Test
    // void getListValidators() {
    //     assertEquals(validators, bmvScore.call("validators"));
    // }

    // // TODO test getStatus readonly
    // @Test
    // void getStatus() {
    //     long a = 0;
    //     BMVStatus status = (BMVStatus) bmvScore.call("getStatus");
    //     // assertEquals(status.height, 11);
    // }
}
