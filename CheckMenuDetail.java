import java.sql.*;

public class CheckMenuDetail {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false";
        try (Connection c = DriverManager.getConnection(url, "root", "123456");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT id, pid, name, path, hidden, type FROM core_menu WHERE hidden = 0 ORDER BY id")) {
            System.out.println("All visible menus:");
            while (r.next()) {
                System.out.printf("ID:%d PID:%d Type:%d Hidden:%d Name:%s Path:%s%n",
                    r.getInt("id"),
                    r.getInt("pid"),
                    r.getInt("type"),
                    r.getInt("hidden"),
                    r.getString("name"),
                    r.getString("path"));
            }
        }
    }
}
