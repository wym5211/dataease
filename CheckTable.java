import java.sql.*;

public class CheckTable {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false";
        try (Connection c = DriverManager.getConnection(url, "root", "123456");
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT * FROM de_standalone_version")) {
            ResultSetMetaData m = r.getMetaData();
            int cols = m.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                System.out.print(m.getColumnName(i) + "\t");
            }
            System.out.println();
            while (r.next()) {
                for (int i = 1; i <= cols; i++) {
                    System.out.print(r.getObject(i) + "\t");
                }
                System.out.println();
            }
        }
    }
}
