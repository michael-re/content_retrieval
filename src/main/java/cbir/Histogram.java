package cbir;

import java.util.List;
import java.util.stream.IntStream;

public class Histogram {
    private static final double RED_INTENSITY   = 0.299;
    private static final double GREEN_INTENSITY = 0.587;
    private static final double BLUE_INTENSITY  = 0.114;

    private Histogram() {
        throw new RuntimeException("Error: can't instantiate Histogram class");
    }

    /**
     * Calculates the distance between the two sets of histograms, each
     * representing an image. The value calculated by this method indicates
     * the dissimilarity between the two histograms, reflecting how different
     * the image represented by {@code h1} is from the image represented by
     * {@code h2}.
     *
     * <br>
     * <br>
     * To compute the distance between the two histograms, this method
     * calculates the sum of the Manhattan distance between the bins of
     * each histogram using the following formula:
     *
     * <br>
     * <br>
     * {@code distance = Σ | H1(i)/s1 - H2(i)/s2 |}
     *
     * <ul>
     * <li> {@code H1(i)} - The ith bin of the histogram for the first image
     * <li> {@code H2(i)} - The ith bin of the histogram for the second image
     * <li> {@code H1(i)} - The ith bin of the histogram for the first image
     * <li> {@code s1}    - The number of pixels in the first image
     * <li> {@code s2}    - The number of pixels in the second image
     * </ul>
     *
     * <br>
     * <br>
     * A value of {@code 0.0} implies that the two histograms are identical.
     * As the value increases, the dissimilarity between the histograms grows.
     *
     * @param h1 - The histogram of the first image.
     * @param h2 - The histogram of the second image.
     * @param s1 - The number of pixels in the first image.
     * @param s2 - The number of pixels in the second image.
     * @param sB - The number of bins to compare between the two histograms.
     * @return A double representing the total distance between the two images
     *         represented by the histograms {@code h1} and {@code h2}.
     */
    public static double calculateDistance(final Double[] h1,
                                           final Double[] h2,
                                           final int      s1,
                                           final int      s2,
                                           final int      sB) {
        return IntStream.range(0, sB).mapToDouble(i -> Math.abs((h1[i] / s1) - (h2[i] / s2))).sum();
    }

    /**
     * Calculates the weighted distance between the two sets of histograms, each
     * representing an image. The value calculated by this method indicates
     * the dissimilarity between the two histograms, reflecting how different
     * the image represented by {@code h1} is from the image represented by
     * {@code h2}. When calculating the weighted distance between {@code h1}
     * and {@code h2}, the distance between any given two bins is scaled by
     * the corresponding weight provided in {@code w}.
     *
     * <br>
     * <br>
     * To compute the weighted distance between two histogram sets, this
     * method calculates the weighted Manhattan distance using the following
     * formula:
     *
     * <br>
     * <br>
     * {@code weighted distance = Σ w(i) * | H1(i) - H2(i) |}
     *
     * <ul>
     * <li> {@code w(i)}  - The weight assigned to the ith bin of the two
     *                      histograms
     * <li> {@code H1(i)} - The ith bin of the histogram values for the first
     *                      image
     * <li> {@code H2(i)} - The ith bin of the histogram values for the second
     *                      image
     * </ul>
     *
     * <br>
     * <br>
     * A value of {@code 0.0} implies that the two histograms are identical.
     * As the value increases, the dissimilarity between the histograms grows.
     *
     * @param h1 - The histogram of the first image.
     * @param h2 - The histogram of the second image.
     * @param w  - Weight assigned to each histogram's bin.
     * @param sB - The number of bins to compare between the two histograms.
     * @return A double representing the weighted distance between the two
     *         images represented by the histograms {@code h1} and {@code h2},
     *         considering the assigned weights in {@code w}.
     */
    public static double calculateWeightedDistance(final Double[] h1,
                                                   final Double[] h2,
                                                   final Double[] w,
                                                   final int      sB) {
        return IntStream.range(0, sB).mapToDouble(i -> w[i] * Math.abs(h1[i] - h2[i])).sum();
    }

    /**
     * Calculates the mean value of each bin in the given histogram matrix,
     * which represents the histogram values for some collection of images.
     * Each bin mean in the returned array reflects the average value of the
     * corresponding bin across all images in the matrix.
     *
     * @param matrix  - A matrix containing the histograms of multiple images.
     * @param binSize - The number of bins in each histogram.
     * @param n       - The number of images to get mean of.
     * @return An array of {@code Double} values representing the mean values
     *         of bins across all images in the histogram matrix from {@code 0}
     *         to {@code n}.
     */
    public static Double[] calculateBinMean(final Double[][] matrix,
                                            final int        binSize,
                                            final int        n) {
        final var binMean = Utility.doubleArray(binSize, 0);
        IntStream.range(0, binSize).forEach(i -> binMean[i] = Utility.accumulateColumn(matrix, i, n) / n);
        return binMean;
    }

    /**
     * Calculates the standard deviations for the bin values in the given
     * matrix, which represents the histogram values for some collection of
     * images. The standard deviation of each bin reflects the degree of
     * variability in the corresponding bin value across all images in the
     * matrix.
     *
     * @param matrix  - A matrix containing the histograms of multiple images.
     * @param mean    - The mean value of each bin in the given matrix.
     * @param n       - The number of images to get standard deviations of.
     * @return An array of {@code Double} values representing the standard
     *         deviations of bins across all images in the histogram matrix
     *         from {@code 0} to {@code n}.
     */
    public static Double[] calculateBinStandardDeviation(final Double[][] matrix,
                                                         final Double[]   mean,
                                                         final int        n) {
        final var standardDeviation = Utility.doubleArray(mean.length, 0);
        IntStream.range(0, standardDeviation.length).forEach(i -> {
            final var ssd = IntStream.range(0, n).mapToDouble(j -> Math.pow(matrix[j][i] - mean[i], 2)).sum();
            standardDeviation[i] = Math.sqrt(ssd / (n - 1));
        });
        return standardDeviation;
    }

