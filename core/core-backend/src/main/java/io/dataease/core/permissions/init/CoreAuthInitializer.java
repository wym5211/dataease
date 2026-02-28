package io.dataease.core.permissions.init;

import io.dataease.system.dao.auto.entity.SysOrg;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.mapper.SysOrgMapper;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
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
    }
}