package cbir;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

public class ImageCollection {
    private static final String[] EXTENSIONS = {"png", "jpg", "jpeg"};

    private final List<BufferedImage> images; // images in the given directory
    private final List<String>        names;  // names of the files in the directory
    private final List<Integer>       sizes;  // the size of each image
    private int                       size;   // total number of images in the directory

    ImageCollection(final String directory) {
        images = new ArrayList<>();
        names  = new ArrayList<>();
        sizes  = new ArrayList<>();
        size   = 0;
        loadImages(directory);
    }

    public final List<BufferedImage> getImages() { return images; }
    public final List<String>        getNames()  { return names;  }
    public final List<Integer>       getSizes()  { return sizes;  }
    public final int                 getSize()   { return size;   }

    public final BufferedImage getImageAt(final int index)     { return images.get(index); }
    public final String        getNameAt(final int index)      { return names.get(index);  }
    public final int           getSizeOfImage(final int index) { return sizes.get(index);  }

    /**
     * Retrieves the pixel values of an image at the specified index and
     * returns them as a list of integers.
     *
     * @param index - The index of the image for which to retrieve pixel values.
     * @return A list containing the pixel values of the image at the
     *         specified index in this {@code ImageCollection} object.
     */
    public final List<Integer> getPixelValuesOfImage(final int index) {
        final var image       = getImageAt(index);
        final var pixelValues = new ArrayList<Integer>();

        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                pixelValues.add(image.getRGB(x, y));
        return pixelValues;
    }

    /**
     * Loads images from the given directory. The method scans the directory for
     * image files, processes each valid image file, and populates the internal
     * collection with image data.
     *
     * @param directory - The directory path containing the image files to load.
     */
    private void loadImages(final String directory) {
        final var files = new File(directory).listFiles();
        if (files == null || files.length == 0) {
            System.out.println("ImageCollection: failed to find any files in the given directory");
            return;
        }

        Arrays.sort(files, (a, b) -> Utility.naturalComparison(a.getName(), b.getName()));
        for (final var file : files) {
            if (!file.isFile())                                                    continue;
            if (!Arrays.asList(EXTENSIONS).contains(getExtension(file.getName()))) continue;

            try {
                final var image = ImageIO.read(file);
                if (image == null) continue;

                images.add(image);
                names.add(file.getName());
                sizes.add(image.getWidth() * image.getHeight());
                size++;
            } catch (Exception ignored) {}
        }
    }

    /**
     * Extracts the file extension from a given filepath. This method processes
     * the provided filepath and extracts the extension, which is the part of
     * the filename that comes after the last dot ({@code '.'}) character. The
     * extracted extension is converted to lowercase and returned.
     *
     * <br>
     * <br>
     * If the input filepath is null, empty, or doesn't contain a dot
     * ({@code '.'}) character, an empty string is returned.
     *
     * @param filepath - The filepath to a file extract extension from.
     * @return A lowercase string representing the extracted file extension.
     */
    private String getExtension(final String filepath) {
        if (filepath == null || filepath.isEmpty()) return "";
        if (!filepath.contains("."))              return "";

        final var finalDot = filepath.lastIndexOf('.');
        return filepath.substring(finalDot + 1).toLowerCase();
    }
}

