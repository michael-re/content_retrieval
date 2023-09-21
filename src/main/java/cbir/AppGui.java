package cbir;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.util.Arrays;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

public class AppGui extends JFrame {
    private final static int   IMAGES_PER_PAGE = 20;
    private final static Color BUTTON_COLOR    = new Color(243, 250, 254);
    private final static Font  DEFAULT_FONT    = new Font("Helvetica", Font.BOLD, 14);

    private JPanel topPanel;
    private JPanel bottomPanel;
    private JPanel imageViewPanel;

    private JLabel    selectedPageView;
    private JLabel    selectedImageView;
    private JCheckBox relevanceCheckBox;

    private JLabel[]    imageIconLabel;
    private Integer[]   imageIconOrder;
    private JButton[]   imageIconButton;
    private JCheckBox[] imageIconCheckBox;

    private int currentPageNumber;
    private int lastPageNumber;

    private int[][] pages;

    private String currentDirectory;

    private transient FeatureMatrix matrix;

    public AppGui() {
        setTitle("CBIR");
        setSize(1000, 1000);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        initializeComponents();
        createMenuBar();
    }

    private void initializeComponents() {
        // create components
        topPanel          = new JPanel();
        bottomPanel       = new JPanel();
        imageViewPanel    = new JPanel();
        selectedPageView  = new JLabel();
        selectedImageView = new JLabel();
        relevanceCheckBox = new JCheckBox("Relevance");

        // set colors
        getContentPane().setBackground(Color.WHITE);
        topPanel.setBackground(Color.WHITE);
        bottomPanel.setBackground(Color.WHITE);
        imageViewPanel.setBackground(Color.WHITE);
        relevanceCheckBox.setBackground(Color.WHITE);

        // set layouts
        setLayout(new GridLayout(2, 1, 5, 5));
        topPanel.setLayout(new GridLayout(1, 3, 5, 5));
        bottomPanel.setLayout(new GridLayout(4, 5, 0, 0));
        imageViewPanel.setLayout(new GridBagLayout());

        // add to this frame
        add(topPanel);
        add(bottomPanel);
    }

    private void createMenuBar() {
        // create components
        final var menuBar       = new JMenuBar();
        final var menuPanel     = new JPanel();
        final var exitButton    = new JButton("Exit");
        final var processButton = new JButton("Process");
        final var selectButton  = new JButton("Select New Folder");
        final var folderText    = new JTextField(40);

        currentDirectory = folderText.getText();

        // set colors
        menuBar.setBackground(Color.WHITE);
        menuPanel.setBackground(Color.WHITE);
        exitButton.setBackground(BUTTON_COLOR);
        processButton.setBackground(BUTTON_COLOR);
        selectButton.setBackground(BUTTON_COLOR);
        folderText.setBackground(BUTTON_COLOR);

        // add action listeners
        exitButton.addActionListener(e -> System.exit(0));
        selectButton.addActionListener(e -> openFileChooser(folderText));
        processButton.addActionListener(e -> processDirectory(folderText.getText()));

        // add menu items to panel
        menuPanel.add(exitButton);
        menuPanel.add(new JLabel("Selected Folder:"));
        menuPanel.add(folderText);
        menuPanel.add(selectButton);
        menuPanel.add(processButton);

        // add the menu to this frame
        menuBar.add(menuPanel);
        setJMenuBar(menuBar);
    }

    private void createSelectedImageView() {
        topPanel.removeAll();
        selectedImageView.setText(null);
        imageViewPanel.setBorder(BorderFactory.createTitledBorder(""));

        // create constraint to center selectedImageView in imageViewPanel
        final var constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.CENTER;
        imageViewPanel.add(selectedImageView, constraints);
        topPanel.add(imageViewPanel);
    }

