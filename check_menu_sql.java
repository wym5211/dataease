import java.sql.*;
import java.util.*;

public class check_menu_sql {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
        String user = "root";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Statement stmt = conn.createStatement();

            // 查询所有权限相关的菜单
            System.out.println("=== 所有权限相关菜单 ===");
            ResultSet rs = stmt.executeQuery(
                "SELECT id, pid, name, path, component, menu_sort, hidden, auth FROM core_menu " +
                "WHERE name LIKE '%permission%' OR path LIKE '%permission%' OR id >= 100 ORDER BY id"
            );

            while (rs.next()) {
                System.out.printf("ID:%d PID:%d Name:%s Path:%s Component:%s Sort:%d Hidden:%d Auth:%d%n",
                    rs.getInt("id"),
                    rs.getInt("pid"),
                    rs.getString("name"),
                    rs.getString("path"),
                    rs.getString("component"),
                    rs.getInt("menu_sort"),
                    rs.getInt("hidden"),
                    rs.getInt("auth")
                );
            }

            // 查询所有菜单按path分组，看是否有重复path
            System.out.println("\n=== 检查重复path ===");
            rs = stmt.executeQuery(
                "SELECT path, COUNT(*) as cnt, GROUP_CONCAT(id) as ids " +
                "FROM core_menu GROUP BY path HAVING cnt > 1"
            );
            while (rs.next()) {
                System.out.printf("Path:%s Count:%d IDs:%s%n",
                    rs.getString("path"),
                    rs.getInt("cnt"),
                    rs.getString("ids")
                );
            }

            // 查询name包含permissions的菜单
            System.out.println("\n=== name包含permissions的菜单 ===");
            rs = stmt.executeQuery(
                "SELECT id, pid, name, path, component FROM core_menu " +
                "WHERE name LIKE '%permissions%' ORDER BY id"
            );
            while (rs.next()) {
                System.out.printf("ID:%d PID:%d Name:%s Path:%s Component:%s%n",
                    rs.getInt("id"),
                    rs.getInt("pid"),
                    rs.getString("name"),
                    rs.getString("path"),
                    rs.getString("component")
                );
            }
        }
    }
}
