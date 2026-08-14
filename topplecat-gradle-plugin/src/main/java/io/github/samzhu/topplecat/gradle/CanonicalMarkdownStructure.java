package io.github.samzhu.topplecat.gradle;

import io.github.samzhu.topplecat.report.SpecMarkdownBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.SourceSpan;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.IncludeSourceSpans;
import org.commonmark.parser.Parser;

/**
 * The single structural seam for selected Markdown.
 *
 * <p>One CommonMark parse produces both the declaration/marker events and the safe document
 * projection. Container ancestry is retained in recursive block children, so a list or quote is
 * never reinterpreted as ordinary inline text by the report renderer.
 */
final class CanonicalMarkdownStructure {
  private static final java.util.regex.Pattern ACCEPTANCE_MARKER =
      java.util.regex.Pattern.compile(
          "^<!-- topplecat:acceptance:(AC-[A-Za-z0-9][A-Za-z0-9-]*) -->$");
  private static final java.util.regex.Pattern ACCEPTANCE_DIRECTIVE =
      java.util.regex.Pattern.compile("^<!--\\s*topplecat:acceptance(?:[: ].*)?-->$");
  private static final java.util.regex.Pattern AC_ID_PREFIX =
      java.util.regex.Pattern.compile("AC-[A-Za-z0-9][A-Za-z0-9-]*");
  private static final Parser PARSER =
      Parser.builder()
          .extensions(List.of(TablesExtension.create()))
          .includeSourceSpans(IncludeSourceSpans.BLOCKS_AND_INLINES)
          .build();

  private CanonicalMarkdownStructure() {}

  static Parsed parse(String source) {
    return parse(source, ignored -> new ResolvedImage("", ""));
  }

  /** Parse once and return both structural events and the safe Review projection. */
  static Parsed parse(String source, Function<String, ResolvedImage> imageResolver) {
    Document document = (Document) PARSER.parse(source);
    List<Event> events = new ArrayList<>();
    Set<Integer> documentLevelMarkerLines = new java.util.LinkedHashSet<>();
    for (Node node = document.getFirstChild(); node != null; node = node.getNext()) {
      if (node instanceof Heading heading) {
        SourceSpan span = firstSpan(heading);
        if (span != null) {
          events.add(
              new Event(
                  EventKind.HEADING,
                  headingText(heading),
                  "",
                  span.getLineIndex() + 1,
                  span.getColumnIndex() + 1));
        }
      } else if (node instanceof HtmlBlock htmlBlock) {
        SourceSpan span = firstSpan(htmlBlock);
        if (span == null) {
          continue;
        }
        String literal = htmlBlock.getLiteral();
        String markerId = acceptanceMarkerId(literal);
        if (markerId != null) {
          documentLevelMarkerLines.add(span.getLineIndex());
          events.add(
              new Event(
                  EventKind.MARKER,
                  literal.strip(),
                  markerId,
                  span.getLineIndex() + 1,
                  span.getColumnIndex() + 1));
        } else if (isAcceptanceDirective(literal)) {
          events.add(
              new Event(
                  EventKind.INVALID_MARKER,
                  literal.strip(),
                  candidateAcId(literal),
                  span.getLineIndex() + 1,
                  span.getColumnIndex() + 1));
        }
      }
    }
    return new Parsed(
        List.copyOf(events),
        Set.copyOf(documentLevelMarkerLines),
        projectChildren(source, document, imageResolver, true));
  }

  private static List<SpecMarkdownBlock> projectChildren(
      String source,
      Node parent,
      Function<String, ResolvedImage> imageResolver,
      boolean documentLevel) {
    List<SpecMarkdownBlock> result = new ArrayList<>();
    for (Node node = parent.getFirstChild(); node != null; node = node.getNext()) {
      result.addAll(projectNode(source, node, imageResolver, documentLevel));
    }
    return List.copyOf(result);
  }

