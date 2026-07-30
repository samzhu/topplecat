package io.github.samzhu.topplecat.core;

/** Signals an invalid ToppleCat contract, custody state, or verification result. */
public final class ToppleCatException extends RuntimeException {
  public ToppleCatException(String message) {
    super(message);
  }

  public ToppleCatException(String message, Throwable cause) {
    super(message, cause);
  }
}
