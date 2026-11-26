package com.leonardobishop.quests.common.tasktype.key;

import com.leonardobishop.quests.common.config.ConfigProblem;
import com.leonardobishop.quests.common.config.ConfigProblemDescriptions;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Platform independent {@link TaskKeyParser} implementations.
 */
@NullMarked
public final class TaskKeyParsers {

    private TaskKeyParsers() {
    }

    // -------------------------------------------------------------------------------------------
    // Scalars
    // -------------------------------------------------------------------------------------------

    /**
     * Accepts integral numbers which fit in an {@code int}.
     */
    public static TaskKeyParser<Integer> integer() {
        return (raw, context) -> {
            if (raw instanceof Integer i) {
                return i;
            }
            if ((raw instanceof Long || raw instanceof Short || raw instanceof Byte) && fitsInt(((Number) raw).longValue())) {
                return ((Number) raw).intValue();
            }

            context.error("Expected an integer for '" + context.getPath() + "', but got '" + raw + "' instead");
            return null;
        };
    }

    /**
     * Accepts any integral number.
     */
    public static TaskKeyParser<Long> longValue() {
        return (raw, context) -> {
            if (raw instanceof Long || raw instanceof Integer || raw instanceof Short || raw instanceof Byte) {
                return ((Number) raw).longValue();
            }

            context.error("Expected a long for '" + context.getPath() + "', but got '" + raw + "' instead");
            return null;
        };
    }

    /**
     * Accepts any number.
     */
    public static TaskKeyParser<Double> decimal() {
        return (raw, context) -> {
            if (raw instanceof Number number) {
                return number.doubleValue();
            }

            context.error("Expected a number for '" + context.getPath() + "', but got '" + raw + "' instead");
            return null;
        };
    }

    public static TaskKeyParser<Boolean> bool() {
        return (raw, context) -> {
            if (raw instanceof Boolean b) {
                return b;
            }

            context.error("Expected a boolean for '" + context.getPath() + "', but got '" + raw + "' instead");
            return null;
        };
    }

    /**
     * Accepts any scalar value and converts it to a string.
     */
    public static TaskKeyParser<String> string() {
        return (raw, context) -> {
            if (isScalar(raw)) {
                return String.valueOf(raw);
            }

            context.error("Expected a string for '" + context.getPath() + "', but got '" + raw + "' instead");
            return null;
        };
    }

    // -------------------------------------------------------------------------------------------
    // Collections
    // -------------------------------------------------------------------------------------------

    /**
     * Accepts either a single scalar or a list of scalars.
     */
    public static TaskKeyParser<List<String>> stringList() {
        return list(string());
    }

    /**
     * Accepts either a single scalar or a list of scalars.
     */
    public static TaskKeyParser<Set<String>> stringSet() {
        return stringList().map(list -> Collections.unmodifiableSet(new LinkedHashSet<>(list)));
    }

    /**
     * Accepts either a single value or a list of values; every element is parsed by
     * {@code elementParser}. Elements for which the element parser returns null are skipped
     * (the element parser is responsible for reporting them).
     *
     * @param elementParser parser applied to each element
     * @param <E>           element type
     * @return unmodifiable list parser
     */
    public static <E> TaskKeyParser<List<E>> list(final TaskKeyParser<E> elementParser) {
        Objects.requireNonNull(elementParser, "elementParser cannot be null");

        return (raw, context) -> {
            final List<?> rawList = raw instanceof List<?> l ? l : List.of(raw);
            final List<E> result = new ArrayList<>(rawList.size());

            for (final Object element : rawList) {
                if (element == null) {
                    continue;
                }

                final E parsed = elementParser.parse(element, context);
                if (parsed != null) {
                    result.add(parsed);
                }
            }

            return Collections.unmodifiableList(result);
        };
    }

    // -------------------------------------------------------------------------------------------
    // Enums and accepted values
    // -------------------------------------------------------------------------------------------

    /**
     * Parses a constant of the given enum by its exact name. Unknown values are reported as warnings.
     */
    public static <E extends Enum<E>> TaskKeyParser<E> enumValue(final Class<E> enumClass) {
        Objects.requireNonNull(enumClass, "enumClass cannot be null");

        final Map<String, E> constants = new LinkedHashMap<>();
        for (final E constant : enumClass.getEnumConstants()) {
            constants.put(constant.name(), constant);
        }

        return acceptedValue(constants);
    }

    /**
     * Parses one or a list of constants of the given enum by their exact name.
     */
    public static <E extends Enum<E>> TaskKeyParser<List<E>> enumList(final Class<E> enumClass) {
        return list(enumValue(enumClass));
    }

    /**
     * Maps an accepted (exact, case-sensitive) string to a value.
     * Unknown values are reported as warnings listing all accepted values.
     *
     * @param acceptedValues map of accepted string to value; iteration order is used in the problem description
     */
    public static <V> TaskKeyParser<V> acceptedValue(final Map<String, V> acceptedValues) {
        Objects.requireNonNull(acceptedValues, "acceptedValues cannot be null");

        final Map<String, V> copy = Collections.unmodifiableMap(new LinkedHashMap<>(acceptedValues));

        return (raw, context) -> {
            if (!(raw instanceof String string)) {
                context.warning("Expected a string for '" + context.getPath() + "', but got '" + raw + "' instead");
                return null;
            }

            final V value = copy.get(string);
            if (value == null) {
                final StringBuilder extended = new StringBuilder("The accepted values are:");
                for (final String accepted : copy.keySet()) {
                    extended.append("<br> - ").append(accepted);
                }

                context.report(ConfigProblem.ConfigProblemType.WARNING,
                        ConfigProblemDescriptions.NOT_ACCEPTED_VALUE.getDescription(string, context.getTaskType()),
                        extended.toString());
            }

            return value;
        };
    }

    /**
     * Like {@link #acceptedValue(Map)} but for plain strings.
     */
    public static TaskKeyParser<String> acceptedString(final Collection<String> acceptedValues) {
        Objects.requireNonNull(acceptedValues, "acceptedValues cannot be null");

        final Map<String, String> map = new LinkedHashMap<>();
        for (final String value : acceptedValues) {
            map.put(value, value);
        }

        return acceptedValue(map);
    }

    // -------------------------------------------------------------------------------------------

    private static boolean fitsInt(final long value) {
        return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE;
    }

    private static boolean isScalar(final Object raw) {
        return raw instanceof String || raw instanceof Number || raw instanceof Boolean || raw instanceof Character;
    }

    /**
     * Returns the given raw value as a plain scalar string, or null if it is not a scalar.
     */
    public static @Nullable String scalarToString(final @Nullable Object raw) {
        return raw != null && isScalar(raw) ? String.valueOf(raw) : null;
    }
}
