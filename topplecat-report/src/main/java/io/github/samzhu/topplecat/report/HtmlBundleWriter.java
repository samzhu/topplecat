package io.github.samzhu.topplecat.report;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Writes self-contained offline report bundles from resource-owned HTML, CSS, and JavaScript. */
public final class HtmlBundleWriter {
  private static final String ROOT = "/io/github/samzhu/topplecat/report/bundle/";
  private static final String DATA = "__TOPPLECAT_DATA__";

  private HtmlBundleWriter() {}

  public static void spec(Path output, SpecView view) {
    write(output, ReportJson.writeSpec(view));
  }

  public static void review(Path output, ReviewView view) {
    write(output, ReportJson.writeReview(view));
  }

  public static void verification(Path output, VerificationView view) {
    write(output, ReportJson.writeVerification(view));
  }

  private static void write(Path output, String json) {
    try {
      Files.createDirectories(output.resolve("assets"));
      String shell = resource("index.html").replace(DATA, safeScriptJson(json));
      Files.writeString(output.resolve("index.html"), shell, StandardCharsets.UTF_8);
      Files.writeString(output.resolve("data.json"), json, StandardCharsets.UTF_8);
      copy("assets/report.css", output.resolve("assets/report.css"));
      copy("assets/report.js", output.resolve("assets/report.js"));
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot write ToppleCat offline HTML bundle " + output + ": " + exception.getMessage(),
          exception);
    }
  }

  private static String resource(String name) throws IOException {
    try (InputStream input = HtmlBundleWriter.class.getResourceAsStream(ROOT + name)) {
      if (input == null) {
        throw new IOException("Missing bundled report resource " + name);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static void copy(String name, Path output) throws IOException {
    try (InputStream input = HtmlBundleWriter.class.getResourceAsStream(ROOT + name)) {
      if (input == null) {
        throw new IOException("Missing bundled report resource " + name);
      }
      Files.copy(input, output, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  private static String safeScriptJson(String json) {
    return json.replace("<", "\\u003c")
        .replace(">", "\\u003e")
        .replace("&", "\\u0026")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029");
  }
}
