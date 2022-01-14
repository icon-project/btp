package foundation.icon.btp.bsh;

import score.Address;
import score.Context;
import score.annotation.External;

public class HashMock {

    public HashMock() {

    }
    @External(readonly = true)
    public byte[] check(byte[] msg) {
       //return Context.hash("keccak-256", msg);
        byte[] res = Context.hash("keccak-256", msg);
        return res;
    }
}
