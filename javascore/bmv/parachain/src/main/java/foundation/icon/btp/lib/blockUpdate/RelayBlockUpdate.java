package foundation.icon.btp.lib.parachain.blockupdate;

import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;
import foundation.icon.btp.lib.blockheader.BlockHeader;
import foundation.icon.btp.lib.votes.Votes;

import score.Context;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.List;

public class RelayBlockUpdate {
    private final BlockHeader blockHeader;
    private final Votes votes;

    public RelayBlockUpdate(byte[] serialized) throws RelayMessageRLPException {
        ObjectReader rlpReader = Context.newByteArrayObjectReader("RLPn", serialized);
        rlpReader.beginList();

        // decode Para block header
        byte[] encodedHeader = rlpReader.readByteArray();
        if (encodedHeader != null && encodedHeader.length > 0) {
            this.blockHeader = new BlockHeader(encodedHeader);
        } else {
            this.blockHeader = null;
        }

        // decode list of validator's votes
        byte[] encodedVotes = rlpReader.readByteArray();
        if (encodedVotes != null && encodedVotes.length > 0) {
            this.votes = Votes.fromBytes(encodedVotes);
        } else {
            this.votes = null;
        }
        rlpReader.end();
    }

    public BlockHeader getBlockHeader() {
        return this.blockHeader;
    }

    public Votes getVotes() {
        return this.votes;
    }

    public void verify(List<byte[]> validators, BigInteger currentSetId) {
        if (this.votes == null) {
            Context.revert(ErrorCode.INVALID_BLOCK_UPDATE, "not exists votes");
        }

        this.votes.verify(this.blockHeader.getNumber(), this.blockHeader.getHash(), validators, currentSetId);
    }

    public static RelayBlockUpdate fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new RelayBlockUpdate(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("RelayBlockUpdate ", e.toString());
        }
    }
}