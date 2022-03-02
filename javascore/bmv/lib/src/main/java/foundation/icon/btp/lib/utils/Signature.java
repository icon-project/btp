package foundation.icon.btp.lib.utils;

import java.util.Arrays;

import score.Context;

public class Signature {
  public static boolean verify(byte[] message, byte[] signature, byte[] publicKey) {
      byte[] recoverPublicKey = Context.recoverKey("ed25519", message, signature, false);
      return Arrays.equals(publicKey, recoverPublicKey);
  }

  public static byte[] recoverKey(byte[] message, byte[] signature) {
    return Context.recoverKey("ed25519", message, signature, false);
}
}