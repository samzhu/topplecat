package io.github.samzhu.topplecat.core;

/** A repository-relative Markdown document selected for one executable delivery scope. */
public record SelectedSpecDocument(String path, String sha256) implements Comparable<SelectedSpecDocument> {
    public SelectedSpecDocument {
        if (path == null || path.isBlank() || path.startsWith("/") || path.contains("\\")
                || path.equals("..") || path.startsWith("../") || path.contains("/../")
                || sha256 == null || !sha256.matches("[0-9a-f]{64}")) {
            throw new ToppleCatException("Selected Spec document is invalid.");
        }
    }

    @Override
    public int compareTo(SelectedSpecDocument other) {
        return path.compareTo(other.path);
    }
}