    /**
     * Calculates the normalized feature weights based on the standard deviation
     * and mean of a set of features for a given matrix of histogram values.
     * The normalized feature weight represents the importance of each feature
     * in the given matrix of histogram values.
     * 
     * <br>
     * <br>
     * To compute the normalized feature weights, this method calculates weights
     * for each feature using the following formula:
     *
     * <ul>
     * <li> {@code weight[i]} = {@code 1.0 / stdev[i]} if {@code stdev[i]}
     *      is non-zero
     * <li> {@code weight[i]} = {@code minstdev} if {@code stdev[i]} is zero
     *      and {@code mean[i]} is non-zero
     * <li> {@code weight[i]} = {@code 0.0} if both {@code stdev[i]} and
     *      {@code mean[i]} are zero
     * </ul>
     *
     * <br>
     * <br>
     * Keep in mind, the resulting weights are normalized to ensure they sum up
     * to {@code 1.0}.
     *
     * @param stdev - An array that holds the standard deviations of each
     *                feature for a given matrix of histogram values.
     * @param mean  - An array that holds the mean value of each feature for a
     *                given matrix of histogram values.
     * @param n     - The number of images to get normalized feature weights of.
     * @return An array of {@code Double} values that represent the normalized
     *         feature weights for a given matrix of histogram values.
     */
    public static Double[] calculateNormalizedFeatureWeight(final Double[] stdev,
                                                            final Double[] mean,
                                                            final int      n) {
        final var minstdev = 2 / Utility.minNonZeroValue(stdev);
        final var weight   = Utility.doubleArray(n, 0);

        IntStream.range(0, n).forEach(i -> {
            if      (stdev[i] != 0)                 weight[i] = 1.0 / stdev[i];
            else if (stdev[i] == 0 && mean[i] != 0) weight[i] = minstdev;
        });

        // normalize feature weight
        final var total = Utility.accumulate(weight);
        if (total != 0) IntStream.range(0, n).forEach(i -> weight[i] /= total);

        return weight;
    }

    /**
     * Generates an intensity histogram for the given image. To generate the
     * intensity values of each color, the 24-bit RGB value is transformed into
     * a single 8-bit value. This value is calculated using the following
     * formula: {@code I = 0.299R + 0.587G + 0.114B}
     *
     * <br>
     * <br>
     * The intensity histogram will have 25 bins in total. Each bin of the
     * histogram corresponds to a specific range of intensity values,
     * mapping as follows:
     *
     * <ul>
     * <li> {@code Bin 01} - Intensity range (0, 10)
     * <li> {@code Bin 02} - Intensity range (10, 20)
     * <li> {@code Bin 03} - Intensity range (20, 30)
     * <li> {@code ......} - ....
     * <li> {@code Bin 25} - Intensity range (240, 255)
     * </ul>
     *
     * <br>
     * <br>
     * Do keep in mind, each bin encompasses a range of 10 intensity values.
     * However, the final bin encompasses 15 intensity values in total.
     *
     * @param colors - A list of integer values representing the rgb value of
     *                 each pixel in some image.
     * @return An array of Double values representing the intensity histogram.
     */
    public static Double[] intensityHistogram(final List<Integer> colors) {
        final var histogram = Utility.doubleArray(25, 0);
        for (final var value : colors) {
            final var red       = RED_INTENSITY   * ((value >> 16) & 0xFF);
            final var green     = GREEN_INTENSITY * ((value >>  8) & 0xFF);
            final var blue      = BLUE_INTENSITY  * (value & 0xFF);
            final var intensity = Math.min(240, (int) (red + green + blue));
            histogram[intensity / 10]++;
        }
        return histogram;
    }

    /**
     * Generates a color code histogram for the given image. Each color code is
     * derived from the 24-bit RGB value by dividing each color component by 64,
     * resulting in a single 6-bit code for each component. These individual
     * color codes are then combined to create a final 6-bit color code value
     * for the pixel.
     *
     * <br>
     * <br>
     * The color code histogram will have 64 bins in total. Each bin in the
     * histogram corresponds to a specific 6-bit color code, mapping as follows:
     *
     * <ul>
     * <li> {@code Bin 01} - Color code {@code 000000}
     * <li> {@code Bin 02} - Color code {@code 000001}
     * <li> {@code Bin 03} - Color code {@code 000010}
     * <li> {@code ......} - ....
     * <li> {@code Bin 64} - Color code {@code 111111}
     * </ul>
     *
     * <br>
     * <br>
     * Each bin in the histogram represents a unique combination of color codes
     * for the red, green, and blue components of the pixel.
     *
     * @param colors - A list of integer values representing the rgb value of
     *                 each pixel in some image.
     * @return An array of {@code Double} values representing the color code
     *         histogram.
     */
    public static Double[] colorCodeHistogram(final List<Integer> colors) {
        final var histogram = Utility.doubleArray(64, 0);
        for (final var value : colors) {
            final var red   = ((value >> 16) & 0xFF) >> 6;
            final var green = ((value >>  8) & 0xFF) >> 6;
            final var blue  = (value & 0xFF)         >> 6;
            final var code  = (red << 4) | (green << 2) | blue;
            histogram[code]++;
        }
        return histogram;
    }
}
