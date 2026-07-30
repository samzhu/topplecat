package io.github.samzhu.topplecat.core;

/** Stable diagnostic reference captured while javac still owns the source tree. */
public record SourceRef(String file, long line, long column) {
  public SourceRef {
    if (file == null || file.isBlank() || line < 1 || column < 1) {
      throw new ToppleCatException("A source reference requires file, line, and column.");
    }
  }
}
