package foundation.icon.btp.lib.utils;

// import io.emeraldpay.polkaj.tx.Hashing;
import score.Context;

public class Hash {
  public static byte[] getHash(byte[] data) {
      return Context.hash("blake2b-256", data);
  }
}