package cbir;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Utility {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

    private Utility() {
        throw new RuntimeException("Error: can't instantiate Utility class");
    }

    /**
     * Creates a Double array of size {@code n}, with all elements initialized
     * to the given {@code value}.
     *
     * @param n     - The size of the array to create.
     * @param value - Value to initialize all elements with.
     * @return A Double array of size {@code n}, with all elements initialized
     *         to the given {@code value}.
     */
    public static Double[] doubleArray(final int n, final double value) {
        return Stream.generate(() -> value).limit(n).toArray(Double[]::new);
    }

    /**
     * Accumulates the values of the given array of values.
     *
     * @param values - Array of values to accumulate.
     * @return The sum of the values in the array of values.
     */
    public static double accumulate(final Double[] values) {
        return Stream.of(values).mapToDouble(d -> d).sum();
    }

    /**
     * Accumulates the values of the given column in the given 2D array of
     * values.
     *
     * @param values  - 2D array of values to accumulate column of.
     * @param column  - Index of the column to be accumulated.
     * @param n       - The number of rows to accumulation: from row 0 - n.
     * @return The sum of the values in the specified column.
     */
    public static double accumulateColumn(final Double[][] values,
                                          final int        column,
                                          final int        n) {
        return IntStream.range(0, n).mapToDouble(i -> values[i][column]).sum();
    }

    /**
     * Finds the minimum non-zero value from an array of values.
     *
     * @param values - Array of values to search for minimum non-zero value in.
     * @return The minimum non-zero value, or {@code Double.MAX_VALUE} if no
     *         non-zero value is found.
     */
    public static double minNonZeroValue(final Double[] values) {
        return Arrays.stream(values).filter(value -> value != 0).min(Double::compare).orElse(Double.MAX_VALUE);
    }

    /**
     * Concatenates multiple arrays of Double values into a single array.
     *
     * @param arrays - List of arrays to be concatenated.
     * @return A new Double array containing all elements from the input arrays.
     */
    public static Double[] concatenateArray(Double[]... arrays) {
        return Stream.of(arrays).flatMap(Arrays::stream).toArray(Double[]::new);
    }

    /**
     * Simple wrapper method around {@code System.arraycopy} to copy the
     * values of the source array into the given destination.
     *
     * @param source      - Array to copy values from.
     * @param destination - Array to copy values into.
     */
    public static void copyArray(final Double[] source,
                                 final Double[] destination) {
        System.arraycopy(source, 0, destination, 0, source.length);
    }

    /**
     * Performs a natural comparison between the two strings, a and b.
     *
     * <br>
     * <br>
     * This method compares two strings using a natural ordering approach,
     * where numeric substrings within the strings are compared as numbers,
     * and the remaining substrings are compared as strings. This is useful for
     * sorting strings that contain numbers in a human-friendly way.
     *
     * @param a - The first string for comparison.
     * @param b - The second string for comparison.
     * @return A negative value if {@code a} is less than {@code b}, a positive
     *         value if {@code a} is greater than {@code b}, and 0 if {@code a}
     *         and {@code b} are equal.
     *
     * @see <a href="https://blog.jooq.org/how-to-order-file-names-semantically-in-java/">blog.jooq</a>
     */
    public static int naturalComparison(final String a, final String b) {
        final var segmentA = NUMBER_PATTERN.split(a);
        final var segmentB = NUMBER_PATTERN.split(b);
        final var length = Math.min(segmentA.length, segmentB.length);

        for (int i = 0, result = 0; i < length; i++, result = 0) {
            final var charA = segmentA[i].charAt(0);
            final var charB = segmentB[i].charAt(0);

            // sort numerically
            if (Character.isDigit(charA) && Character.isDigit(charB))
                result = new BigInteger(segmentA[i]).compareTo(new BigInteger(segmentB[i]));

            // sort lexicographically
            if (result == 0) result = segmentA[i].compareTo(segmentB[i]);

            // short circuit if the two strings are no longer equal
            if (result != 0) return result;
        }

        return segmentA.length - segmentB.length;
    }
}
