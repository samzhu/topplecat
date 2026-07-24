package io.github.samzhu.topplecat.report;

import java.util.List;

/** Safe, deliberately small Markdown subset sourced from an external spec document. */
public record SpecMarkdownBlock(Kind kind, int headingLevel, String text, List<String> items) {
    public SpecMarkdownBlock {
        if (headingLevel < 0 || headingLevel > 6) {
            throw new IllegalArgumentException("Markdown heading level must be between 0 and 6.");
        }
        text = text == null ? "" : text;
        items = List.copyOf(items == null ? List.of() : items);
        if (kind == Kind.LIST && items.isEmpty()) {
            throw new IllegalArgumentException("A Markdown list block must contain at least one item.");
        }
    }

    public enum Kind {
        HEADING,
        PARAGRAPH,
        LIST
    }
}
