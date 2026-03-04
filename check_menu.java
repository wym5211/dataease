import java.sql.*;

public class check_menu {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dataease10?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String password = "123456";

        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            // 检查权限管理菜单
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, path, component FROM core_menu WHERE id >= 100 ORDER BY id");

            System.out.println("权限管理菜单数据:");
            boolean hasData = false;
            while (rs.next()) {
                hasData = true;
                System.out.println("ID: " + rs.getLong("id") +
                    ", Name: " + rs.getString("name") +
                    ", Path: " + rs.getString("path") +
                    ", Component: " + rs.getString("component"));
            }

            if (!hasData) {
                System.out.println("没有找到权限管理菜单数据 (ID >= 100)，需要插入数据...");

                // 插入权限管理菜单
                String[] inserts = {
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (100, 0, 1, 'permissions', NULL, 10, 'icon_permissions', '/permissions', 0, 1, 0)",
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (101, 100, 2, 'permissions-menu', 'permissions/menu', 1, NULL, 'menu', 0, 1, 1)",
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (102, 100, 2, 'permissions-user', 'permissions/user', 2, NULL, 'user', 0, 1, 1)",
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (103, 100, 2, 'permissions-role', 'permissions/role', 3, NULL, 'role', 0, 1, 1)",
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (104, 100, 2, 'permissions-menu-auth', 'permissions/menu', 4, NULL, 'menu-auth', 0, 1, 1)",
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (105, 100, 2, 'permissions-resource', 'permissions/resource', 5, NULL, 'resource', 0, 1, 1)",
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (106, 100, 2, 'permissions-templates', 'permissions/template', 6, NULL, 'templates', 0, 1, 1)",
                    "INSERT INTO core_menu (id, pid, type, name, component, menu_sort, icon, path, hidden, in_layout, auth) VALUES (107, 100, 2, 'permissions-audit', 'permissions/audit', 7, NULL, 'audit', 0, 1, 1)"
                };

                for (String sql : inserts) {
                    try {
                        stmt.executeUpdate(sql);
                        System.out.println("执行: " + sql.substring(0, 80) + "...");
                    } catch (SQLException e) {
                        System.out.println("跳过 (可能已存在): " + e.getMessage());
                    }
                }

                // 插入角色菜单关联
                String[] roleMenus = {
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 100)",
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 101)",
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 102)",
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 103)",
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 104)",
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 105)",
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 106)",
                    "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, 107)"
                };

                for (String sql : roleMenus) {
                    try {
                        stmt.executeUpdate(sql);
                        System.out.println("执行: " + sql);
                    } catch (SQLException e) {
                        System.out.println("跳过 (可能已存在): " + e.getMessage());
                    }
                }

                System.out.println("\n数据插入完成，请刷新页面测试菜单功能。");
            } else {
                System.out.println("\n权限管理菜单数据已存在。");
            }
        }
    }
}
