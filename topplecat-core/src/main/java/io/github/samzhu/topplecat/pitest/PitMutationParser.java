package io.github.samzhu.topplecat.pitest;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Secure reader for the PIT {@code mutations.xml} subset ToppleCat needs. */
public final class PitMutationParser {
  /** Parses a PIT report from disk. */
  public PitMutationReport parse(Path report) {
    if (report == null || !Files.isRegularFile(report)) {
      throw new ToppleCatException("PIT mutations.xml file is required: " + report);
    }
    try {
      return parse(Files.readString(report));
    } catch (IOException exception) {
      throw new ToppleCatException("Cannot read PIT mutations.xml: " + report, exception);
    }
  }

  /** Parses PIT XML text. */
  public PitMutationReport parse(String xml) {
    if (xml == null || xml.isBlank()) {
      throw new ToppleCatException("PIT mutations.xml content is required.");
    }
    try {
      Document document =
          factory().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
      if (!"mutations".equals(document.getDocumentElement().getTagName())) {
        throw new ToppleCatException("PIT mutations.xml root element must be mutations.");
      }
      NodeList nodes = document.getDocumentElement().getElementsByTagName("mutation");
      List<PitMutation> mutations = new ArrayList<>(nodes.getLength());
      boolean matrix = true;
      for (int index = 0; index < nodes.getLength(); index++) {
        Element mutation = (Element) nodes.item(index);
        String status = requiredAttribute(mutation, "status");
        Element covering = directChild(mutation, "coveringTests");
        Element killing = directChild(mutation, "killingTests");
        Element succeeding = directChild(mutation, "succeedingTests");
        // PIT legitimately omits coveringTests for NO_COVERAGE. The other selector groups are
        // required by fullMutationMatrix even when they contain no selectors.
        matrix &=
            (covering != null || "NO_COVERAGE".equals(status))
                && killing != null
                && succeeding != null;
        mutations.add(
            new PitMutation(
                booleanAttribute(mutation, "detected"),
                status,
                requiredChild(mutation, "mutatedClass"),
                requiredChild(mutation, "mutator"),
                requiredChild(mutation, "description"),
                testNames(covering),
                testNames(killing),
                testNames(succeeding)));
      }
      return new PitMutationReport(mutations, matrix);
    } catch (ParserConfigurationException | SAXException | IOException exception) {
      throw new ToppleCatException("Cannot parse PIT mutations.xml.", exception);
    }
  }

  private static DocumentBuilderFactory factory() throws ParserConfigurationException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    return factory;
  }

  private static boolean booleanAttribute(Element element, String name) {
    String value = requiredAttribute(element, name);
    if (!"true".equals(value) && !"false".equals(value)) {
      throw new ToppleCatException("PIT mutation attribute must be true or false: " + name);
    }
    return Boolean.parseBoolean(value);
  }

  private static String requiredAttribute(Element element, String name) {
    String value = element.getAttribute(name);
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("PIT mutation attribute is required: " + name);
    }
    return value;
  }

  private static String requiredChild(Element element, String name) {
    Element child = directChild(element, name);
    String value = child == null ? null : child.getTextContent();
    if (value == null || value.isBlank()) {
      throw new ToppleCatException("PIT mutation element is required: " + name);
    }
    return value;
  }

  private static List<String> testNames(Element container) {
    if (container == null
        || container.getTextContent() == null
        || container.getTextContent().isBlank()) {
      return List.of();
    }
    return java.util.Arrays.stream(container.getTextContent().split("\\|"))
        .filter(value -> !value.isEmpty())
        .toList();
  }

  private static Element directChild(Element parent, String name) {
    NodeList children = parent.getChildNodes();
    for (int index = 0; index < children.getLength(); index++) {
      Node child = children.item(index);
      if (child instanceof Element element && name.equals(element.getTagName())) {
        return element;
      }
    }
    return null;
  }
}
