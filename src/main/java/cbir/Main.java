package cbir;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final var app = new AppGui();
            app.setVisible(true);
        });
    }
}
