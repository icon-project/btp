package foundation.icon.btp.lib.event;

import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;

import scorex.util.ArrayList;
import java.util.List;

public class EVMLogEvent {
    protected byte[] address;
    protected List<byte[]> evmTopics = new ArrayList<byte[]>(3);
    protected byte[] evmData;

    public EVMLogEvent(byte[] eventData) {
        ByteSliceInput input = new ByteSliceInput(eventData);
        this.address = input.take(20); // 20 bytes address of contract
        int topicSize = ScaleReader.readUintCompactSize(input); // u32 compact number of item in list
        for (int i = 0; i < topicSize; i++) {
            this.evmTopics.add(input.take(32)); // 32 bytes of topic;
        }

        int evmDataSize = ScaleReader.readUintCompactSize(input); // u32 compact number of bytes of evm data
        this.evmData = input.take(evmDataSize);
    }

    public byte[] getAddress() {
        return this.address;
    }
    
    public List<byte[]> getEvmTopics() {
        return this.evmTopics;
    }

    public byte[] getEvmEventData() {
        return this.evmData;
    }
}
