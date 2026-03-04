import java.sql.*;

public class fix_flyway {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
        String user = "root";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();

            int rows = stmt.executeUpdate(
                "DELETE FROM flyway_schema_history WHERE version='2.11.9' AND success=0"
            );
            System.out.println("Deleted failed migration record: " + rows);

            ResultSet rs = stmt.executeQuery(
                "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank"
            );
            System.out.println("Current Flyway history:");
            while (rs.next()) {
                System.out.printf("Version: %s, Success: %b, Description: %s%n",
                    rs.getString("version"),
                    rs.getBoolean("success"),
                    rs.getString("description"));
            }
        }
    }
}
