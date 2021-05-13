package foundation.icon.btp.lib.utils;

import score.Context;

public class Hash {
  public static byte[] getHash(byte[] data) {
      return Context.hash("blake2b-256", data);
  }
}