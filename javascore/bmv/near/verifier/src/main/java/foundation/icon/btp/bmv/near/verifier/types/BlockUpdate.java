package foundation.icon.btp.bmv.near.verifier.types;

import score.ObjectReader;
import score.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import foundation.icon.btp.bmv.near.verifier.utils.Hasher;
import foundation.icon.score.util.StringUtil;
import io.ipfs.multibase.Base58;
import foundation.icon.btp.bmv.near.verifier.utils.ArrayZip;

public class BlockUpdate {
    private BlockHeader blockHeader;
    private byte[] nextBlockInnerHash;
    private List<Optional<Signature>> approvalsAfterNext;
    private ApprovalMessage approvalMessage;
    private ApprovalMessage approvalMessageAfterNext;
    private List<BlockProducer> nextBlockProducers;

    public static BlockUpdate readObject(ObjectReader reader) {
        BlockUpdate blockUpdate = new BlockUpdate();
        reader.beginList();
        blockUpdate.blockHeader = BlockHeader.readObject(reader);
        blockUpdate.nextBlockInnerHash = reader.readByteArray();

        reader.beginList();
        blockUpdate.approvalsAfterNext = new ArrayList<>();
        while (reader.hasNext()) {
            Signature approval = Signature.readObject(reader);
            blockUpdate.approvalsAfterNext.add(Optional.ofNullable(approval));
        }
        reader.end();

        blockUpdate.approvalMessage = ApprovalMessage.readObject(reader);
        blockUpdate.approvalMessageAfterNext = ApprovalMessage.readObject(reader);

        reader.beginList();
        blockUpdate.nextBlockProducers = new ItemList<>();
        while (reader.hasNext()) {
            BlockProducer blockProducer = BlockProducer.readObject(reader);
            blockUpdate.nextBlockProducers.add(blockProducer);
        }
        reader.end();

        reader.end();
        return blockUpdate;
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);

        return BlockUpdate.readObject(reader);
    }

    public ApprovalMessage approvalMessageAfterNext() {
        return null;
    }

    private boolean verifyCurrentApprovals(ItemList<MapItem<BlockProducer, Optional<Signature>>> blockProducersMap) {
        int count = 0;
        boolean isValid = true;
        while (count < 4) {
            MapItem<BlockProducer, Optional<Signature>> blockProducerMap = blockProducersMap.getRandom();
            if (blockProducerMap.getValue().isEmpty()) {
                continue;
            }
            if (!approvalMessage.verify(blockProducerMap.getKey().getPublicKey(), blockProducerMap.getValue().get())) {
                isValid = false;
            }
            count++;
        }
        return isValid;
    }

    private boolean validateApprovalMessage(byte[] blockHash) {
        return approvalMessage.getApprovalInner().equals(new ApprovalEndorsement(blockHash));
    }

    private boolean validateApprovalMessage(long blockHeight) {
        return approvalMessage.getApprovalInner().equals(new ApprovalSkip(blockHeight));
    }

    public boolean validateHeader(byte[] blockHash, ItemList<BlockProducer> blockProducers) {
        if (approvalMessage.getTargetHeight() != blockHeader.getHeight() && !validateApprovalMessage(blockHash)
                && !verifyCurrentApprovals((ItemList<MapItem<BlockProducer, Optional<Signature>>>) ArrayZip
                        .zip(blockProducers, approvalsAfterNext)))
            return false;
        return true;
    }

    public boolean validateHeader(byte[] blockHash, long blockHeight, ItemList<BlockProducer> blockProducers) {
        if (approvalMessage.getTargetHeight() != blockHeader.getHeight() && !validateApprovalMessage(blockHeight)
                && !verifyCurrentApprovals((ItemList<MapItem<BlockProducer, Optional<Signature>>>) ArrayZip
                        .zip(blockProducers, approvalsAfterNext)))
            return true;

        return false;
    }
}
