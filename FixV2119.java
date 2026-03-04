import java.sql.*;

public class FixV2119 {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false";
        try (Connection c = DriverManager.getConnection(url, "root", "123456");
             Statement s = c.createStatement()) {
            // Delete failed V2.11.9 record
            int rows = s.executeUpdate("DELETE FROM de_standalone_version WHERE version='2.11.9'");
            System.out.println("Deleted V2.11.9 records: " + rows);
        }
    }
}
