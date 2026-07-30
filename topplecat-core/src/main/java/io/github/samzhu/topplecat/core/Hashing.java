package io.github.samzhu.topplecat.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 utilities used for escrow and later evidence integrity. */
public final class Hashing {
  private Hashing() {}

  public static String sha256(byte[] bytes) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
      StringBuilder hex = new StringBuilder(digest.length * 2);
      for (byte value : digest) {
        hex.append(String.format("%02x", value));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable", exception);
    }
  }
}
