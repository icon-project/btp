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

import foundation.icon.btp.bmv.lib.Codec;
import foundation.icon.btp.bmv.types.*;
import foundation.icon.ee.io.DataReader;
import foundation.icon.ee.io.DataWriter;
import foundation.icon.ee.types.Address;
import foundation.icon.ee.util.Crypto;
import scorex.util.ArrayList;

import java.util.List;

public class ClassDecoderUtil {

    public static RelayMessage decodeMessageRelay(byte[] bytes) {
        DataReader reader = Codec.rlp.newReader(bytes);
        ArrayList<BlockUpdate> updates = new ArrayList<>();
        ArrayList<ReceiptProof> receipts = new ArrayList<>();
        BlockProof blockProof = null;
        reader.readListHeader();

        reader.readListHeader();

        while (reader.hasNext())
            updates.add(decodeBlockUpdate(reader.readByteArray()));

        reader.readFooter();

        if (reader.hasNext() && !reader.readNullity())
            blockProof = BlockProof.fromBytes(reader.readByteArray());

        if (reader.hasNext() && !reader.readNullity())
            reader.readListHeader();

        while (reader.hasNext()) {
            receipts.add(decodeReceiptProof(reader.readByteArray()));
        }

        return new RelayMessage(blockProof, updates, receipts);
    }

    public static BlockUpdate decodeBlockUpdate(byte[] serialized) {
        DataReader reader = Codec.rlp.newReader(serialized);
        reader.readListHeader();

        BlockHeader blockHeader = decodeBlockHeader(reader.readByteArray(), Codec.rlp);
        Votes2 votes = new Votes2(reader.readByteArray(), Codec.rlp);

        byte[][] nextValidators = null;

        if (reader.hasNext() && !reader.readNullity())
            nextValidators = decodeValidatorList(reader.readByteArray(), Codec.rlp);

        reader.readFooter();
        return new BlockUpdate(blockHeader, null, nextValidators);
    }

    public static BlockHeader decodeBlockHeader(byte[] bytes, Codec codec) {
        byte[] hash = Crypto.sha3_256(bytes);
        DataReader r = codec.newReader(bytes);
        r.readListHeader();

        int version = r.readInt();
        long height = r.readLong();
        long timestamp = r.readLong();
        byte[] proposer = r.readByteArray();
        byte[] prevID = null;
        byte[] votesHash = null;

        if (!r.readNullity())
            prevID = r.readByteArray();

        if (!r.readNullity())
            votesHash = r.readByteArray();

        byte[] nextValidatorHash = r.readByteArray();
        r.skip(3); // PatchTransactionsHash, NormalTransactionHash, LogBloom

        var rr = codec.newReader(r.readByteArray());
        rr.readListHeader();
        rr.skip(2); // StateHash, PatchReceiptsHash

        byte[] normalReceiptHash = null;

        if (!rr.readNullity())
            normalReceiptHash = rr.readByteArray();

        while (rr.hasNext()) {
            rr.skip(1);
        }
        rr.readFooter();
        // clean-up remains
        while (r.hasNext()) {
            r.skip(1);
        }
        r.readFooter();
       /* return new BlockHeader(version,
                    height,
                    proposer,
                    prevID,
                    timestamp,
                    votesHash,
                    nextValidatorHash,
                    normalReceiptHash,
                    hash);*/
        return BlockHeader.fromBytes(hash);

    }

    public static BlockProof decodeBlockProof(byte[] serialized) {
        DataReader reader = Codec.rlp.newReader(serialized);
        reader.readListHeader();

        return new BlockProof(
                ClassDecoderUtil.decodeBlockHeader(reader.readByteArray(), Codec.rlp),
                BlockWitness.fromBytes(reader.readByteArray()));

    }

    public static byte[][] decodeValidatorList(byte[] bytes, Codec c) {
        var r = c.newReader(bytes);
        r.readListHeader();
        List<byte[]> vl = new java.util.ArrayList<byte[]>();
        while (r.hasNext()) {
            vl.add(r.readByteArray());
        }
        r.readFooter();
        return vl.toArray(new byte[][]{});
    }

    public static ValidatorList2 decodeValidatorList(byte[] bytes) {
        var r = Codec.rlp.newReader(bytes);
        r.readListHeader();
        List<Address> vl = new java.util.ArrayList<Address>();
        while (r.hasNext()) {
            vl.add(new Address(formatAddress(r.readByteArray())));
        }
        r.readFooter();

        Address[] vlp = new Address[vl.size()];
        for (int i = 0; i < vlp.length; i++) {
            vlp[i] = vl.get(i);
        }

        ValidatorList2 validatorList = new ValidatorList2(vlp);
        return validatorList;
    }

    public static byte[] formatAddress(byte[] addr) {
        if (addr.length == 21)
            return addr;
        var ba2 = new byte[addr.length + 1];
        System.arraycopy(addr, 0, ba2, 1, addr.length);
        ba2[0] = 1;
        return ba2;
    }

    public static ReceiptProof decodeReceiptProof(byte[] serialized) {
        DataReader reader = Codec.rlp.newReader(serialized);
        reader.readListHeader();

        int index = reader.readInt();
        DataWriter w = Codec.rlp.newWriter();
        w.write(index);
        byte[] mptKey = w.toByteArray();

        List<byte[]> mptProofs = readMPTProofs(reader.readByteArray());

        List<EventProof> eventProofs = new java.util.ArrayList<EventProof>();

        reader.readListHeader();
        reader.readListHeader();
        while (reader.hasNext())
            eventProofs.add(decodeEventProof(reader.readByteArray()));

        return new ReceiptProof(index, mptKey, mptProofs, eventProofs, new java.util.ArrayList<>());
    }

    public static EventProof decodeEventProof(byte[] serialized) {
        DataReader reader = Codec.rlp.newReader(serialized);
        reader.readListHeader();
        if (!reader.hasNext())
            return new EventProof(0, new byte[]{0});
        int index = reader.readInt();
        byte[] proof = reader.readByteArray();
        return new EventProof(index, proof);
    }

    private static List<byte[]> readMPTProofs(byte[] serialized) {
        List<byte[]> mptProofs = new java.util.ArrayList<>();
        DataReader reader = Codec.rlp.newReader(serialized);
        reader.readListHeader();
        while (reader.hasNext()) {
            mptProofs.add(reader.readByteArray());
        }
        return mptProofs;
    }
}
