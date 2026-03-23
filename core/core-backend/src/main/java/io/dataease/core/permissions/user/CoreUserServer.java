package io.dataease.core.permissions.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.dataease.api.permissions.login.dto.MfaLoginDTO;
import io.dataease.api.permissions.login.vo.MfaQrVO;
import io.dataease.api.permissions.role.dto.UserRequest;
import io.dataease.api.permissions.user.api.UserApi;
import io.dataease.api.permissions.user.dto.*;
import io.dataease.api.permissions.user.vo.*;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.i18n.Lang;
import io.dataease.utils.CacheUtils;
import io.dataease.auth.vo.TokenVO;
import io.dataease.utils.AuthUtils;
import io.dataease.exception.DEException;
import io.dataease.model.KeywordRequest;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.entity.SysRole;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMapper;
import io.dataease.utils.BeanUtils;
import io.dataease.utils.IPUtils;
import io.dataease.utils.RsaUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service("userServer")
@Primary
@RestController
@RequestMapping("/user")
public class CoreUserServer implements UserApi {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public IPage<UserGridVO> pager(int goPage, int pageSize, UserGridRequest request) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();

        // 关键词搜索：用户名、昵称、邮箱
        if (request != null && StringUtils.isNotBlank(request.getKeyword())) {
            qw.and(w -> w.like("username", request.getKeyword())
                    .or().like("nick_name", request.getKeyword())
                    .or().like("email", request.getKeyword()));
        }

        // 状态筛选
        if (request != null && CollectionUtils.isNotEmpty(request.getStatusList())) {
            List<Integer> statuses = request.getStatusList().stream()
                    .map(enable -> enable ? 1 : 0)
                    .collect(Collectors.toList());
            qw.in("status", statuses);
        }

        // 角色筛选
        if (request != null && CollectionUtils.isNotEmpty(request.getRoleIdList())) {
            List<Long> userIds = getUserIdsByRoleIds(request.getRoleIdList());
            if (CollectionUtils.isEmpty(userIds)) {
                // 如果没有匹配的用户，返回空结果
                return new Page<>(goPage, pageSize);
            }
            qw.in("id", userIds);
        }

        // 排序
        if (request != null && Boolean.TRUE.equals(request.getTimeDesc())) {
            qw.orderByDesc("create_time");
        } else {
            qw.orderByAsc("id");
        }

        // 分页查询
        Page<SysUser> page = new Page<>(goPage, pageSize);
        IPage<SysUser> userPage = sysUserMapper.selectPage(page, qw);

