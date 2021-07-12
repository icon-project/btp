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

package com.iconloop.btp.bmc;

import com.iconloop.score.data.EnumerableDictDB;
import com.iconloop.score.util.Logger;
import score.Address;

public class Services extends EnumerableDictDB<String, Address> {
    private static final Logger logger = Logger.getLogger(Services.class);

    public Services(String id) {
        super(id, String.class, Address.class);
    }

}
