package io.github.samzhu.topplecat.core;

/** One exact-byte public contract input approved by a reviewer. */
public record PublicContractEntry(String path, String sha256) implements Comparable<PublicContractEntry> {
    public PublicContractEntry {
        if (!validRelativePath(path) || !digest(sha256)) {
            throw new ToppleCatException("Public contract approval entry is invalid.");
        }
    }

    @Override
    public int compareTo(PublicContractEntry other) {
        return path.compareTo(other.path);
    }

    private static boolean validRelativePath(String value) {
        if (value == null || value.isBlank() || value.startsWith("/") || value.contains("\\")) {
            return false;
        }
        for (String segment : value.split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                return false;
            }
        }
        return true;
    }

    private static boolean digest(String value) {
        return value != null && value.matches("[0-9a-f]{64}");
    }
}
