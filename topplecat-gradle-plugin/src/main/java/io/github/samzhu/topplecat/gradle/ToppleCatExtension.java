package io.github.samzhu.topplecat.gradle;

import org.gradle.api.Action;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Nested;

/** Gradle layout convention for the pure Java ToppleCat workflow. */
public abstract class ToppleCatExtension {
  /** Public JSON/YAML cases visible to the implementer. */
  public abstract DirectoryProperty getPublicCaseRoot();

  /** Complete reviewer-only source set moved by {@code toppleCatSeal}. */
  public abstract DirectoryProperty getHiddenSourceRoot();

  /** Invocation-only list supplied through {@code --spec}; there is no persistent Spec DSL. */
  public abstract ListProperty<String> getCommandLineSpecPaths();

  /** True only when the current Gradle invocation explicitly supplied {@code --spec}. */
  public abstract Property<Boolean> getCommandLineSpecProvided();

  /** Invocation-only AC IDs supplied through {@code toppleCatVerify --ac}. */
  public abstract ListProperty<String> getCommandLineAcceptanceConditionIds();

  /** True only when the current Verify invocation explicitly supplied {@code --ac}. */
  public abstract Property<Boolean> getCommandLineAcceptanceConditionsProvided();

  /** Invocation-only Reviewer HTML presentation language; never project verification policy. */
  public abstract Property<String> getCommandLineReportLanguage();

  /** Runtime-only Verify escalation; this never weakens a sealed hidden scope. */
  public abstract Property<Boolean> getAllHiddenRequested();

  @Nested
  public abstract ToppleCatHiddenTestsExtension getHiddenTests();

  @Nested
  public abstract ToppleCatMutationTestingExtension getMutationTesting();

  @Nested
  public abstract ToppleCatPropertyBasedTestingExtension getPropertyBasedTesting();

  @Nested
  public abstract ToppleCatExpectedConsumptionExtension getExpectedConsumption();

  public void hiddenTests(Action<? super ToppleCatHiddenTestsExtension> action) {
    action.execute(getHiddenTests());
  }

  public void mutationTesting(Action<? super ToppleCatMutationTestingExtension> action) {
    action.execute(getMutationTesting());
  }

  public void propertyBasedTesting(Action<? super ToppleCatPropertyBasedTestingExtension> action) {
    action.execute(getPropertyBasedTesting());
  }

  public void expectedConsumption(Action<? super ToppleCatExpectedConsumptionExtension> action) {
    action.execute(getExpectedConsumption());
  }
}
