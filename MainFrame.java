import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class MainFrame {
    private JPanel mainframe;                   // Hauptpanel (Form)
    private JComboBox<String> kategorieComboBox;  // Hier wählen wir "Einnahme" oder "Ausgabe"
    private JTextField betragTextField;           // Betrag
    private JLabel betraglabel;
    private JLabel datenLabel;
    private JLabel kategorieLabel;
    private JLabel beschreibungsLabel;
    private JTextField datenTextField;            // Datum (Format: yyyy-MM-dd)
    private JTextField beschreibungTextField;     // Beschreibung
    private JButton speichernButton;              // "Speichern"-Button
    private JTable einnahmenAusgabenTable;        // Tabelle zur Anzeige der Transaktionen
    private JTextArea gegnueberstellungTextArea;   // TextArea für die Gegenüberstellung
    private JLabel gegenueberstellungLabel;

    /**
     * Konstruktor – hier wird der GUI-Designer-Code (z.B. $$$setupUI$$$) aufgerufen,
     * danach füllen wir die ComboBox mit den beiden Optionen und laden die Transaktionen.
     */
    public MainFrame() {
        // GUI-Designer-Code wird automatisch aufgerufen

        // ComboBox mit den zwei Optionen füllen
        kategorieComboBox.addItem("Einnahme");
        kategorieComboBox.addItem("Ausgabe");

        // Transaktionen laden (Tabelle & Gegenüberstellung)
        loadTransactions();

        // ActionListener für "Speichern"-Button
        speichernButton.addActionListener(e -> saveTransaction());
    }

    /**
     * Gibt das Hauptpanel zurück, damit es im JFrame verwendet werden kann.
     */
    public JPanel getMainPanel() {
        return mainframe;
    }

    /**
     * Lädt alle Transaktionen aus der Tabelle 'transaction'
     * und füllt die JTable. Anschließend wird die Gegenüberstellung aktualisiert.
     */
    private void loadTransactions() {
        String sql = "SELECT transaction_id, transaction_type, amount, transaction_date, description " +
                "FROM transaction " +
                "ORDER BY transaction_date DESC";

        try (Connection conn = Main.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            String[] columnNames = {"ID", "Typ", "Betrag", "Datum", "Beschreibung"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0);

            while (rs.next()) {
                int transactionId = rs.getInt("transaction_id");
                String transactionType = rs.getString("transaction_type");
                double amount = rs.getDouble("amount");
                Date date = rs.getDate("transaction_date");
                String description = rs.getString("description");

                model.addRow(new Object[]{
                        transactionId,
                        transactionType,
                        amount,
                        date,
                        description
                });
            }
            einnahmenAusgabenTable.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainframe, "Fehler beim Laden der Transaktionen: " + e.getMessage());
        }

        // Anschließend Gegenüberstellung aktualisieren
        updateGegenueberstellung();
    }

    /**
     * Liest die Eingaben (Typ, Betrag, Datum, Beschreibung) aus und fügt einen neuen Datensatz in die DB ein.
     */
    private void saveTransaction() {
        // Typ (Einnahme/Ausgabe) aus der ComboBox lesen
        String selectedType = (String) kategorieComboBox.getSelectedItem();
        if (selectedType == null) {
            JOptionPane.showMessageDialog(mainframe, "Bitte Typ (Einnahme oder Ausgabe) auswählen!");
            return;
        }

        // Betrag einlesen und in double konvertieren
        double amount;
        try {
            amount = Double.parseDouble(betragTextField.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(mainframe, "Bitte einen gültigen Betrag eingeben!");
            return;
        }

        // Datum parsen (Format yyyy-MM-dd)
        String dateStr = datenTextField.getText().trim();
        if (dateStr.isEmpty()) {
            JOptionPane.showMessageDialog(mainframe, "Bitte ein Datum eingeben (Format yyyy-MM-dd)!");
            return;
        }
        java.sql.Date sqlDate;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date parsed = sdf.parse(dateStr);
            // Check: Datum darf nicht in der Zukunft liegen
            Date today = sdf.parse(sdf.format(new Date()));  // aktuelles Datum ohne Uhrzeit
            if (parsed.after(today)) {
                JOptionPane.showMessageDialog(mainframe, "Eintrag für zukünftige Daten sind nicht erlaubt!");
                return;
            }
            sqlDate = new java.sql.Date(parsed.getTime());
        } catch (ParseException e) {
            JOptionPane.showMessageDialog(mainframe, "Datum muss im Format yyyy-MM-dd sein!");
            return;
        }

        // Beschreibung wird eingelesen
        String description = beschreibungTextField.getText().trim();

        String insertSQL = "INSERT INTO transaction (transaction_type, amount, transaction_date, description) " +
                "VALUES (?, ?, ?, ?)";
        try (Connection conn = Main.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setString(1, selectedType);
            pstmt.setDouble(2, amount);
            pstmt.setDate(3, sqlDate);
            pstmt.setString(4, description);

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(mainframe, "Transaktion gespeichert!");

            // Tabelle und Gegenüberstellung werden aktualisiert
            loadTransactions();

            // Eingabefelder leeren
            betragTextField.setText("");
            datenTextField.setText("");
            beschreibungTextField.setText("");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainframe, "Fehler beim Speichern: " + e.getMessage());
        }
    }

    /**
     * Aktualisiert die Gegenüberstellung aller Transaktionen in der TextArea.
     * Es werden Einnahmen und Ausgaben getrennt aufgeführt sowie die Gesamtsummen und der Kontostand berechnet.
     */
    private void updateGegenueberstellung() {
        String sql = "SELECT transaction_type, amount, transaction_date, description " +
                "FROM transaction " +
                "ORDER BY transaction_type, transaction_date";

        double totalEinnahmen = 0.0;
        double totalAusgaben = 0.0;
        List<String> einnahmenList = new ArrayList<>();
        List<String> ausgabenList = new ArrayList<>();

        try (Connection conn = Main.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String type = rs.getString("transaction_type"); // "Einnahme" oder "Ausgabe"
                double amount = rs.getDouble("amount");
                Date date = rs.getDate("transaction_date");
                String desc = rs.getString("description");

                if ("Einnahme".equalsIgnoreCase(type)) {
                    einnahmenList.add("Einnahme: " + amount + " € am " + date + " (" + desc + ")");
                    totalEinnahmen += amount;
                } else if ("Ausgabe".equalsIgnoreCase(type)) {
                    ausgabenList.add("Ausgabe: " + amount + " € am " + date + " (" + desc + ")");
                    totalAusgaben += amount;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(mainframe, "Fehler beim Laden der Gegenüberstellung: " + e.getMessage());
        }

        double saldo = totalEinnahmen - totalAusgaben;
        StringBuilder sb = new StringBuilder();
        sb.append("Einnahmen:\n");
        for (String einnahme : einnahmenList) {
            sb.append("  - ").append(einnahme).append("\n");
        }
        sb.append("Summe Einnahmen: ").append(totalEinnahmen).append(" €\n\n");

        sb.append("Ausgaben:\n");
        for (String ausgabe : ausgabenList) {
            sb.append("  - ").append(ausgabe).append("\n");
        }
        sb.append("Summe Ausgaben: ").append(totalAusgaben).append(" €\n\n");

        sb.append("Aktueller Kontostand: ").append(saldo).append(" €\n");

        gegnueberstellungTextArea.setText(sb.toString());
    }
}
