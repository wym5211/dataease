import java.sql.*;

public class CheckMenu {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false";
        try (Connection c = DriverManager.getConnection(url, "root", "123456");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id, pid, name, path, component FROM core_menu WHERE path LIKE '%permission%' OR name LIKE '%permission%' ORDER BY id")) {
            System.out.println("Permission related menus:");
            while (r.next()) {
                System.out.printf("ID:%d PID:%d Name:%s Path:%s Component:%s%n",
                    r.getInt("id"),
                    r.getInt("pid"),
                    r.getString("name"),
                    r.getString("path"),
                    r.getString("component"));
            }
        }
    }
}
