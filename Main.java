import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {
    private static final String URL = "jdbc:mysql://localhost:3306/haushaltsbuch";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    /**
     * Stellt eine Verbindung zur Datenbank her.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        // Das GUI wird im Event-Dispatch-Thread gestartet
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Buchhaltung - App");
            MainFrame mainFrame = new MainFrame();
            frame.setContentPane(mainFrame.getMainPanel());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack(); // Fenstergröße an den Inhalt anpassen
            frame.setLocationRelativeTo(null); // Fenster zentrieren
            frame.setVisible(true);
        });
    }
}