  private static List<SpecMarkdownBlock> projectNode(
      String source,
      Node node,
      Function<String, ResolvedImage> imageResolver,
      boolean documentLevel) {
    if (node instanceof Heading heading) {
      return List.of(
          block(
              SpecMarkdownBlock.Kind.HEADING,
              heading.getLevel(),
              inlineSource(source, heading),
              List.of(),
              "",
              List.of()));
    }
    if (node instanceof HtmlBlock htmlBlock) {
      String markerId = acceptanceMarkerId(htmlBlock.getLiteral());
      if (documentLevel && markerId != null) {
        return List.of(
            new SpecMarkdownBlock(
                SpecMarkdownBlock.Kind.ACCEPTANCE_MARKER,
                0,
                "",
                List.of(),
                "",
                "",
                "",
                List.of(),
                List.of(),
                markerId,
                List.of()));
      }
      return List.of(
          block(
              SpecMarkdownBlock.Kind.FALLBACK,
              0,
              htmlBlock.getLiteral(),
              List.of(),
              "html",
              List.of()));
    }
    if (node instanceof FencedCodeBlock code) {
      String language = code.getInfo() == null ? "" : code.getInfo().trim().toLowerCase();
      return List.of(
          block(
              language.equals("mermaid")
                  ? SpecMarkdownBlock.Kind.MERMAID
                  : SpecMarkdownBlock.Kind.CODE_FENCE,
              0,
              code.getLiteral(),
              List.of(),
              language,
              List.of()));
    }
    if (node instanceof IndentedCodeBlock code) {
      return List.of(
          block(SpecMarkdownBlock.Kind.CODE_FENCE, 0, code.getLiteral(), List.of(), "", List.of()));
    }
    if (node instanceof ThematicBreak) {
      return List.of(
          block(SpecMarkdownBlock.Kind.HORIZONTAL_RULE, 0, "", List.of(), "", List.of()));
    }
    if (node instanceof TableBlock table) {
      List<String> headers = new ArrayList<>();
      List<List<String>> rows = new ArrayList<>();
      List<TableRow> tableRows = new ArrayList<>();
      collectTableRows(table, tableRows);
      for (TableRow row : tableRows) {
        List<String> cells = new ArrayList<>();
        for (Node cellNode = row.getFirstChild(); cellNode != null; cellNode = cellNode.getNext()) {
          if (cellNode instanceof TableCell cell) {
            cells.add(inlineSource(source, cell));
          }
        }
        if (headers.isEmpty()) {
          headers.addAll(cells);
        } else {
          rows.add(cells);
        }
      }
      return List.of(
          new SpecMarkdownBlock(
              SpecMarkdownBlock.Kind.TABLE,
              0,
              "",
              List.of(),
              "",
              "",
              "",
              headers,
              rows,
              "",
              List.of()));
    }
    if (node instanceof BlockQuote) {
      return List.of(
          block(
              SpecMarkdownBlock.Kind.BLOCK_QUOTE,
              0,
              "",
              List.of(),
              "",
              projectChildren(source, node, imageResolver, false)));
    }
    if (node instanceof ListBlock list) {
      return List.of(projectList(source, list, imageResolver));
    }
    if (node instanceof Paragraph paragraph) {
      Node first = paragraph.getFirstChild();
      if (first != null && onlyImages(paragraph)) {
        List<SpecMarkdownBlock> images = new ArrayList<>();
        for (Node imageNode = first; imageNode != null; imageNode = imageNode.getNext()) {
          if (!(imageNode instanceof Image image)) {
            continue;
          }
          ResolvedImage resolved = imageResolver.apply(image.getDestination());
          images.add(
              new SpecMarkdownBlock(
                  SpecMarkdownBlock.Kind.IMAGE,
                  0,
                  inlineSource(source, image),
                  List.of(),
                  "",
                  resolved.destination(),
                  image.getTitle() == null || image.getTitle().isBlank()
                      ? resolved.message()
                      : image.getTitle(),
                  List.of(),
                  List.of(),
                  "",
                  List.of()));
        }
        return List.copyOf(images);
      }
      return List.of(
          block(
              SpecMarkdownBlock.Kind.PARAGRAPH,
              0,
              inlineSource(source, paragraph).trim(),
              List.of(),
              "",
              List.of()));
    }
    return List.of(
        block(
            SpecMarkdownBlock.Kind.FALLBACK,
            0,
            sourceSlice(source, node),
            List.of(),
            "markdown",
            List.of()));
  }

  private static SpecMarkdownBlock projectList(
      String source, ListBlock list, Function<String, ResolvedImage> imageResolver) {
    List<SpecMarkdownBlock> itemBlocks = new ArrayList<>();
    boolean allTasks = true;
    for (Node child = list.getFirstChild(); child != null; child = child.getNext()) {
      if (!(child instanceof ListItem item)) {
        continue;
      }
      List<SpecMarkdownBlock> children =
          new ArrayList<>(projectChildren(source, item, imageResolver, false));
      String taskMarker = "";
      if (!children.isEmpty()
          && children.getFirst().kind() == SpecMarkdownBlock.Kind.PARAGRAPH
          && children.getFirst().text().matches("^\\[[ xX]\\]\\s+.*")) {
        String paragraphText = children.getFirst().text();
        taskMarker = paragraphText.substring(0, 3).toLowerCase();
        SpecMarkdownBlock first = children.getFirst();
        children.set(0, copyWithText(first, paragraphText.substring(3).stripLeading()));
      } else {
        allTasks = false;
      }
      itemBlocks.add(
          new SpecMarkdownBlock(
              SpecMarkdownBlock.Kind.LIST_ITEM,
              0,
              taskMarker,
              List.of(),
              "",
              "",
              "",
              List.of(),
              List.of(),
              "",
              children));
    }
    SpecMarkdownBlock.Kind kind =
        list instanceof OrderedList
            ? SpecMarkdownBlock.Kind.ORDERED_LIST
            : allTasks && !itemBlocks.isEmpty()
                ? SpecMarkdownBlock.Kind.TASK_LIST
                : SpecMarkdownBlock.Kind.LIST;
    return block(kind, 0, "", List.of(), "", itemBlocks);
  }