        // 转换为VO
        return userPage.convert(this::toUserGridVO);
    }

    @Override
    public UserFormVO queryById(Long id) {
        SysUser sysUser = sysUserMapper.selectById(id);
        if (sysUser == null) {
            DEException.throwException("User not found");
        }
        UserFormVO vo = new UserFormVO();
        BeanUtils.copyBean(vo, sysUser);
        vo.setAccount(sysUser.getUsername());
        vo.setName(sysUser.getNickName());
        vo.setEnable(sysUser.getStatus() != null && sysUser.getStatus() == 1);
        return vo;
    }

    @Override
    public UserFormVO personInfo() {
        TokenUserBO tokenUser = AuthUtils.getUser();
        if (tokenUser == null) {
            DEException.throwException("Not logged in");
        }
        return queryById(tokenUser.getUserId());
    }

    @Override
    public UserGridVO personSysVariableInfo(Long id) {
        return null;
    }

    @Override
    public CurIpVO ipInfo() {
        CurIpVO vo = new CurIpVO();
        vo.setIp(IPUtils.get());
        TokenUserBO tokenUser = AuthUtils.getUser();
        if (tokenUser != null) {
            SysUser user = sysUserMapper.selectById(tokenUser.getUserId());
            if (user != null) {
                vo.setAccount(user.getUsername());
                vo.setName(user.getNickName());
            }
        }
        return vo;
    }

    @Override
    @Transactional
    public Long create(UserCreator creator) {
        // 验证用户名唯一性
        QueryWrapper<SysUser> usernameCheck = new QueryWrapper<>();
        usernameCheck.eq("username", creator.getAccount());
        if (sysUserMapper.selectCount(usernameCheck) > 0) {
            DEException.throwException("用户名已存在");
        }

        // 验证邮箱唯一性
        if (StringUtils.isNotBlank(creator.getEmail())) {
            QueryWrapper<SysUser> emailCheck = new QueryWrapper<>();
            emailCheck.eq("email", creator.getEmail());
            if (sysUserMapper.selectCount(emailCheck) > 0) {
                DEException.throwException("邮箱已被使用");
            }
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(creator.getAccount());
        user.setNickName(creator.getName());
        user.setEmail(creator.getEmail());
        user.setPhone(creator.getPhone());
        user.setStatus(creator.getEnable() != null && creator.getEnable() ? 1 : 0);
        // 使用传入的密码或默认密码
        String pwd = StringUtils.isNotBlank(creator.getPassword()) ? creator.getPassword() : "DataEase@123456";
        user.setPassword(passwordEncoder.encode(pwd));
        user.setCreateTime(System.currentTimeMillis());
        user.setDeptId(AuthUtils.getUser() != null ? AuthUtils.getUser().getDefaultOid() : null);

        sysUserMapper.insert(user);

        // 关联角色
        if (CollectionUtils.isNotEmpty(creator.getRoleIds())) {
            for (Long roleId : creator.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }

        return user.getId();
    }

    @Override
    public void createPlatform(PlatformUserCreator creator) {

    }

    @Override
    @Transactional
    public void edit(UserEditor editor) {
        if (editor == null || editor.getId() == null) {
            DEException.throwException("用户ID不能为空");
        }

        SysUser user = sysUserMapper.selectById(editor.getId());
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        // 验证邮箱唯一性（排除自己）
        if (StringUtils.isNotBlank(editor.getEmail())) {
            QueryWrapper<SysUser> emailCheck = new QueryWrapper<>();
            emailCheck.eq("email", editor.getEmail());
            emailCheck.ne("id", editor.getId());
            if (sysUserMapper.selectCount(emailCheck) > 0) {
                DEException.throwException("邮箱已被使用");
            }
        }

        // 更新基本信息
        user.setNickName(editor.getName());
        user.setEmail(editor.getEmail());
        user.setPhone(editor.getPhone());
        user.setStatus(editor.getEnable() != null && editor.getEnable() ? 1 : 0);
        user.setUpdateTime(System.currentTimeMillis());

        sysUserMapper.updateById(user);

        // 更新角色关联：先删除旧关联，再添加新关联
        QueryWrapper<SysUserRole> deleteQw = new QueryWrapper<>();
        deleteQw.eq("user_id", editor.getId());
        sysUserRoleMapper.delete(deleteQw);

        if (CollectionUtils.isNotEmpty(editor.getRoleIds())) {
            for (Long roleId : editor.getRoleIds()) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(editor.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }
    }

    @Override
    public void personEdit(UserEditor editor) {

    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            DEException.throwException("用户ID不能为空");
        }

        // 不能删除admin用户（ID=1）
        if (id.equals(1L)) {
            DEException.throwException("不能删除admin用户");
        }

        // 不能删除当前登录用户
        TokenUserBO currentUser = AuthUtils.getUser();
        if (currentUser != null && id.equals(currentUser.getUserId())) {
            DEException.throwException("不能删除当前登录用户");
        }

        // 删除用户角色关联
        QueryWrapper<SysUserRole> deleteQw = new QueryWrapper<>();
        deleteQw.eq("user_id", id);
        sysUserRoleMapper.delete(deleteQw);

        // 删除用户
        sysUserMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void batchDel(List<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }

        TokenUserBO currentUser = AuthUtils.getUser();
        if (currentUser != null) {
            // 不能删除当前登录用户
            if (ids.contains(currentUser.getUserId())) {
                DEException.throwException("不能删除当前登录用户");
            }
        }

        // 不能删除admin用户（ID=1）
        if (ids.contains(1L)) {
            DEException.throwException("不能删除admin用户");
        }

        // 删除用户角色关联
        QueryWrapper<SysUserRole> deleteQw = new QueryWrapper<>();
        deleteQw.in("user_id", ids);
        sysUserRoleMapper.delete(deleteQw);

        // 删除用户
        sysUserMapper.deleteBatchIds(ids);
    }

    @Override
    public List<UserItemVO> optionForRole(UserRequest request) {
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.eq("status", 1);

        // 如果指定了角色，返回该角色未绑定的用户
        if (request != null && request.getRid() != null) {
            Set<Long> userIds = getSelectedUserIds(request.getRid());
            if (CollectionUtils.isNotEmpty(userIds)) {
                qw.notIn("id", userIds);
            }
        }

        // 关键词搜索
        if (request != null && StringUtils.isNotBlank(request.getKeyword())) {
            qw.and(w -> w.like("username", request.getKeyword())
                    .or().like("nick_name", request.getKeyword())
                    .or().like("email", request.getKeyword()));
        }

        qw.orderByAsc("id");
        return sysUserMapper.selectList(qw).stream().map(this::toUserItemVO).toList();
    }

    @Override
    public List<UserItemVO> optionForOrg() {
        TokenUserBO tokenUser = AuthUtils.getUser();
        if (tokenUser == null) {
            return Collections.emptyList();
        }
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.eq("dept_id", tokenUser.getDefaultOid());
        qw.eq("status", 1);
        return sysUserMapper.selectList(qw).stream().map(u -> {
            UserItemVO vo = new UserItemVO();
            vo.setId(u.getId());
            vo.setAccount(u.getUsername());
            vo.setName(u.getNickName());
            vo.setEmail(u.getEmail());
            return vo;
        }).toList();
    }

    @Override
    public IPage<UserItemVO> selectedForRole(int goPage, int pageSize, UserRequest request) {
        if (request == null || request.getRid() == null) {
            return new Page<>(goPage, pageSize);
        }

        // 获取该角色已绑定的用户ID
        Set<Long> userIds = getSelectedUserIds(request.getRid());
        if (CollectionUtils.isEmpty(userIds)) {
            return new Page<>(goPage, pageSize);
        }

        // 查询用户
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.in("id", userIds);
        qw.eq("status", 1);

        // 关键词搜索
        if (StringUtils.isNotBlank(request.getKeyword())) {
            qw.and(w -> w.like("username", request.getKeyword())
                    .or().like("nick_name", request.getKeyword())
                    .or().like("email", request.getKeyword()));
        }

        qw.orderByAsc("id");

        // 分页查询
        Page<SysUser> page = new Page<>(goPage, pageSize);
        IPage<SysUser> userPage = sysUserMapper.selectPage(page, qw);

        // 转换为VO
        return userPage.convert(this::toUserItemVO);
    }

    @Override
    public TokenVO switchOrg(Long oId) {
        return null;
    }

    @Override
    public CurUserVO info() {
        TokenUserBO tokenUser = AuthUtils.getUser();
        if (tokenUser == null) {
             DEException.throwException("Not logged in");
        }
        SysUser sysUser = sysUserMapper.selectById(tokenUser.getUserId());
        CurUserVO vo = new CurUserVO();
        if (sysUser != null) {
            vo.setId(sysUser.getId());
            vo.setName(sysUser.getNickName());
            vo.setOid(tokenUser.getDefaultOid());
            Object langObj = CacheUtils.get(io.dataease.constant.CacheConstant.UserCacheConstant.USER_COMMUNITY_LANGUAGE, "de");
            if (ObjectUtils.isNotEmpty(langObj) && StringUtils.isNotBlank(langObj.toString())) {
                vo.setLanguage(langObj.toString());
            } else {
                vo.setLanguage("zh-CN");
            }
        }
        return vo;
    }

    @Override
    public List<UserItem> byCurOrg(KeywordRequest request) {
        TokenUserBO tokenUser = AuthUtils.getUser();
        if (tokenUser == null) {
            return Collections.emptyList();
        }
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        qw.eq("dept_id", tokenUser.getDefaultOid());
        if (request != null && StringUtils.isNotBlank(request.getKeyword())) {
            qw.and(w -> w.like("username", request.getKeyword()).or().like("nick_name", request.getKeyword()));
        }
        qw.eq("status", 1);
        qw.orderByAsc("id");
        return sysUserMapper.selectList(qw).stream().map(u -> {
            UserItem item = new UserItem();
            item.setId(u.getId());
            item.setName(u.getNickName());
            item.setAccount(u.getUsername());
            return item;
        }).toList();
    }

    @Override
    public int userCount() {
        return 0;
    }

    @Override
    public void switchLanguage(LangSwitchRequest request) {
        String lang = request.getLang();
        if (StringUtils.equalsIgnoreCase(Lang.zh_CN.getDesc(), lang)) {
            lang = Lang.zh_CN.getDesc();
        } else if (StringUtils.equalsAnyIgnoreCase(lang, "en", "tw")) {
            lang = lang.toLowerCase();
        } else {
            DEException.throwException("无效language");
        }
        CacheUtils.put(io.dataease.constant.CacheConstant.UserCacheConstant.USER_COMMUNITY_LANGUAGE, "de", lang);
    }

    @Override
    public void excelTemplate() {

    }

    @Override
    public UserImportVO batchImport(MultipartFile file) {
        return null;
    }

    @Override
    public void errorRecord(String key) {

    }

    @Override
    public void clearErrorRecord(String key) {

    }

    @Override
    public String defaultPwd() {
        return "DataEase@123456";
    }

    @Override
    public void resetPwd(Long id) {
        if (id == null) {
            DEException.throwException("用户ID不能为空");
        }

        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        // 重置为默认密码
        user.setPassword(passwordEncoder.encode("DataEase@123456"));
        user.setUpdateTime(System.currentTimeMillis());
        sysUserMapper.updateById(user);
    }

    @Override
    public void enable(EnableSwitchRequest request) {
        if (request == null || request.getId() == null) {
            DEException.throwException("用户ID不能为空");
        }

        SysUser user = sysUserMapper.selectById(request.getId());
        if (user == null) {
            DEException.throwException("用户不存在");
        }

        // 不能禁用当前登录用户
        TokenUserBO currentUser = AuthUtils.getUser();
        if (currentUser != null && request.getId().equals(currentUser.getUserId())) {
            DEException.throwException("不能禁用当前登录用户");
        }

        // 不能禁用admin用户
        if (request.getId().equals(1L)) {
            DEException.throwException("不能禁用admin用户");
        }

        user.setStatus(request.getEnable() != null && request.getEnable() ? 1 : 0);
        user.setUpdateTime(System.currentTimeMillis());
        sysUserMapper.updateById(user);
    }

    @Override
    public void modifyPwd(ModifyPwdRequest request) {
        TokenUserBO tokenUser = AuthUtils.getUser();
        SysUser user = sysUserMapper.selectById(tokenUser.getUserId());
        if (user == null) {
            DEException.throwException("User not found");
        }

        String oldPwd = request.getPwd();
        String newPwd = request.getNewPwd();

        try {
            if (oldPwd != null && oldPwd.length() > 20) {
                oldPwd = RsaUtils.decryptStr(oldPwd);
            }
        } catch (Exception e) {
        }
        try {
            if (newPwd != null && newPwd.length() > 20) {
                newPwd = RsaUtils.decryptStr(newPwd);
            }
        } catch (Exception e) {
        }

        boolean matches = false;
        if (user.getPassword() != null && (user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"))) {
            matches = passwordEncoder.matches(oldPwd, user.getPassword());
        } else if (oldPwd != null && oldPwd.equals(user.getPassword())) {
            matches = true;
        }
        if (!matches) {
            DEException.throwException("Old password incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPwd));
        sysUserMapper.updateById(user);
    }

    @Override
    public List<Long> firstEchelon(Long limit) {
        return Collections.emptyList();
    }

    @Override
    public CurUserVO queryByAccount(String account) {
        QueryWrapper<SysUser> query = new QueryWrapper<>();
        query.eq("username", account);
        SysUser user = sysUserMapper.selectOne(query);
        if (user == null) return null;
        
        CurUserVO vo = new CurUserVO();
        vo.setId(user.getId());
        vo.setName(user.getNickName());
        return vo;
    }

    @Override
    public List<UserItem> allUser(KeywordRequest request) {
        return Collections.emptyList();
    }

    @Override
    public void adminBind(AdminBindRequest request) {

    }

    @Override
    public void bind(UserBindRequest request) {

    }

    @Override
    public void unBind(Integer origin) {

    }

    @Override
    public List<Integer> bindStatus() {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> getRecipient(UserReciRequest request) {
        return Collections.emptyList();
    }

    @Override
    public boolean orgAdmin() {
        return false;
    }

    @Override
    public boolean defaultOrgAdmin() {
        return false;
    }

    @Override
    public List<UserItem> subOrgUser(List<Long> oidList) {
        return Collections.emptyList();
    }

    @Override
    public List<Long> getRecipientUserIds(UserReciRequest request) {
        return Collections.emptyList();
    }

    @Override
    public List<Long> getUserIdByAccount(String account) {
        return Collections.emptyList();
    }

    @Override
    public List<Long> getUserIdByName(String name) {
        return Collections.emptyList();
    }

    @Override
    public List<Map<String, Object>> listUserInfosByIds(List<Long> ids) {
        return Collections.emptyList();
    }

    @Override
    public MfaQrVO mfaQr() {
        return null;
    }

    @Override
    public Boolean mfaBound() {
        return false;
    }

    @Override
    public void mfaBind(MfaLoginDTO dto) {

    }

    @Override
    public String mfaUnbind(String code) {
        return null;
    }

    @Override
    public void resetBind(Long id) {

    }

    @Override
    public String userLang() {
        Object langObj = CacheUtils.get(io.dataease.constant.CacheConstant.UserCacheConstant.USER_COMMUNITY_LANGUAGE, "de");
        if (ObjectUtils.isNotEmpty(langObj) && StringUtils.isNotBlank(langObj.toString())) {
            return langObj.toString();
        }
        return "zh-CN";
    }

    // ========== 辅助方法 ==========

    /**
     * 转换为用户列表VO
     */
    private UserGridVO toUserGridVO(SysUser user) {
        UserGridVO vo = new UserGridVO();
        vo.setId(user.getId());
        vo.setAccount(user.getUsername());
        vo.setName(user.getNickName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setEnable(user.getStatus() != null && user.getStatus() == 1);
        vo.setCreateTime(user.getCreateTime());

        // 获取用户角色
        List<UserGridRoleItem> roleItems = getUserRoles(user.getId());
        vo.setRoleItems(roleItems);

        return vo;
    }

    /**
     * 转换为用户项VO
     */
    private UserItemVO toUserItemVO(SysUser user) {
        UserItemVO vo = new UserItemVO();
        vo.setId(user.getId());
        vo.setAccount(user.getUsername());
        vo.setName(user.getNickName());
        vo.setEmail(user.getEmail());
        return vo;
    }

    /**
     * 获取用户的角色列表
     */
    private List<UserGridRoleItem> getUserRoles(Long userId) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("user_id", userId);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(qw);

        if (CollectionUtils.isEmpty(userRoles)) {
            return Collections.emptyList();
        }

        // 获取角色ID列表
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        // 查询角色详情
        QueryWrapper<SysRole> roleQw = new QueryWrapper<>();
        roleQw.in("id", roleIds);
        List<SysRole> roles = sysRoleMapper.selectList(roleQw);

        // 转换为VO
        return roles.stream().map(role -> {
            UserGridRoleItem item = new UserGridRoleItem();
            item.setId(role.getId());
            item.setName(role.getName());
            return item;
        }).toList();
    }

    /**
     * 获取角色已绑定的用户ID集合
     */
    private Set<Long> getSelectedUserIds(Long roleId) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("role_id", roleId);
        return sysUserRoleMapper.selectList(qw).stream()
                .map(SysUserRole::getUserId)
                .collect(Collectors.toSet());
    }

    /**
     * 根据角色ID列表获取用户ID列表
     */
    private List<Long> getUserIdsByRoleIds(List<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return Collections.emptyList();
        }
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.in("role_id", roleIds);
        return sysUserRoleMapper.selectList(qw).stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }
}
