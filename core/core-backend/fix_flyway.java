import java.sql.*;

public class fix_flyway {
    public static void main(String[] args) throws Exception {
        // Load MySQL driver explicitly
        Class.forName("com.mysql.cj.jdbc.Driver");

        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // Delete failed migration record
            String sql = "DELETE FROM de_standalone_version WHERE version = '2.11.17' AND success = 0";
            try (Statement stmt = conn.createStatement()) {
                int rows = stmt.executeUpdate(sql);
                System.out.println("Deleted " + rows + " failed migration records");
            }
            
            // Insert users if not exist
            sql = "DELETE FROM sys_user_role WHERE user_id IN (1, 2)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
            
            sql = "DELETE FROM sys_user WHERE id IN (1, 2)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
            }
            
            sql = "INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time) VALUES (1, 'admin', 'DataEase@123456', '管理员', 'admin@dataease.io', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Inserted admin user");
            }
            
            sql = "INSERT INTO sys_user (id, username, password, nick_name, email, dept_id, status, create_time, update_time) VALUES (2, '999', 'DataEase@123456', '测试用户', '999@test.com', 1, 1, UNIX_TIMESTAMP() * 1000, UNIX_TIMESTAMP() * 1000)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Inserted 999 user");
            }
            
            sql = "INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("Inserted user roles");
            }
            
            System.out.println("Fix completed successfully!");
        }
    }
}
