package cbir;

public class FeatureMatrix {
    private final ImageCollection imageCollection;
    private final Double[][]      intensityDistance;
    private final Double[][]      colorCodeDistance;
    private final Double[][]      normalized;

    public FeatureMatrix(final ImageCollection imageCollection) {
        if (imageCollection == null)        throw new RuntimeException("FeatureMatrix: can't process null ImageCollection");
        if (imageCollection.getSize() == 0) throw new RuntimeException("FeatureMatrix: can't process empty ImageCollection");

        final var intensity = new Double[imageCollection.getSize()][];
        final var colorCode = new Double[imageCollection.getSize()][];
        final var sizes     = new int[imageCollection.getSize()];

        // get intensity and color-code histogram for each image in imageCollection
        for (int i = 0; i < imageCollection.getSize(); i++) {
            final var colors = imageCollection.getPixelValuesOfImage(i);
            intensity[i]     = Histogram.intensityHistogram(colors);
            colorCode[i]     = Histogram.colorCodeHistogram(colors);
            sizes[i]         = colors.size();
        }

        this.intensityDistance = calculateDistanceMatrix(intensity, null, sizes);
        this.colorCodeDistance = calculateDistanceMatrix(colorCode, null, sizes);
        this.normalized        = calculateNormalizedMatrix(intensity, colorCode, sizes);
        this.imageCollection   = imageCollection;
    }

    public final Double[][] getIntensityDistanceMatrix() { return intensityDistance; }
    public final Double[][] getColorCodeDistanceMatrix() { return colorCodeDistance; }
    public final ImageCollection getImageCollection()    { return imageCollection;   }

    /**
     * Compares two image indices, {@code a} and {@code b}, based on their
     * respective distances from a reference image, {@code image}, within the
     * given distance matrix. The comparison is performed by evaluating the
     * differences in distances between the reference image, {@code image},
     * and images {@code a} and {@code b}.
     *
     * <ul>
     * <li> If the result is positive, image {@code a} is closer to the
     *      reference image than image {@code b}.
     * <li> If the result is negative, image {@code b} is closer to the
     *      reference image than image {@code b}.
     * <li> If the result is zero, both images are equidistant from the
     *      reference image.
     * </ul>
     *
     * <br>
     * <br>
     * The method calculates the difference in distances using the following
     * formula: {@code difference = matrix[image][a] - matrix[image][b]}
     *
     * For more information about the formula above, please see
     * {@link #calculateDistanceMatrix(Double[][], Double[], int[])
     * calculateDistanceMatrix}
     *
     * @param matrix - The distance matrix containing distances between images.
     * @param image  - The index of the reference image in the distance matrix.
     * @param a      - The index of the first image to compare with the
     *                 reference image.
     * @param b      - The index of the second image to compare with the
     *                 reference image.
     * @return An integer value: {@code -1}, {@code 0}, or {@code 1} if the
     *         distance between {@code a } and the reference image is less than,
     *         equal to, or greater than the distance between {@code b} and the
     *         reference image.
     */
    public static int compare(final Double[][] matrix,
                              final int        image,
                              final int        a,
                              final int        b) {
        final var difference = matrix[image][a] - matrix[image][b];
        if (difference > 0) return 1;
        if (difference < 0) return -1;
        return 0;
    }

    /**
     * Implements a simplified Relevance Feedback (RF) algorithm to calculate
     * the distance between the query image and all other images based on the
     * weights obtained from the set of selected images that are considered
     * relevant. If no images are selected as relevant besides the initial query
     * image, each feature will have the same weight. Since the set of relevant
     * images is not constant, this value cannot be cached. Therefore, each time
     * this method is called, the returned distance matrix is newly generated.
     *
     * <br>
     * <br>
     * To get the distance between image i and image j from the returned
     * distance matrix, you can access it by visiting the distance matrix
     * as follows:
     *
     * <ul>
     * <li> distance between image i and j = {@code rfMatrix[i][j]}
     * <li> distance between image i and j = {@code rfMatrix[j][i]}
     * </ul>
     *
     * @param image    - The index of the query image for which the relevance
     *                   analysis is performed.
     * @param order    - An array indicating the order of images for relevance
     *                   analysis.
     * @param relevant - A boolean array indicating whether each image is
     *                   considered relevant or not.
     * @return A 2D {@code Double} array representing the distance matrix
     *         between the query image and all other images, calculated
     *         based on the weights obtained from the relevant images.
     */
    public Double[][] relevanceAnalysis(final int       image,
                                        final Integer[] order,
                                        final boolean[] relevant) {
        final var feedbackMatrix = new Double[normalized.length][normalized[0].length];
        var imageCount = 0;

        // get selected image
        Utility.copyArray(normalized[image], feedbackMatrix[imageCount++]);

        // get all other images that are marked relevant
        for (int i = 0; i < normalized.length; i++) {
            final var index = order[i];
            if (relevant[index] && index != image)
                Utility.copyArray(normalized[index], feedbackMatrix[imageCount++]);
        }

        // calculate weight for each feature based on feedback matrix
        final var weight = calculateFeatureWeight(feedbackMatrix, imageCount, feedbackMatrix[0].length);

        // return the new distance matrix based on rf analysis
        return calculateDistanceMatrix(normalized, weight, null);
    }

