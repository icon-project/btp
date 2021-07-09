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
package foundation.icon.btp.bmv;

import foundation.icon.btp.bmv.types.BTPAddress;
import foundation.icon.ee.types.Address;
import org.bouncycastle.util.encoders.Hex;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VerifyTests {

    public void testBTPAddress() {
        BTPAddress bmcAddress = BTPAddress.fromString("btp://0x1.iconee/cxa18bf891d029d836ace51cbe41aaad2fdd5f9c65");
        assertEquals("btp", bmcAddress.getProtocol());
        assertEquals("0x1.iconee", bmcAddress.getNet());
        assertEquals("cxa18bf891d029d836ace51cbe41aaad2fdd5f9c65", bmcAddress.getContract());
    }

    public void testValidators() {
        var bytes = Hex.decode("f85494a93af7e0abfdcd9b962d17af65e47d0d7607460e944d3edde4e6b5863f5a220c56f843268f5b6afea1948c7799051f5f4c98936feb4aba83f633def77bf694a46817c03260f6d625984a3ec5fec8e03f6a5f0e");
        ValidatorList2 validators = ClassDecoderUtil.decodeValidatorList(bytes);

        String[] expected = new String[]{
                "00a93af7e0abfdcd9b962d17af65e47d0d7607460e",
                "004d3edde4e6b5863f5a220c56f843268f5b6afea1",
                "008c7799051f5f4c98936feb4aba83f633def77bf6",
                "00a46817c03260f6d625984a3ec5fec8e03f6a5f0e"
        };

        for (int i = 0; i < expected.length; i++) {
            Address addr = new Address(formatAddress(Hex.decode(expected[i])));
            //System.out.println(validators.contains(addr));
            System.out.println(addr.toString() + " " + validators.getValidators()[i]);
        }
    }

    public static byte[] formatAddress(byte[] addr) {
        if (addr.length == 21)
            return addr;
        var ba2 = new byte[addr.length + 1];
        System.arraycopy(addr, 0, ba2, 1, addr.length);
        ba2[0] = 1;
        return ba2;
    }
/*

    @Test
    public void testVotes() {
        byte[] bytes = Hex.decode("ef800100808080a068fbbf5672601c394c5dc9738ea2dedb10b315a51084a03c36897392d5c565a980808084c3808080");
        //System.out.println(Hex.toHexString(Crypto.sha3_256(bytes)));
        BlockHeader blockHeader = ClassDecoderUtil.decodeBlockHeader(bytes, Codec.rlp);

        bytes = Hex.decode("f85494a93af7e0abfdcd9b962d17af65e47d0d7607460e944d3edde4e6b5863f5a220c56f843268f5b6afea1948c7799051f5f4c98936feb4aba83f633def77bf694a46817c03260f6d625984a3ec5fec8e03f6a5f0e");
        //ValidatorList2 validators = ClassDecoderUtil.decodeValidatorList(bytes);
        ValidatorList2 validators = new ValidatorList2(new Address[]
                {new Address(Hex.decode("00f8ad56d48d1be61edfb042d41bbaf865b5db9f9f"))});

        bytes = Hex
                .decode("f87300e201a0599dc0c3c2f91f4d00f1121d32cda5b455de1122e8c541b67fb7b6cee1eda151f84df84b870598c2d9aaf5deb8419c2f8f07486a56f9bb41aef291d0d8f3de3a4e3a7efff4d58a425541c1c6b20274b2ff31f87faed4acb42fb2cf1e5dedf6c76d1ff63fa6d4532654f2bdf434d201");
        Votes2 votes = new Votes2(bytes, Codec.rlp);
        System.out.println(Hex.toHexString(blockHeader.getHash()));
        bytes = Hex.decode("ef4c7667003275cc1a2c40b043032d0a6b8d71c6d44f86f28c4536bbfd395e85");
       // assertEquals(false, votes.verify(blockHeader.getHeight(), bytes, validators));

        for (int i = 0; i < validators.getValidators().length; i++) {
            System.out.println(validators.getValidators()[i].toString());
        }

    }
*/

}
