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

package foundation.icon.btp.nativecoin.irc31;

import com.iconloop.score.token.irc31.IRC31;
import foundation.icon.btp.nativecoin.NCSMessage;
import foundation.icon.btp.nativecoin.TransferRequest;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IRC31Test implements IRC31IntegrationTest {

    static IRC31 irc31 = IRC31IntegrationTest.irc31Supplier;


}