    /**
     * Calculates distance matrix for the given feature matrix containing a list
     * histograms. The distance matrix returned stores the distance between any
     * two images, i and j. To get the distance between image i and image j,
     * you can access it by visiting the returned distance matrix as follows:
     *
     * <ul>
     * <li> distance between image i and j = {@code distanceMatrix[i][j]}
     * <li> distance between image i and j = {@code distanceMatrix[j][i]}
     * </ul>
     *
     * If this method is supplied an array of weights to apply to each feature,
     * this will calculate the weighted distance matrix for the given feature
     * matrix.
     *
     * @apiNote When calling this method to get the weighted distance, the
     *          feature matrix supplied should have normalized bin values
     *          and size should be left {@code null} as it will not be used for
     *          any calculations.
     *
     * @param matrix - Feature matrix containing a list of histogram values for
     *                 a set of images to calculate distance matrix of.
     * @param weight - Array containing the weight to apply to each bin when
     *                 calculating weighted distance. If you do not want the
     *                 distance being calculated, leave this field {@code null}.
     * @param size   - Array containing the size of each image. The image size
     *                 is its resolution. This can be left {@code null} if you
     *                 are trying to calculate the weighted distance matrix.
     * @return An array of {@code Double} values that represent the distance
     *         matrix for the given feature matrix. This distance matrix is
     *         equal in size to the given feature matrix.
     */
    private Double[][] calculateDistanceMatrix(final Double[][] matrix,
                                               final Double[]   weight,
                                               final int[]      size) {
        final var distanceMatrix = new Double[matrix.length][matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j <= i; j++) {
                final var distance = (weight == null) ? Histogram.calculateDistance(matrix[i], matrix[j], size[i], size[j], matrix[i].length) :
                                                        Histogram.calculateWeightedDistance(matrix[i], matrix[j], weight, weight.length);
                distanceMatrix[i][j] = distance;
                distanceMatrix[j][i] = distance;
            }
        }
        return distanceMatrix;
    }

    /**
     * Calculates the feature weights to be used in the distance calculations
     * for a given set of images and histogram bins. The feature weights are
     * computed using the following formula:
     *
     * <ul>
     * <li> {@code weight}: {@code wi = 1 / σi}
     * <li> {@code normalize}: {@code wi = wi / (Σwi)}
     * </ul>
     *
     * <br>
     * <br>
     * If this method is called during the initial iteration of RF
     * (when only the query image is selected as relevant), it assigns equal
     * weights to all features, ensuring that each feature contributes equally
     * to the distance calculation. However, during subsequent iterations with
     * additional relevant images, this method calculates updated feature
     * weights based on the statistical properties of the histogram bins.
     *
     * @param matrix     - Feature matrix containing histogram values for a set
     *                     of images to calculate feature weight of.
     * @param imageCount - The number of images (including the query image)
     *                     in the relevant set.
     * @param binSize    - The number of bins in each histogram.
     * @return An array of {@code Double} values representing the calculated
     *         feature weights. These weights determine the contribution of
     *         each feature to the distance calculation.
     */
    private Double[] calculateFeatureWeight(final Double[][] matrix,
                                            final int        imageCount,
                                            final int        binSize) {
        // Check if this is the first iteration of RF. If so, then all features will have the same weight.
        if (imageCount == 1) return Utility.doubleArray(binSize, 1.0 / binSize);

        // calculate updated weight for each feature and return the feature weight
        final var mean  = Histogram.calculateBinMean(matrix, binSize, imageCount);
        final var stdev = Histogram.calculateBinStandardDeviation(matrix, mean, imageCount);
        return Histogram.calculateNormalizedFeatureWeight(stdev, mean, binSize);
    }

    /**
     * Calculates the normalized feature matrix for the given intensity and
     * color code matrices. For each image, the normalized feature matrix is
     * generated by concatenating the intensity and color code bin values,
     * and then dividing each bin value by the image size. Subsequently, the
     * mean value and standard deviation for each bin are computed, and each
     * feature is normalized using the formula:
     *
     * <ul>
     * <li> {@code vi = (vi - μi) / σi}
     * <li> {@code vi} is the feature value of {@code i}
     * <li> {@code μi} is the mean of feature {@code i}
     * <li> {@code σi} is the standard deviation for feature {@code i}
     * </ul>
     *
     * @param intensity - 2D array containing the intensity histograms for a
     *                    collection of images.
     * @param colorCode - 2D array containing the color code histograms for a
     *                    collection of images.
     * @param imageSize - An array containing the size of each image,
     *                    representing their resolution.
     * @return A 2D {@code Double} array representing the normalized feature
     *         matrix for the given histogram values for a set of images.
     */
    private Double[][] calculateNormalizedMatrix(final Double[][] intensity,
                                                 final Double[][] colorCode,
                                                 final int[]      imageSize) {
        final int binSize = intensity[0].length + colorCode[0].length;

        // combine intensity and color-code matrix and normalize values
        final var matrix = new Double[imageSize.length][binSize];
        for (int i = 0; i < imageSize.length; i++) {
            matrix[i] = Utility.concatenateArray(intensity[i], colorCode[i]);
            for (int j = 0; j < binSize; j++) matrix[i][j] /= imageSize[i];
        }

        final var mean  = Histogram.calculateBinMean(matrix, binSize, imageSize.length);
        final var stdev = Histogram.calculateBinStandardDeviation(matrix, mean, imageSize.length);

        // normalize by apply the following formula: v = (v - μ) / σ
        for (int i = 0; i < binSize; i++) {
            for (int j = 0; j < imageSize.length; j++) {
                matrix[j][i] -= mean[i];                     // x = (v - μ)
                if (stdev[i] > 0) matrix[j][i] /= stdev[i];  // x / σ
            }
        }
        return matrix;
    }
}
