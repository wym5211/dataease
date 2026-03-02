package io.dataease.core.permissions.role;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.api.permissions.role.api.RoleApi;
import io.dataease.api.permissions.role.dto.*;
import io.dataease.api.permissions.role.vo.ExternalUserVO;
import io.dataease.api.permissions.role.vo.RoleDetailVO;
import io.dataease.api.permissions.role.vo.RoleVO;
import io.dataease.exception.DEException;
import io.dataease.model.KeywordRequest;
import io.dataease.system.dao.auto.entity.SysRole;
import io.dataease.system.dao.auto.entity.SysRoleMenu;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysRoleMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMenuMapper;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.utils.AuthUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service("roleServer")
@Primary
@RestController
@RequestMapping("/role")
public class CoreRoleServer implements RoleApi {

    @Autowired
    private SysRoleMapper sysRoleMapper;
    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;
    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public List<RoleVO> query(KeywordRequest request) {
        QueryWrapper<SysRole> qw = new QueryWrapper<>();
        if (request != null && StringUtils.isNotBlank(request.getKeyword())) {
            qw.like("name", request.getKeyword());
        }
        qw.orderByAsc("id");
        return sysRoleMapper.selectList(qw).stream().map(this::toRoleVO).toList();
    }

    @Override
    @Transactional
    public Long create(RoleCreator creator) {
        SysRole role = new SysRole();
        role.setName(creator.getName());
        role.setRoleAlias(creator.getName());
        role.setType(creator.getTypeCode());
        role.setDescription(creator.getDesc());
        role.setStatus(creator.getStatus() != null ? creator.getStatus() : 1); // 默认启用
        role.setCreateTime(System.currentTimeMillis());
        sysRoleMapper.insert(role);
        return role.getId();
    }

    @Override
    public void edit(RoleEditor editor) {
        SysRole role = sysRoleMapper.selectById(editor.getId());
        if (role == null) {
            DEException.throwException("角色不存在");
        }
        if (role.getType() != null && role.getType() == 0 && !AuthUtils.isSysAdmin()) {
            DEException.throwException("系统角色不可编辑");
        }
        role.setName(editor.getName());
        role.setRoleAlias(editor.getName());
        role.setDescription(editor.getDesc());
        if (editor.getStatus() != null) {
            role.setStatus(editor.getStatus());
        }
        sysRoleMapper.updateById(role);
    }

    @Override
    @Transactional
    public void mountUser(MountUserRequest request) {
        if (request == null || request.getRid() == null || CollectionUtils.isEmpty(request.getUids())) {
            return;
        }
        Long rid = request.getRid();
        Set<Long> existing = selectedUserIds(rid);
        for (Long uid : request.getUids()) {
            if (uid == null || existing.contains(uid)) {
                continue;
            }
            SysUserRole ur = new SysUserRole();
            ur.setRoleId(rid);
            ur.setUserId(uid);
            sysUserRoleMapper.insert(ur);
        }
    }

    @Override
    @Transactional
    public void mountExternalUser(MountExternalUserRequest request) {
        if (request == null || request.getRid() == null || request.getUid() == null) {
            return;
        }
        MountUserRequest r = new MountUserRequest();
        r.setRid(request.getRid());
        r.setUids(List.of(request.getUid()));
        mountUser(r);
    }

    @Override
    public ExternalUserVO searchExternalUser(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return null;
        }
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.like("username", keyword).or().like("nick_name", keyword).or().like("email", keyword);
        qw.last("limit 1");
        SysUser user = sysUserMapper.selectOne(qw);
        if (user == null) {
            return null;
        }
        ExternalUserVO vo = new ExternalUserVO();
        vo.setUid(user.getId());
        vo.setAccount(user.getUsername());
        vo.setName(user.getNickName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        return vo;
    }

    @Override
    @Transactional
    public void unMountUser(UnmountUserRequest request) {
        if (request == null || request.getRid() == null || request.getUid() == null) {
            return;
        }
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("role_id", request.getRid()).eq("user_id", request.getUid());
        sysUserRoleMapper.delete(qw);
    }

    @Override
    public List<RoleVO> optionForUser(RoleRequest request) {
        List<SysRole> roles = sysRoleMapper.selectList(new QueryWrapper<SysRole>().orderByAsc("id"));
        if (request == null || request.getUid() == null) {
            return roles.stream().map(this::toRoleVO).toList();
        }
        Set<Long> selected = selectedRoleIds(request.getUid());
        return roles.stream().filter(r -> !selected.contains(r.getId())).map(this::toRoleVO).toList();
    }

    @Override
    public List<RoleVO> selectedForUser(RoleRequest request) {
        if (request == null || request.getUid() == null) {
            return Collections.emptyList();
        }
        Set<Long> selected = selectedRoleIds(request.getUid());
        if (selected.isEmpty()) {
            return Collections.emptyList();
        }
        QueryWrapper<SysRole> qw = new QueryWrapper<>();
        qw.in("id", selected);
        qw.orderByAsc("id");
        return sysRoleMapper.selectList(qw).stream().map(this::toRoleVO).toList();
    }

    @Override
    public RoleDetailVO detail(Long rid) {
        SysRole role = sysRoleMapper.selectById(rid);
        if (role == null) {
            return null;
        }
        RoleDetailVO vo = new RoleDetailVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setTypeCode(role.getType());
        vo.setDesc(role.getDescription());
        vo.setStatus(role.getStatus() != null ? role.getStatus() : 1);
        vo.setRid(role.getId());
        return vo;
    }

    @Override
    @Transactional
    public void delete(Long rid) {
        SysRole role = sysRoleMapper.selectById(rid);
        if (role == null) {
            return;
        }
        if (role.getType() != null && role.getType() == 0) {
            DEException.throwException("系统角色不可删除");
        }
        sysRoleMapper.deleteById(rid);
        sysUserRoleMapper.delete(new QueryWrapper<SysUserRole>().eq("role_id", rid));
        sysRoleMenuMapper.delete(new QueryWrapper<SysRoleMenu>().eq("role_id", rid));
    }

    @Override
    public Integer beforeUnmountInfo(UnmountUserRequest request) {
        return 0;
    }

    @Override
    public void copy(RoleCopyRequest request) {
        DEException.throwException("暂不支持");
    }

    @Override
    public List<RoleVO> byCurOrg(KeywordRequest request) {
        return query(request);
    }

    @Override
    public List<RoleVO> queryWithOid(Long oid) {
        return query(new KeywordRequest());
    }

    private RoleVO toRoleVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setCode(role.getRoleAlias()); // 角色编码使用 role_alias
        vo.setDescription(role.getDescription());
        vo.setCreateTime(role.getCreateTime());
        vo.setStatus(role.getStatus() != null ? role.getStatus() : 1); // 使用数据库中的 status
        vo.setReadonly(role.getType() != null && role.getType() == 0);
        vo.setRoot(role.getId() != null && role.getId() == 1L);
        return vo;
    }

    private Set<Long> selectedRoleIds(Long uid) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("user_id", uid);
        return sysUserRoleMapper.selectList(qw).stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
    }

    private Set<Long> selectedUserIds(Long rid) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("role_id", rid);
        return sysUserRoleMapper.selectList(qw).stream().map(SysUserRole::getUserId).collect(Collectors.toSet());
    }
}

