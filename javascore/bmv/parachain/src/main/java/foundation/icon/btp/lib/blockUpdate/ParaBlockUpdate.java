package foundation.icon.btp.lib.parachain.blockupdate;

import foundation.icon.btp.lib.ErrorCode;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;
import foundation.icon.btp.lib.blockheader.BlockHeader;
import foundation.icon.btp.lib.parachain.relaychaindata.RelayChainData;

import score.Context;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.List;

public class ParaBlockUpdate {
    private final BlockHeader blockHeader;
    private final RelayChainData relayChainData;

    public ParaBlockUpdate(byte[] serialized) throws RelayMessageRLPException {
        ObjectReader rlpReader = Context.newByteArrayObjectReader("RLPn", serialized);
        rlpReader.beginList();

        // decode Para block header
        byte[] encodedHeader = rlpReader.readByteArray();
        if (encodedHeader != null && encodedHeader.length > 0) {
            this.blockHeader = new BlockHeader(encodedHeader);
        } else {
            this.blockHeader = null;
        }

        // decode Relay chain data
        byte[] encodedRelayChainData = rlpReader.readNullable(byte[].class);
        if (encodedRelayChainData != null && encodedRelayChainData.length > 0) {
            this.relayChainData = RelayChainData.fromBytes(encodedRelayChainData);
        } else {
            this.relayChainData = null;
        }
        rlpReader.end();
    }

    public BlockHeader getBlockHeader() {
        return this.blockHeader;
    }

    public RelayChainData getRelayChainData() {
        return this.relayChainData;
    }

    public static ParaBlockUpdate fromBytes(byte[] serialized) throws RelayMessageRLPException {
        try {
            return new ParaBlockUpdate(serialized);
        } catch (IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new RelayMessageRLPException("PraBlockUpdate ", e.toString());
        }
    }
}