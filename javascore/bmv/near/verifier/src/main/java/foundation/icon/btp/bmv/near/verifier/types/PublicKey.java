package foundation.icon.btp.bmv.near.verifier.types;

import score.ObjectReader;
import org.near.borshj.Borsh;
import org.near.borshj.BorshBuffer;
import io.ipfs.multibase.Base58;

public class PublicKey implements Borsh {
    private KeyType keyType;
    private byte[] key;

    protected PublicKey() {
    }

    public PublicKey(String publicKey) {
        String[] data = publicKey.split(":");
        switch (data[0]) {
            case "ed25519":
                keyType = KeyType.ED25519;
                break;
            case "secp256k1":
                keyType = KeyType.SECP256K1;
                break;
            default:
                throw new IllegalArgumentException("not supported key for PublicKey");
        }
        key = Base58.decode(data[1]);
    }

    public byte[] getKey() {
        return key;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public static PublicKey readObject(ObjectReader reader) {
        byte[] data = reader.readNullable(byte[].class);
        if (data == null)
            return null;

        PublicKey publicKey = new PublicKey();
        switch (data[0]) {
            case 0:
                publicKey.keyType = KeyType.ED25519;
                break;
            case 1:
                publicKey.keyType = KeyType.SECP256K1;
                break;
            default:
                throw new IllegalArgumentException("not supported key for PublicKey");
        }

        publicKey.key = slice(data, 1, 64);
        return publicKey;
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
