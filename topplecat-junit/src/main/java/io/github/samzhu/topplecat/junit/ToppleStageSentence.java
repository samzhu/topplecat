package io.github.samzhu.topplecat.junit;

import io.github.samzhu.topplecat.core.ToppleCatException;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Shared sentence rules for runtime stages and static reviewer rendering. */
public final class ToppleStageSentence {
    private static final Pattern BRACED_TOKEN = Pattern.compile("\\{([^}]*)}");
    private static final Pattern VALID_PLACEHOLDER = Pattern.compile("\\{\\d+}");

    private ToppleStageSentence() {
    }

    /** Renders a sentence from runtime values, rejecting an invalid {@link As} declaration. */
    public static String runtime(String methodName, String template, Object[] arguments, String declaration) {
        List<String> values = Arrays.stream(arguments == null ? new Object[0] : arguments)
                .map(String::valueOf).toList();
        return render(methodName, template, values, declaration, false);
    }

    /**
     * Renders a reviewer sentence from Java source expressions. When a template needs
     * values that static analysis cannot obtain, the expressions remain visible instead
     * of fabricating runtime values.
     */
    public static String staticSentence(String methodName, String template, List<String> arguments) {
        return render(methodName, template, List.copyOf(arguments == null ? List.of() : arguments),
                "static review", true);
    }

    private static String render(String methodName, String template, List<String> arguments,
                                 String declaration, boolean retainUnavailableArguments) {
        if (template == null) {
            return appendArguments(words(methodName), arguments);
        }
        if (template.isBlank()) {
            throw new ToppleCatException("@As on " + declaration + " must contain a sentence.");
        }
        Matcher matcher = BRACED_TOKEN.matcher(template);
        StringBuffer rendered = new StringBuffer();
        boolean hasPlaceholder = false;
        while (matcher.find()) {
            String token = matcher.group(1);
            if (!token.matches("\\d+")) {
                throw new ToppleCatException("@As on " + declaration + " uses invalid placeholder {" + token + "}. "
                        + "Use zero-based placeholders such as {0}.");
            }
            int index = Integer.parseInt(token);
            if (index >= arguments.size()) {
                if (retainUnavailableArguments) {
                    return appendArguments(template, arguments);
                }
                throw new ToppleCatException("@As on " + declaration + " references {" + index + " but received only "
                        + arguments.size() + " arguments.");
            }
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(arguments.get(index)));
            hasPlaceholder = true;
        }
        matcher.appendTail(rendered);
        if (VALID_PLACEHOLDER.matcher(template).replaceAll("").contains("{")
                || VALID_PLACEHOLDER.matcher(template).replaceAll("").contains("}")) {
            throw new ToppleCatException("@As on " + declaration + " has unmatched braces.");
        }
        return hasPlaceholder ? rendered.toString() : appendArguments(rendered.toString(), arguments);
    }

    private static String appendArguments(String sentence, List<String> arguments) {
        return arguments.isEmpty() ? sentence : sentence + " " + String.join(", ", arguments);
    }

    private static String words(String methodName) {
        return methodName.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').trim();
    }
}
