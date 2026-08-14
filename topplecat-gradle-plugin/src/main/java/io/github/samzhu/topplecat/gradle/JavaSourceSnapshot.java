package io.github.samzhu.topplecat.gradle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.gradle.api.GradleException;

/** Captures one checked Java method/property without treating braces in literals as syntax. */
final class JavaSourceSnapshot {
  private JavaSourceSnapshot() {}

  static String read(Path root, String fileName, long oneBasedDeclarationLine) {
    try (Stream<Path> sources = Files.walk(root)) {
      Path source =
          sources
              .filter(path -> path.getFileName().toString().equals(fileName))
              .findFirst()
              .orElse(null);
      return source == null ? "" : capture(Files.readAllLines(source), oneBasedDeclarationLine);
    } catch (IOException exception) {
      throw new GradleException(
          "Cannot read checked acceptance source: " + exception.getMessage(), exception);
    }
  }

  static String capture(List<String> lines, long oneBasedDeclarationLine) {
    if (lines.isEmpty()) return "";
    int declaration = (int) Math.max(0L, Math.min(lines.size() - 1L, oneBasedDeclarationLine - 1L));
    int start = declaration;
    while (start > 0 && lines.get(start - 1).stripLeading().startsWith("@")) start--;

    int end = declaration;
    int braces = 0;
    boolean bodyStarted = false;
    JavaLexicalState state = new JavaLexicalState();
    for (int lineIndex = declaration; lineIndex < lines.size(); lineIndex++) {
      String line = lines.get(lineIndex);
      for (int index = 0; index < line.length(); index++) {
        char current = line.charAt(index);
        char next = index + 1 < line.length() ? line.charAt(index + 1) : '\0';
        char afterNext = index + 2 < line.length() ? line.charAt(index + 2) : '\0';
        if (state.consume(current, next, afterNext)) continue;
        if (current == '{') {
          braces++;
          bodyStarted = true;
        } else if (current == '}') {
          braces--;
        }
      }
      state.endLine();
      end = lineIndex;
      if (bodyStarted && braces == 0) break;
    }

    List<String> snippet = lines.subList(start, end + 1);
    int indentation =
        snippet.stream()
            .filter(line -> !line.isBlank())
            .mapToInt(JavaSourceSnapshot::leadingWhitespace)
            .min()
            .orElse(0);
    return snippet.stream()
        .map(line -> line.length() >= indentation ? line.substring(indentation) : line)
        .reduce((left, right) -> left + System.lineSeparator() + right)
        .orElse("");
  }

  private static int leadingWhitespace(String line) {
    int index = 0;
    while (index < line.length() && Character.isWhitespace(line.charAt(index))) index++;
    return index;
  }

  private static final class JavaLexicalState {
    private boolean lineComment;
    private boolean blockComment;
    private boolean string;
    private boolean character;
    private boolean textBlock;
    private boolean escaped;
    private int quoteSkip;

    private boolean consume(char current, char next, char afterNext) {
      if (quoteSkip > 0) {
        quoteSkip--;
        return true;
      }
      if (lineComment) return true;
      if (blockComment) {
        if (current == '*' && next == '/') {
          blockComment = false;
          quoteSkip = 1;
        }
        return true;
      }
      if (textBlock) {
        if (current == '"' && next == '"' && afterNext == '"') {
          textBlock = false;
          quoteSkip = 2;
        }
        return true;
      }
      if (string || character) {
        if (escaped) {
          escaped = false;
        } else if (current == '\\') {
          escaped = true;
        } else if ((string && current == '"') || (character && current == '\'')) {
          string = false;
          character = false;
        }
        return true;
      }
      if (current == '/' && next == '/') {
        lineComment = true;
        quoteSkip = 1;
        return true;
      }
      if (current == '/' && next == '*') {
        blockComment = true;
        quoteSkip = 1;
        return true;
      }
      if (current == '"' && next == '"' && afterNext == '"') {
        textBlock = true;
        quoteSkip = 2;
        return true;
      }
      if (current == '"') {
        string = true;
        return true;
      }
      if (current == '\'') {
        character = true;
        return true;
      }
      return false;
    }

    private void endLine() {
      lineComment = false;
    }
  }
}
