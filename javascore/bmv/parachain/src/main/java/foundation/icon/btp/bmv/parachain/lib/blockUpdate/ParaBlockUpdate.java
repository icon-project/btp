package foundation.icon.btp.bmv.parachain.lib.blockUpdate;

import foundation.icon.btp.lib.blockHeader.BlockHeader;
import foundation.icon.btp.bmv.parachain.lib.relayChainData.RelayChainData;
import foundation.icon.btp.lib.exception.RelayMessageRLPException;
import score.Context;
import score.ObjectReader;

public class ParaBlockUpdate {
    private final BlockHeader blockHeader;
    private final RelayChainData relayChainData;

    public ParaBlockUpdate(byte[] serialized) throws RelayMessageRLPException {
        ObjectReader rlpReader = Context.newByteArrayObjectReader("RLPn", serialized);
        rlpReader.beginList();

        // decode Para block header
        byte[] encodedHeader = rlpReader.readNullable(byte[].class);
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