import java.sql.*;

public class CheckUsers {
    public static void main(String[] args) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:mysql://localhost:3306/dataease10?useSSL=false", "root", "123456");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id, username, password, status FROM sys_user")) {
            System.out.println("Users in sys_user table:");
            while (r.next()) {
                String pwd = r.getString("password");
                String pwdPreview = pwd != null && pwd.length() > 30 ? pwd.substring(0, 30) + "..." : pwd;
                System.out.println("ID: " + r.getLong("id") + " | Username: " + r.getString("username") + " | Status: " + r.getInt("status") + " | Pwd: " + pwdPreview);
            }
        }
    }
}
