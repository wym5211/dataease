import java.sql.*;

public class CheckAllMenu {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false";
        try (Connection c = DriverManager.getConnection(url, "root", "123456");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id, pid, name, path, component FROM core_menu ORDER BY id")) {
            System.out.println("All menus:");
            while (r.next()) {
                System.out.printf("ID:%d PID:%d Name:%s Path:%s%n",
                    r.getInt("id"),
                    r.getInt("pid"),
                    r.getString("name"),
                    r.getString("path"));
            }
        }
    }
}
