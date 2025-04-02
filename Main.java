import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Main {

    private static final String URL = "jdbc:mysql://localhost:3306/haushaltsbuch";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // Anpassen, falls du ein Passwort hast

    // Stellt eine Verbindung zur Datenbank her
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // Testmethode zum Ausprobieren
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            if (conn != null) {
                System.out.println("Verbindung erfolgreich!");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
