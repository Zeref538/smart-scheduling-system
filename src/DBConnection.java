import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class DBConnection {
    private static Connection connection = null;

    // MySQL connection parameters - update these to match your environment
    private static final String URL = "jdbc:mysql://localhost:3306/scheduling_system";
    private static final String USER = "root"; // Change to your MySQL username
    private static final String PASSWORD = "529438167"; // Change to your MySQL password

    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        if (connection == null || connection.isClosed()) {
            try {
                // Load MySQL JDBC driver
                Class.forName("com.mysql.cj.jdbc.Driver");

                // Create connection
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Database connection established successfully");
                
                // Test connection
                if (!connection.isValid(5)) { // 5 second timeout
                    throw new SQLException("Database connection test failed");
                }
            } catch (SQLException e) {
                String errorMessage = "Failed to connect to database: " + e.getMessage() + 
                    "\nPlease verify these settings in DBConnection.java:" +
                    "\n- Database URL: " + URL +
                    "\n- Username: " + USER +
                    "\n- Password: [hidden]" +
                    "\n\nMake sure your MySQL server is running and these credentials are correct.";
                
                JOptionPane.showMessageDialog(null, errorMessage, 
                    "Database Connection Error", JOptionPane.ERROR_MESSAGE);
                throw e;
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        } finally {
            connection = null; // Ensure connection is nullified even if close fails
        }
    }
}
