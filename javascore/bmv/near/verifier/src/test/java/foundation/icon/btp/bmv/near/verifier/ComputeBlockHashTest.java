/*
 * Copyright 2022 ICONLOOP Inc.
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

package foundation.icon.btp.bmv.near.verifier;

import org.junit.jupiter.api.Test;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import io.ipfs.multibase.Base58;
import foundation.icon.btp.bmv.near.verifier.types.BlockHeader;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import com.adelean.inject.resources.junit.jupiter.GivenTextResource;
import com.adelean.inject.resources.junit.jupiter.TestWithResources;

@TestWithResources
class ComputeBlockHashTest {
    @GivenTextResource("/mock/data/block_header/6gvUukWemPD9tmNoCP4UXoKaxA6uADmv2u9WH6jV7RRD.txt")
    String validBlock;

    @Test
    void validBlockHeaderWithPreviousHashProvided() throws DecoderException {
        byte[] bytes = Hex.decodeHex(validBlock.toCharArray());
        try {
            BlockHeader blockHeader = BlockHeader.fromBytes(bytes);

            assertArrayEquals(Base58.decode("6gvUukWemPD9tmNoCP4UXoKaxA6uADmv2u9WH6jV7RRD"),
                    blockHeader.hash(Base58.decode("67j1sSg3QrhmYGkMKhnvr64Sb5ww2PSTSM5Z99VwLawj")));

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }

    @Test
    void validBlockHeaderWithoutPreviousHashProvided() throws DecoderException {
        byte[] bytes = Hex.decodeHex(validBlock.toCharArray());
        try {
            BlockHeader blockHeader = BlockHeader.fromBytes(bytes);

            assertArrayEquals(Base58.decode("6gvUukWemPD9tmNoCP4UXoKaxA6uADmv2u9WH6jV7RRD"),
                    blockHeader.hash());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
        }
    }
}
