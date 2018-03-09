package scheduler.infrastructure;

import javafx.util.Pair;
import lombok.AllArgsConstructor;
import lombok.val;
import scheduler.capacity.*;

import java.util.regex.Pattern;

public class CapacityProviderFactory {

    public static class UnrecognizedSkillException extends RuntimeException {
        public UnrecognizedSkillException(String skill) {
            super(skill);
        }
    }

    public static class NoParameterException extends RuntimeException {
        public NoParameterException(String skill) {
            super(skill);
        }
    }

    private static final Pattern PATTERN = Pattern.compile("^\\s*(in\\s+group\\s+)?(any|(?:not\\s+)?containing)\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    @AllArgsConstructor
    private static class ParsedResult {
        boolean inGroup;
        String matcher;
        String value;
    }

    public CapacityProvider create(String description, float capacity) {
        val parsed = parse(description);
        val provider = createProvider(parsed, capacity);
        if (provider != null) {
            if (parsed.inGroup)
                return new InGroupProvider(provider);
            return provider;
        }
        throw new UnrecognizedSkillException(description);
    }

    private static CapacityProvider createProvider(ParsedResult parsed, float capacity) {
        if (parsed != null) {
            switch (parsed.matcher) {
                case "any":
                    return new StaticCapacityProvider(capacity);
                case "containing":
                    if (parsed.value.isEmpty())
                        throw new NoParameterException(parsed.matcher);
                    return new NameContainsProvider(parsed.value, capacity);
                case "not containing":
                    if (parsed.value.isEmpty())
                        throw new NoParameterException(parsed.matcher);
                    return new NameNotContainsProvider(parsed.value, capacity);
            }
        }
        return null;
    }

    private static ParsedResult parse(String description) {
        val matcher = PATTERN.matcher(description);
        if (!matcher.matches())
            return null;
        return new ParsedResult(
                matcher.group(1) != null,
                matcher.group(2).toLowerCase().trim().replaceAll("\\s+", " "),
                matcher.group(3).trim());
    }

}
