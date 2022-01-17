package foundation.icon.btp.bmv.near.verifier.types;

import score.Context;

public class SHA3 implements Hash {

    @Override
    public byte[] computeHash(byte[] data) {
        return Context.hash("sha3-256", data);
    }
    
}