    private void createNavigationAndOptionButtons() {
        // create components
        final var optionsPanel       = new JPanel();
        final var navigationPanel    = new JPanel();
        final var pageSelectionPanel = new JPanel();

        // set colors
        optionsPanel.setBackground(Color.WHITE);
        navigationPanel.setBackground(Color.WHITE);
        pageSelectionPanel.setBackground(Color.WHITE);

        // set layouts
        optionsPanel.setLayout(new GridLayout(4, 2, 10, 10));
        navigationPanel.setLayout(new GridLayout(3, 2, 5, 20));

        // create navigation buttons and attach action listeners to buttons
        final var buttonsNames = new String[]{"Intensity", "Color-Code", "Intensity + Color-Code", "Reset", "< Previous Page", "  Next Page   >"};
        final var buttons = GuiFactory.createButtons(buttonsNames);

        // Intensity
        buttons[0].addActionListener(e -> {
            resetRelevanceCheckBox();
            sortImages(matrix.getIntensityDistanceMatrix());
        });

        // Color-Code
        buttons[1].addActionListener(e -> {
            resetRelevanceCheckBox();
            sortImages(matrix.getColorCodeDistanceMatrix());
        });

        // Intensity + Color-Code
        buttons[2].addActionListener(e -> {
            final var index = getSelectedImageNumber();
            if (index != -1)
                sortImages(matrix.relevanceAnalysis(index, imageIconOrder, getMarkedImages()));
        });

        // Reset
        buttons[3].addActionListener(e -> {
            IntStream.range(0, matrix.getImageCollection().getSize()).forEach(i -> imageIconOrder[i] = i);
            selectedImageView.setIcon(null);
            imageViewPanel.setBorder(BorderFactory.createTitledBorder(""));
            resetRelevanceCheckBox();
            displayFirstPage();
        });

        buttons[4].addActionListener(e -> updatePageView(currentPageNumber - 2, true));
        buttons[5].addActionListener(e -> updatePageView(currentPageNumber, true));

        relevanceCheckBox.addActionListener(e -> {
            final var selected = relevanceCheckBox.isSelected();
            IntStream.range(0, imageIconCheckBox.length).forEach(i -> imageIconCheckBox[i].setVisible(selected));
        });

        // configure page selection panel with button and text
        pageSelectionPanel.setFont(DEFAULT_FONT);
        pageSelectionPanel.add(buttons[4]);
        pageSelectionPanel.add(buttons[5]);
        pageSelectionPanel.add(selectedPageView);

        // add buttons to panel
        IntStream.range(0, 4).forEach(i -> optionsPanel.add(buttons[i]));
        optionsPanel.add(relevanceCheckBox);

        // add components to navigation panel
        navigationPanel.add(Box.createHorizontalBox());
        navigationPanel.add(optionsPanel);
        navigationPanel.add(pageSelectionPanel);

        // add navigation panel to top panel
        topPanel.add(navigationPanel);
        topPanel.revalidate();
        topPanel.repaint();
    }

    private void loadImages() {
        // create components
        final var size    = matrix.getImageCollection().getSize();
        imageIconOrder    = new Integer[size];
        imageIconLabel    = new JLabel[size];
        imageIconButton   = new JButton[size];
        imageIconCheckBox = new JCheckBox[size];
        currentPageNumber = 1;
        lastPageNumber    = (int) Math.ceil((float) size / IMAGES_PER_PAGE);
        pages             = new int[lastPageNumber][];

        // load page information
        for (int i = 0, count = 0; i < lastPageNumber; i++) {
            pages[i] = new int[Math.min(IMAGES_PER_PAGE, size - count)];
            for (int j = 0; j < pages[i].length; j++) pages[i][j] = count++;
        }

        // load images
        IntStream.range(0, matrix.getImageCollection().getSize()).forEach(i -> {
            imageIconCheckBox[i] = GuiFactory.createCheckBox();
            imageIconButton[i]   = GuiFactory.createButton(this, matrix.getImageCollection().getImageAt(i), matrix.getImageCollection().getNameAt(i));
            imageIconLabel[i]    = GuiFactory.createButtonLabel(imageIconButton[i], imageIconCheckBox[i]);
            imageIconOrder[i]    = i;
        });
    }

    private void displayFirstPage() {
        // reset page number
        currentPageNumber = 1;
        selectedPageView.setText("page 1/" + lastPageNumber);

        // reset selected image checkbox
        final var selected = relevanceCheckBox.isSelected();
        if (selectedImageView.getIcon() != null)
            imageIconCheckBox[getSelectedImageNumber()].setSelected(selected);

        // display first page
        updatePageView(0, false);
    }

    /**
     * Updates the current page view in the bottom panel to the page specified
     * by the page index.
     *
     * @param pageIndex         - The index of the page to update the view to.
     * @param updateCurrentPage - A flag indicating whether to update the
     *                            current page number.
     */
    private void updatePageView(final int pageIndex, final boolean updateCurrentPage) {
        if (pageIndex >= pages.length) return;
        if (pageIndex < 0) {
            displayFirstPage();
            return;
        }

        // remove images from the bottom panel
        bottomPanel.removeAll();

        // add images for selected page
        Arrays.stream(pages[pageIndex]).forEach(i -> bottomPanel.add(imageIconLabel[imageIconOrder[i]]));
        final var remaining = Math.abs(IMAGES_PER_PAGE - pages[pageIndex].length);
        IntStream.range(0, remaining).forEach(i -> bottomPanel.add(new JLabel()));

        // update page number
        if (updateCurrentPage) currentPageNumber = pageIndex + 1;
        selectedPageView.setText("page " + currentPageNumber + "/" + lastPageNumber);

        // refresh bottom panel
        bottomPanel.revalidate();
        bottomPanel.repaint();
    }

    /**
     * Returns the index of the currently selected image.
     *
     * @return The index of the currently selected image or {@code -1} if no
     *         image is currently selected.
     */
    private int getSelectedImageNumber() {
        if (selectedImageView.getIcon() == null) return -1;
        final var title = ((TitledBorder) imageViewPanel.getBorder()).getTitle();
        return matrix.getImageCollection().getNames().indexOf(title);
    }

