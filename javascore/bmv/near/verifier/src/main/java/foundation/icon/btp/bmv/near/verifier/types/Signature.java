package foundation.icon.btp.bmv.near.verifier.types;

import score.ObjectReader;
import org.near.borshj.Borsh;
import org.near.borshj.BorshBuffer;
import io.ipfs.multibase.Base58;

public class Signature implements Borsh {
    private KeyType keyType;
    private byte[] key;

    protected Signature() {
    }

    public Signature(String signature) {
        String[] data = signature.split(":");
        switch (data[0]) {
            case "ed25519":
                keyType = KeyType.ED25519;
                break;
            case "secp256k1":
                keyType = KeyType.SECP256K1;
                break;
            default:
                throw new IllegalArgumentException("not supported key for signature");
        }
        key = Base58.decode(data[1]);
    }

    public byte[] getKey() {
        return key;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public static Signature readObject(ObjectReader reader) {
        byte[] data = reader.readNullable(byte[].class);
        if (data == null)
            return null;

        Signature signature = new Signature();
        switch (data[0]) {
            case 0:
                signature.keyType = KeyType.ED25519;
                break;
            case 1:
                signature.keyType = KeyType.SECP256K1;
                break;
            default:
                throw new IllegalArgumentException("not supported key for signature");
        }

        signature.key = slice(data, 1, 64);
        return signature;
    }

    private static byte[] slice(byte[] b1, int position, int size) {
        byte[] data = new byte[size];
        System.arraycopy(b1, position, data, 0, size);
        return data;
    }

    @Override
    public void append(BorshBuffer writer) {
        switch (keyType) {
            case ED25519:
                writer.write((byte) (0));
                break;
            case SECP256K1:
                writer.write((byte) (1));
            default:
                break;
        }
        writer.write(key);
    }
}