  private static SpecMarkdownBlock copyWithText(SpecMarkdownBlock block, String text) {
    return new SpecMarkdownBlock(
        block.kind(),
        block.headingLevel(),
        text,
        block.items(),
        block.language(),
        block.destination(),
        block.title(),
        block.tableHeaders(),
        block.tableRows(),
        block.anchorId(),
        block.children());
  }

  private static SpecMarkdownBlock block(
      SpecMarkdownBlock.Kind kind,
      int headingLevel,
      String text,
      List<String> items,
      String language,
      List<SpecMarkdownBlock> children) {
    return new SpecMarkdownBlock(
        kind, headingLevel, text, items, language, "", "", List.of(), List.of(), "", children);
  }

  private static String sourceSlice(String source, Node node) {
    if (node.getSourceSpans().isEmpty()) {
      return "";
    }
    SourceSpan first = node.getSourceSpans().getFirst();
    SourceSpan last = node.getSourceSpans().getLast();
    int start = first.getInputIndex();
    int end = last.getInputIndex() + last.getLength();
    return source.substring(Math.max(0, start), Math.min(source.length(), end));
  }

  private static String contentSource(String source, Node node) {
    Node firstChild = node.getFirstChild();
    if (firstChild == null) {
      return sourceSlice(source, node);
    }
    Node lastChild = node.getLastChild();
    SourceSpan first = firstSpan(firstChild);
    SourceSpan last = lastSpan(lastChild);
    if (first == null || last == null) {
      return sourceSlice(source, node);
    }
    int start = first.getInputIndex();
    int end = last.getInputIndex() + last.getLength();
    return source.substring(Math.max(0, start), Math.min(source.length(), end));
  }

  private static void collectTableRows(Node node, List<TableRow> rows) {
    for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
      if (child instanceof TableRow row) {
        rows.add(row);
      } else {
        collectTableRows(child, rows);
      }
    }
  }

  private static boolean onlyImages(Paragraph paragraph) {
    for (Node child = paragraph.getFirstChild(); child != null; child = child.getNext()) {
      if (!(child instanceof Image) && !(child instanceof SoftLineBreak)) {
        return false;
      }
    }
    return true;
  }

  private static String inlineSource(String source, Node node) {
    String value = contentSource(source, node).trim();
    return value.replaceFirst("^#{1,6}[ \\t]+", "").replaceFirst("[ \\t]+#+[ \\t]*$", "").trim();
  }

  private static SourceSpan firstSpan(Node node) {
    return node.getSourceSpans().isEmpty() ? null : node.getSourceSpans().getFirst();
  }

  private static SourceSpan lastSpan(Node node) {
    return node.getSourceSpans().isEmpty() ? null : node.getSourceSpans().getLast();
  }

  private static String headingText(Heading heading) {
    StringBuilder text = new StringBuilder();
    for (Node child = heading.getFirstChild(); child != null; child = child.getNext()) {
      appendPlainText(child, text);
    }
    return text.toString().replaceAll("\\s+", " ").trim();
  }

  private static void appendPlainText(Node node, StringBuilder text) {
    if (node instanceof Text literal) {
      text.append(literal.getLiteral());
      return;
    }
    if (node instanceof Code code) {
      text.append(code.getLiteral());
      return;
    }
    if (node instanceof HtmlInline) {
      return;
    }
    if (node instanceof SoftLineBreak || node instanceof HardLineBreak) {
      text.append(' ');
      return;
    }
    if (node instanceof Link || node instanceof Image || node.getFirstChild() != null) {
      for (Node child = node.getFirstChild(); child != null; child = child.getNext()) {
        appendPlainText(child, text);
      }
    }
  }

  static String acceptanceMarkerId(String literal) {
    if (literal == null) {
      return null;
    }
    var matcher = ACCEPTANCE_MARKER.matcher(literal.strip());
    return matcher.matches() ? matcher.group(1) : null;
  }

  private static boolean isAcceptanceDirective(String literal) {
    return literal != null && ACCEPTANCE_DIRECTIVE.matcher(literal.strip()).matches();
  }

  private static String candidateAcId(String literal) {
    if (literal == null) {
      return "";
    }
    var matcher = AC_ID_PREFIX.matcher(literal);
    return matcher.find() ? matcher.group() : "";
  }

  record ResolvedImage(String destination, String message) {
    ResolvedImage {
      destination = destination == null ? "" : destination;
      message = message == null ? "" : message;
    }
  }

  record Parsed(
      List<Event> events, Set<Integer> documentLevelMarkerLines, List<SpecMarkdownBlock> blocks) {
    Parsed {
      events = List.copyOf(events);
      documentLevelMarkerLines = Set.copyOf(documentLevelMarkerLines);
      blocks = List.copyOf(blocks);
    }
  }

  record Event(EventKind kind, String text, String acId, int line, int column) {
    Event {
      text = text == null ? "" : text;
      acId = acId == null ? "" : acId;
    }
  }

  enum EventKind {
    HEADING,
    MARKER,
    INVALID_MARKER
  }
}