    /**
     * Retrieves an array indicating which images the user has marked as
     * relevant.
     *
     * @return A boolean array where each index represents the selection status
     *         of the corresponding image checkbox. True indicates the image is
     *         marked as relevant, while false indicates otherwise.
     */
    private boolean[] getMarkedImages() {
        final var selectedImages = new boolean[imageIconCheckBox.length];
        IntStream.range(0, selectedImages.length).forEach(i -> selectedImages[i] = imageIconCheckBox[i].isSelected());
        return selectedImages;
    }

    /**
     * Clear relevance checkbox and sets the visibility of the checkboxes used
     * by the user for relevance feedback to not visible.
     */
    private void resetRelevanceCheckBox() {
        relevanceCheckBox.setSelected(false);
        for (var checkBox : imageIconCheckBox) {
            checkBox.setSelected(false);
            checkBox.setVisible(false);
        }
        relevanceCheckBox.updateUI();
    }

    /**
     * Helper method that sorts the images based on the given distance matrix.
     * The distance matrix contains an array of values that maps the distance
     * between two images at their given index.
     *
     * @param distanceMatrix - Distance matrix used to sort the images.
     */
    private void sortImages(final Double[][] distanceMatrix) {
        final var index = getSelectedImageNumber();
        if (index == -1) return;

        Arrays.sort(imageIconOrder, (a, b) -> FeatureMatrix.compare(distanceMatrix, index, a, b));
        displayFirstPage();
    }

    /**
     * Opens a file chooser dialog to select a directory and sets the selected
     * directory's path to the provided text field.
     *
     * @param selection - The JTextField where the selected directory path will
     *                    be displayed.
     */
    private void openFileChooser(final JTextField selection) {
        final var fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            final var directory = fileChooser.getSelectedFile();
            selection.setText(directory.getAbsolutePath());
        }
    }

    /**
     * Processes the selected folder by creating an {@code ImageCollection}
     * object using the given directory and initializing the UI elements. If
     * The given directory is empty, or it is already selected, this method will
     * display a message to the user indicating the error.
     *
     * @param directory - The path of the directory to process.
     */
    private void processDirectory(final String directory) {
        // check if the directory is empty
        if (directory.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No folder selected.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // check if the directory is already open
        if (directory.equals(currentDirectory)) {
            JOptionPane.showMessageDialog(this, "Folder already processed.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // new directory - load all the images in the given directory
        final var images = new ImageCollection(directory);
        final var size   = images.getSize();

        // if images are found in the directory, load their feature matrix
        if (size == 0) {
            JOptionPane.showMessageDialog(this, "Failed to find any supported images in the given directory.", "Error", JOptionPane.ERROR_MESSAGE);
        } else {
            currentDirectory = directory;
            matrix           = new FeatureMatrix(images);

            loadImages();
            createSelectedImageView();
            createNavigationAndOptionButtons();
            displayFirstPage();

            // clean up memory from previous session
            System.gc();
        }
    }

    /**
     * Selects and displays the image with the given label in the selected image
     * view.
     *
     * @param label - The label of the image to be displayed.
     */
    private void updateSelectedImageView(final String label) {
        final var index = matrix.getImageCollection().getNames().indexOf(label);
        final var icon  = matrix.getImageCollection().getImages().get(index);
        selectedImageView.setIcon(new ImageIcon(icon));
        imageViewPanel.setBorder(BorderFactory.createTitledBorder(label));
    }

    /**
     * Utility class for creating various GUI elements used in the application.
     */
    private static class GuiFactory {
        private static JCheckBox createCheckBox() {
            final var checkBox = new JCheckBox();
            checkBox.setVisible(false);
            checkBox.setBackground(Color.WHITE);
            return checkBox;
        }

        private static JButton createButton(final AppGui gui, final Image image, final String label) {
            final var icon = image.getScaledInstance(140, 80, Image.SCALE_SMOOTH);
            final var button = new JButton(new ImageIcon(icon));
            button.addActionListener(e -> gui.updateSelectedImageView(label));
            button.setBackground(Color.WHITE);

            final var border = new TitledBorder(label);
            border.setTitlePosition(TitledBorder.BELOW_BOTTOM);
            button.setBorder(border);

            return button;
        }

        private static JLabel createButtonLabel(final JButton button, final JCheckBox checkBox) {
            final var buttonLabel = new JLabel();
            buttonLabel.setLayout(new GridBagLayout());

            final var constraints = new GridBagConstraints();
            constraints.gridx = 2;
            constraints.anchor = GridBagConstraints.SOUTHWEST;

            buttonLabel.add(button);
            buttonLabel.add(checkBox, constraints);
            return buttonLabel;
        }

        private static JButton[] createButtons(final String[] buttonNames) {
            final var buttons = new JButton[buttonNames.length];
            IntStream.range(0, buttonNames.length).forEach(i -> {
                buttons[i] = new JButton(buttonNames[i]);
                buttons[i].setBackground(BUTTON_COLOR);
                buttons[i].setFont(DEFAULT_FONT);
            });
            return buttons;
        }
    }
}
