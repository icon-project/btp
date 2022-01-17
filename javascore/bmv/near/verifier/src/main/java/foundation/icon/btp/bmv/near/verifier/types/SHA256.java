package foundation.icon.btp.bmv.near.verifier.types;

import score.Context;

public class SHA256 implements Hash {

    @Override
    public byte[] computeHash(byte[] data) {
        return Context.hash("sha-256", data);
    }
    
}
