package foundation.icon.btp.lib.event;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import foundation.icon.btp.lib.scale.ScaleReader;
import foundation.icon.btp.lib.utils.ByteSliceInput;
import foundation.icon.btp.lib.utils.Hash;
import foundation.icon.btp.lib.utils.HexConverter;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

public class NewAuthoritiesEvent {
    private final List<byte[]> validators = new ArrayList<byte[]>(100);

    public NewAuthoritiesEvent(byte[] eventData) {
        ByteSliceInput input = new ByteSliceInput(eventData);
        int validatorSize = ScaleReader.readUintCompactSize(input);
        for (int i = 0; i < validatorSize; i++) {
            validators.add(input.take(32));
            input.take(8); // AuthorityWeight u64
        }
    }

    public List<byte[]> getValidators() {
        return validators;
    }
}
