package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ToppleCatException;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.ParameterDeclarations;

/** JUnit arguments provider used by {@link ToppleAcceptanceTest}. */
public final class ToppleCaseSourceProvider implements ArgumentsProvider {
  @Override
  public Stream<? extends Arguments> provideArguments(
      ParameterDeclarations parameters, ExtensionContext context) {
    String acId =
        ToppleAnnotations.find(context)
            .map(ToppleAcceptanceBinding::acId)
            .orElseThrow(
                () -> new ToppleCatException("@ToppleAcceptanceTest requires an AC binding."));
    return ToppleCaseLoader.load(acId).stream().map(Arguments::of);
  }
}
