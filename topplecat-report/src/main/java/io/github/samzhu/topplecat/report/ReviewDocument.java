package io.github.samzhu.topplecat.report;

import java.util.List;

/** Complete, reviewer-only projection of one command-selected Markdown specification. */
public record ReviewDocument(
    String path, String sha256, List<SpecMarkdownBlock> blocks, List<ReviewDocumentAsset> assets) {
  public ReviewDocument {
    if (path == null || path.isBlank() || sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
      throw new IllegalArgumentException("Selected Spec document identity is invalid.");
    }
    blocks = List.copyOf(blocks == null ? List.of() : blocks);
    assets = List.copyOf(assets == null ? List.of() : assets);
  }
}
