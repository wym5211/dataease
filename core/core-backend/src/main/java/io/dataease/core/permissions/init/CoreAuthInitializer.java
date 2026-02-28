package io.dataease.core.permissions.init;

import io.dataease.menu.dao.auto.entity.CoreMenu;
import io.dataease.menu.dao.auto.mapper.CoreMenuMapper;
import io.dataease.system.dao.auto.entity.SysOrg;
import io.dataease.system.dao.auto.entity.SysRole;
import io.dataease.system.dao.auto.entity.SysRoleMenu;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysOrgMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMenuMapper;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CoreAuthInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper sysUserMapper;
    
    @Autowired
    private SysOrgMapper sysOrgMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private CoreMenuMapper coreMenuMapper;

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (sysOrgMapper.selectCount(null) == 0) {
            SysOrg org = new SysOrg();
            org.setName("DataEase");
            org.setPid(0L);
            org.setSort(1);
            org.setCreateTime(System.currentTimeMillis());
            sysOrgMapper.insert(org);
        }
        
        if (sysUserMapper.selectCount(null) == 0) {
            SysOrg rootOrg = sysOrgMapper.selectList(null).get(0);
            
            SysUser user = new SysUser();
            user.setUsername("admin");
            user.setNickName("管理员");
            user.setPassword(new BCryptPasswordEncoder().encode("DataEase@123456"));
            user.setStatus(1); // Enable
            user.setDeptId(rootOrg.getId());
            user.setCreateTime(System.currentTimeMillis());
            sysUserMapper.insert(user);
        }

        if (sysRoleMapper.selectCount(null) == 0) {
            SysRole adminRole = new SysRole();
            adminRole.setName("管理员");
            adminRole.setRoleAlias("admin");
            adminRole.setType(0);
            adminRole.setDescription("系统管理员");
            adminRole.setCreateTime(System.currentTimeMillis());
            sysRoleMapper.insert(adminRole);
        }

        SysUser admin = sysUserMapper.selectById(1L);
        SysRole role = sysRoleMapper.selectById(1L);
        if (admin != null && role != null) {
            if (sysUserRoleMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserRole>()
                    .eq("user_id", admin.getId()).eq("role_id", role.getId())) == 0) {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(admin.getId());
                ur.setRoleId(role.getId());
                sysUserRoleMapper.insert(ur);
            }
            if (sysRoleMenuMapper.selectCount(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysRoleMenu>()
                    .eq("role_id", role.getId())) == 0) {
                for (CoreMenu menu : coreMenuMapper.selectList(null)) {
                    if (menu.getAuth() != null && menu.getAuth()) {
                        SysRoleMenu rm = new SysRoleMenu();
                        rm.setRoleId(role.getId());
                        rm.setMenuId(menu.getId());
                        sysRoleMenuMapper.insert(rm);
                    }
                }
            }
        }
    }
}
