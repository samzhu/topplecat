package io.github.samzhu.topplecat.gradle;

import java.util.List;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.options.Option;

/** Shared command-line Spec option for public delivery-facing ToppleCat tasks. */
public abstract class ToppleCatScopedTask extends DefaultTask {
  @Input
  public abstract ListProperty<String> getSelectedSpecPaths();

  @Input
  public abstract Property<Boolean> getSpecOptionProvided();

  @Option(
      option = "spec",
      description = "Repository-relative Markdown Spec to select; may be supplied more than once.")
  public void setSpecs(List<String> specPaths) {
    ToppleCatExtension extension = getProject().getExtensions().getByType(ToppleCatExtension.class);
    extension.getCommandLineSpecPaths().set(specPaths == null ? List.of() : List.copyOf(specPaths));
    extension.getCommandLineSpecProvided().set(true);
  }
}
