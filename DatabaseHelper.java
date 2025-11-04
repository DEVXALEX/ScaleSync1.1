import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;

public class DatabaseHelper {

    private static final String DB_URL = "jdbc:sqlite:weight_tracker.db";

    // Establishes connection to the database
    private Connection connect() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(DB_URL);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return conn;
    }

    // Creates the table if it doesn't exist
    public void createNewTable() {
        String sql = "CREATE TABLE IF NOT EXISTS weights ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " entry_date TEXT NOT NULL UNIQUE," // Store date as text in YYYY-MM-DD format
                + " morning_weight REAL,"
                + " night_weight REAL"
                + ");";

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    // NEW METHOD (replaces old insertEntry)
    public String saveWeight(double weight, String timeOfDay) {
        String date = LocalDate.now().toString();
        String checkSql = "SELECT id FROM weights WHERE entry_date = ?";
        String insertSql;
        String updateSql;

        // Set the correct SQL query based on "morning" or "night"
        if (timeOfDay.equals("morning")) {
            insertSql = "INSERT INTO weights (entry_date, morning_weight) VALUES (?, ?)";
            updateSql = "UPDATE weights SET morning_weight = ? WHERE entry_date = ?";
        } else {
            insertSql = "INSERT INTO weights (entry_date, night_weight) VALUES (?, ?)";
            updateSql = "UPDATE weights SET night_weight = ? WHERE entry_date = ?";
        }

        try (Connection conn = this.connect()) {
            // First, check if an entry for today already exists
            PreparedStatement checkPstmt = conn.prepareStatement(checkSql);
            checkPstmt.setString(1, date);
            ResultSet rs = checkPstmt.executeQuery();

            if (rs.next()) { 
                // Entry exists, so UPDATE it
                PreparedStatement updatePstmt = conn.prepareStatement(updateSql);
                updatePstmt.setDouble(1, weight);
                updatePstmt.setString(2, date);
                updatePstmt.executeUpdate();
            } else { 
                // No entry, so INSERT a new one
                PreparedStatement insertPstmt = conn.prepareStatement(insertSql);
                insertPstmt.setString(1, date);
                insertPstmt.setDouble(2, weight);
                insertPstmt.executeUpdate();
            }
            return timeOfDay + " weight saved for " + date;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    // Helper function to get the first and last morning weights in a period
    private String getWeightChange(String startDate, String endDate) {
        String sql = "SELECT morning_weight FROM weights "
                   + "WHERE entry_date >= ? AND entry_date <= ? "
                   + "ORDER BY entry_date ASC";
        
        double firstWeight = 0;
        double lastWeight = 0;
        int count = 0;

        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                if (count == 0) {
                    firstWeight = rs.getDouble("morning_weight");
                }
                lastWeight = rs.getDouble("morning_weight");
                count++;
            }

        } catch (Exception e) {
            return "Error fetching data: " + e.getMessage();
        }

        if (count < 2) {
            return "Not enough data for this period.";
        }

        double change = lastWeight - firstWeight;
        return String.format("Period Change: %.2f kg (From %.2f to %.2f over %d entries)", 
                             change, firstWeight, lastWeight, count);
    }

    // --- Analysis Methods ---

    public String getDailyAnalysis() {
        String date = LocalDate.now().toString();
        String sql = "SELECT morning_weight, night_weight FROM weights WHERE entry_date = ?";
        
        try (Connection conn = this.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, date);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                double morning = rs.getDouble("morning_weight");
                double night = rs.getDouble("night_weight");
                double change = night - morning;
                return String.format("Today's Analysis (%s):\n", date)
                     + String.format("  Morning: %.2f kg\n", morning)
                     + String.format("  Night:   %.2f kg\n", night)
                     + String.format("  Day's Change: %.2f kg\n", change);
            } else {
                return "No entry found for today.";
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String getWeeklyAnalysis() {
        LocalDate today = LocalDate.now();
        String startDate = today.minusDays(6).toString(); // Last 7 days including today
        String endDate = today.toString();
        
        return "--- Weekly Analysis (Last 7 Days) ---\n" 
               + getWeightChange(startDate, endDate);
    }

    public String getMonthlyAnalysis() {
        LocalDate today = LocalDate.now();
        String startDate = today.minusDays(29).toString(); // Last 30 days including today
        String endDate = today.toString();

        return "--- Monthly Analysis (Last 30 Days) ---\n"
               + getWeightChange(startDate, endDate);
    }
    // --- New Method for Step 3 ---
public DefaultTableModel getAllEntries() {
    // 1. Define column names
    Vector<String> columnNames = new Vector<>();
    columnNames.add("Date");
    columnNames.add("Morning (kg)");
    columnNames.add("Night (kg)");

    // 2. Create empty data vector
    Vector<Vector<Object>> data = new Vector<>();

    // 3. SQL query to get all data
    String sql = "SELECT entry_date, morning_weight, night_weight FROM weights ORDER BY entry_date DESC";

    try (Connection conn = this.connect();
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery(sql)) {

        // 4. Loop through results and build data
        while (rs.next()) {
            Vector<Object> row = new Vector<>();
            row.add(rs.getString("entry_date"));
            row.add(rs.getDouble("morning_weight"));
            row.add(rs.getDouble("night_weight"));
            data.add(row);
        }

    } catch (Exception e) {
        System.out.println(e.getMessage());
    }

    // 5. Return a new table model with the data
    return new DefaultTableModel(data, columnNames);
}
// --- New Method for Reset ---
    public String deleteAllEntries() {
        String sqlDelete = "DELETE FROM weights";
        String sqlVacuum = "VACUUM"; // Resets the auto-incrementing ID

        try (Connection conn = this.connect();
             Statement stmt = conn.createStatement()) {
            
            // Execute the delete operation
            stmt.execute(sqlDelete);
            
            // Execute the vacuum operation (cleans up the file)
            stmt.execute(sqlVacuum);
            
            return "All entries have been deleted. Database is reset.";
        } catch (Exception e) {
            return "Error while resetting database: " + e.getMessage();
        }
    }
}