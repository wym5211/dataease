# 重置数据库指南

## 问题
登录失败，错误：login.validator.pwd

## 解决方案：重置数据库

### 方法一：删除数据库文件（H2 Desktop模式）

1. 停止后端服务
2. 删除以下文件：
   - `core/core-backend/data/desktop.mv.db`
   - `core/core-backend/data/desktop.lock.db`
3. 重新启动后端
4. CoreAuthInitializer会自动创建admin用户

### 方法二：通过H2控制台重置密码

1. 访问 H2 控制台：`http://localhost:8102`（如果启用）
2. 连接到数据库
3. 执行SQL：
```sql
DELETE FROM sys_user WHERE username = 'admin';
```
4. 重启后端，会自动创建新的admin用户

### 方法三：使用脚本重置密码

创建一个临时的reset脚本：

```java
@Component
public class ResetPassword {
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public void run(String... args) {
        SysUser user = sysUserMapper.selectOne(
            new QueryWrapper<SysUser>().eq("username", "admin")
        );
        if (user != null) {
            user.setPassword(new BCryptPasswordEncoder().encode("DataEase@123456"));
            sysUserMapper.updateById(user);
            System.out.println("Password reset successfully!");
        }
    }
}
```

## 当前服务状态

- ✅ 后端运行中：http://localhost:8100
- ✅ 前端运行中：http://localhost:8091
- ⚠️ 数据库可能有旧数据导致密码不匹配

## 推荐操作

1. 访问 http://localhost:8091
2. 尝试登录
3. 如果失败，停止后端，删除 `core/core-backend/data/` 下的数据库文件
4. 重新启动后端
5. 再次尝试登录
