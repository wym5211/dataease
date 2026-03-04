import java.sql.*;

public class FixFlyway {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
        String user = "root";
        String password = "123456";

        Class.forName("com.mysql.cj.jdbc.Driver");

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();

            // Check if using de_standalone_version table
            ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE '%version%'");
            String tableName = null;
            while (rs.next()) {
                tableName = rs.getString(1);
                System.out.println("Found version table: " + tableName);
            }

            if (tableName == null) {
                tableName = "flyway_schema_history";
            }

            // Delete failed V2.11.9 record
            String deleteSql = "DELETE FROM " + tableName + " WHERE version='2.11.9' AND success=0";
            int rows = stmt.executeUpdate(deleteSql);
            System.out.println("Deleted failed V2.11.9 records: " + rows);

            // Show current history
            rs = stmt.executeQuery("SELECT version, description, success FROM " + tableName + " ORDER BY installed_rank");
            System.out.println("\nCurrent Flyway history:");
            while (rs.next()) {
                System.out.printf("Version: %s, Success: %b, Description: %s%n",
                    rs.getString("version"),
                    rs.getBoolean("success"),
                    rs.getString("description"));
            }
        }
    }
}
