package foundation.icon.btp.bmv;

import foundation.icon.btp.bmv.lib.Codec;
import foundation.icon.ee.io.DataReader;
import foundation.icon.ee.io.DataWriter;
import foundation.icon.ee.types.Address;
import foundation.icon.ee.util.Crypto;
import org.bouncycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.List;

public class Votes2 {
    private final byte[] bytes;
    private final long round;
    private final PartSetID blockPartSetID;
    private final VoteItem[] voteItems;

    public static class VoteItem {
        private final long timestamp;
        private final byte[] signature;

        public VoteItem(byte[] bytes, Codec c) {
            this(c.newReader(bytes));
        }

        public VoteItem(DataReader r) {
            r.readListHeader();
            timestamp = r.readLong();
            signature = r.readByteArray();
            while (r.hasNext()) {
                r.skip(1);
            }
            r.readFooter();
        }

        public long getTimestamp() {
            return timestamp;
        }

        public byte[] getSignature() {
            return signature;
        }
    }

    public static class PartSetID {
        private final int count;
        private final byte[] hash;

        public PartSetID(DataReader r) {
            r.readListHeader();
            count = r.readInt();
            hash = r.readByteArray();
            while (r.hasNext()) {
                r.skip(1);
            }
            r.readFooter();
        }

        public int getCount() {
            return count;
        }

        public byte[] getHash() {
            return hash;
        }

        public void writeTo(DataWriter w) {
            w.writeListHeader(2);
            w.write(count);
            w.write(hash);
            w.writeFooter();
        }
    }

    public Votes2(byte[] bytes, Codec c) {
        this.bytes = bytes;
        var r = c.newReader(bytes);
        r.readListHeader();

        round = r.readLong();
        blockPartSetID = new PartSetID(r);

        r.readListHeader();
        var items = new ArrayList<Votes2.VoteItem>();
        while (r.hasNext()) {
            var item = new Votes2.VoteItem(r);
            items.add(item);
        }
        r.readFooter();
        voteItems = items.toArray(new Votes2.VoteItem[0]);

        while (r.hasNext()) {
            r.skip(1);
        }
        r.readFooter();
    }

    public VoteItem[] getVoteItems() {
        return voteItems;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public long getRound() {
        return round;
    }

    public static int VOTE_TYPE_PRECOMMIT = 1;

    public boolean verify(long height, byte[] blockId, ValidatorList2 validatorList) {
        List<Address> positiveValidators = new ArrayList<>();

        for (Votes2.VoteItem voteItem : this.voteItems) {
            byte[] encVoteMessage = encodeVoteMessage(voteItem, height, blockId);
            byte[] voteMessageHash = Crypto.sha3_256(encVoteMessage);
            byte[] publicKey = Crypto.recoverKey(voteMessageHash, voteItem.signature, true);
            Address address = new Address(Crypto.getAddressBytesFromKey(publicKey));

            if (validatorList.contains(address) && !positiveValidators.contains(address))
                positiveValidators.add(address);
            else
                throw new IllegalStateException("Duplicate Votes");

            System.out.println(Hex.toHexString(address.toByteArray()));
            System.out.println(address.toString());
            System.out.println("found -> " + validatorList.contains(address));
        }

        double r = (validatorList.getValidators().length * (double) 2 / 3);
        return (positiveValidators.size() <= r);
    }

    private byte[] encodeVoteMessage(Votes2.VoteItem voteItem, long height, byte[] blockId) {
        DataWriter writer = Codec.rlp.newWriter();
        writer.writeListHeader(6);
        writer.write(height);
        writer.write(getRound());
        writer.write(VOTE_TYPE_PRECOMMIT);
        writer.write(blockId);
        blockPartSetID.writeTo(writer);
        writer.write(voteItem.timestamp);
        writer.writeFooter();
        return writer.toByteArray();
    }

}
