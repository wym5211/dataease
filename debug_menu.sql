-- 调试菜单4、5、6的数据
SELECT
    id,
    pid,
    type,
    name,
    path,
    hidden,
    in_layout,
    auth
FROM core_menu
WHERE id IN (4, 5, 6)
ORDER BY id;
