package io.github.samzhu.topplecat.report;

import io.github.samzhu.topplecat.core.Hashing;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Writes self-contained offline report bundles from resource-owned HTML, CSS, and JavaScript. */
public final class HtmlBundleWriter {
  private static final String ROOT = "/io/github/samzhu/topplecat/report/bundle/";
  private static final String DATA = "__TOPPLECAT_DATA__";
  private static final String LANGUAGE = "__TOPPLECAT_LANGUAGE__";
  private static final String PRESENTATION = "__TOPPLECAT_PRESENTATION__";

  private HtmlBundleWriter() {}

  public static void review(Path output, ReviewView view) {
    review(output, view, ReportLanguage.EN);
  }

  /** Writes a Spec Review in the selected Reviewer presentation language. */
  public static void review(Path output, ReviewView view, ReportLanguage language) {
    write(output, ReportJson.writeReview(view), language);
  }

  /** Writes a Spec Review and copies only parser-approved repository-local image assets. */
  public static void review(Path output, ReviewView view, Path projectRoot) {
    review(output, view, projectRoot, ReportLanguage.EN);
  }

  /** Writes a Spec Review in the selected language and copies approved local Spec assets. */
  public static void review(
      Path output, ReviewView view, Path projectRoot, ReportLanguage language) {
    try {
      List<ApprovedAsset> assets = approvedReviewAssets(view, projectRoot);
      write(output, ReportJson.writeReview(view), language);
      copyReviewAssets(output, assets);
    } catch (RuntimeException exception) {
      clearGeneratedBundle(output);
      throw exception;
    }
  }

  public static void verification(Path output, VerificationView view) {
    verification(output, view, ReportLanguage.EN);
  }

  /** Writes a Verification Report in the selected Reviewer presentation language. */
  public static void verification(Path output, VerificationView view, ReportLanguage language) {
    write(output, ReportJson.writeVerification(view), language);
  }

  private static void write(Path output, String json, ReportLanguage language) {
    try {
      Files.createDirectories(output.resolve("assets"));
      String shell =
          resource("index.html")
              .replace(DATA, safeScriptJson(json))
              .replace(LANGUAGE, language.tag())
              .replace(PRESENTATION, "{\"language\":\"" + language.tag() + "\"}");
      Files.writeString(output.resolve("index.html"), shell, StandardCharsets.UTF_8);
      Files.writeString(output.resolve("data.json"), json, StandardCharsets.UTF_8);
      copy("assets/report.css", output.resolve("assets/report.css"));
      copy("assets/mermaid.js", output.resolve("assets/mermaid.js"));
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

  private static List<ApprovedAsset> approvedReviewAssets(ReviewView view, Path projectRoot) {
    List<ApprovedAsset> approved = new ArrayList<>();
    Path root = projectRoot.toAbsolutePath().normalize();
    try {
      Path realRoot = root.toRealPath();
      for (ReviewDocument document : view.selectedSpecDocuments()) {
        for (ReviewDocumentAsset asset : document.assets()) {
          Path source = root.resolve(asset.sourcePath()).normalize();
          if (!source.startsWith(root)
              || !Files.isRegularFile(source)
              || !source.toRealPath().startsWith(realRoot)
              || containsSymbolicComponent(root, source)) {
            throw new IOException("Review document asset escaped its approved bundle boundary.");
          }
          byte[] bytes = Files.readAllBytes(source);
          String expectedDigest =
              asset
                  .bundlePath()
                  .substring("assets/spec/".length(), asset.bundlePath().lastIndexOf('.'));
          if (!Hashing.sha256(bytes).equals(expectedDigest)) {
            throw new IOException(
                "Selected Spec asset changed after Check ("
                    + asset.sourcePath()
                    + "). Rerun Check and then Review to refresh the checked projection.");
          }
          approved.add(new ApprovedAsset(asset, bytes));
        }
      }
      return List.copyOf(approved);
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot approve safe selected-Spec assets for the ToppleCat review bundle: "
              + exception.getMessage(),
          exception);
    }
  }

  private static void copyReviewAssets(Path output, List<ApprovedAsset> assets) {
    try {
      for (ApprovedAsset approved : assets) {
        Path target = output.resolve(approved.asset().bundlePath()).normalize();
        if (!target.startsWith(output.normalize())) {
          throw new IOException("Review document asset escaped its approved bundle boundary.");
        }
        Files.createDirectories(target.getParent());
        Files.write(target, approved.bytes());
      }
    } catch (IOException exception) {
      throw new IllegalStateException(
          "Cannot copy safe selected-Spec assets into the ToppleCat review bundle: "
              + exception.getMessage(),
          exception);
    }
  }

  private static boolean containsSymbolicComponent(Path root, Path candidate) {
    Path current = root;
    for (Path component : root.relativize(candidate.toAbsolutePath().normalize())) {
      current = current.resolve(component);
      if (Files.isSymbolicLink(current)) return true;
    }
    return false;
  }

  private static void clearGeneratedBundle(Path output) {
    if (!Files.exists(output)) return;
    try (var paths = Files.walk(output)) {
      paths
          .sorted(java.util.Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
              });
    } catch (IOException ignored) {
      // Preserve the original asset-validation failure; stale output is best-effort cleanup.
    }
  }

  private record ApprovedAsset(ReviewDocumentAsset asset, byte[] bytes) {}

  private static String safeScriptJson(String json) {
    return json.replace("<", "\\u003c")
        .replace(">", "\\u003e")
        .replace("&", "\\u0026")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029");
  }
}
