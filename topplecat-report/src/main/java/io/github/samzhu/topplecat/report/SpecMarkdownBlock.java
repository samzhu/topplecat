package io.github.samzhu.topplecat.report;

import java.util.List;

/** Safe parsed Markdown block retained in selected-document order for reviewer rendering. */
public record SpecMarkdownBlock(
    Kind kind,
    int headingLevel,
    String text,
    List<String> items,
    String language,
    String destination,
    String title,
    List<String> tableHeaders,
    List<List<String>> tableRows,
    String anchorId) {
  public SpecMarkdownBlock {
    if (headingLevel < 0 || headingLevel > 6) {
      throw new IllegalArgumentException("Markdown heading level must be between 0 and 6.");
    }
    text = text == null ? "" : text;
    items = List.copyOf(items == null ? List.of() : items);
    if (kind == null) {
      throw new IllegalArgumentException("Markdown block kind is required.");
    }
    if ((kind == Kind.LIST || kind == Kind.ORDERED_LIST || kind == Kind.TASK_LIST)
        && items.isEmpty()) {
      throw new IllegalArgumentException("A Markdown list block must contain at least one item.");
    }
    language = language == null ? "" : language;
    destination = destination == null ? "" : destination;
    title = title == null ? "" : title;
    tableHeaders = List.copyOf(tableHeaders == null ? List.of() : tableHeaders);
    tableRows =
        List.copyOf(tableRows == null ? List.of() : tableRows.stream().map(List::copyOf).toList());
    anchorId = anchorId == null ? "" : anchorId;
  }

  public SpecMarkdownBlock(Kind kind, int headingLevel, String text, List<String> items) {
    this(kind, headingLevel, text, items, "", "", "", List.of(), List.of(), "");
  }

  public enum Kind {
    HEADING,
    PARAGRAPH,
    LIST,
    ORDERED_LIST,
    TASK_LIST,
    BLOCK_QUOTE,
    HORIZONTAL_RULE,
    CODE_FENCE,
    MERMAID,
    TABLE,
    IMAGE,
    FALLBACK
  }
}
