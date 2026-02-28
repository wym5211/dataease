package io.dataease.core.permissions.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.dataease.api.permissions.login.dto.MfaLoginDTO;
import io.dataease.api.permissions.login.vo.MfaQrVO;
import io.dataease.api.permissions.role.dto.UserRequest;
import io.dataease.api.permissions.user.api.UserApi;
import io.dataease.api.permissions.user.dto.*;
import io.dataease.api.permissions.user.vo.*;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.auth.vo.TokenVO;
import io.dataease.utils.AuthUtils;
import io.dataease.exception.DEException;
import io.dataease.model.KeywordRequest;
import io.dataease.system.dao.auto.entity.SysUser;
import io.dataease.system.dao.auto.mapper.SysUserMapper;
import io.dataease.utils.BeanUtils;
import io.dataease.utils.IPUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service("userServer")
@Primary
@RestController
@RequestMapping("/user")
public class CoreUserServer implements UserApi {

    @Autowired
    private SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public IPage<UserGridVO> pager(int goPage, int pageSize, UserGridRequest request) {
        return null;
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
    public Long create(UserCreator creator) {
        return 0L;
    }

    @Override
    public void createPlatform(PlatformUserCreator creator) {

    }

    @Override
    public void edit(UserEditor editor) {

    }

    @Override
    public void personEdit(UserEditor editor) {

    }

    @Override
    public void delete(Long id) {

    }

    @Override
    public void batchDel(List<Long> ids) {

    }

    @Override
    public List<UserItemVO> optionForRole(UserRequest request) {
        return Collections.emptyList();
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
        return null;
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
            vo.setLanguage("zh-CN");
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

    }

    @Override
    public void enable(EnableSwitchRequest request) {

    }

    @Override
    public void modifyPwd(ModifyPwdRequest request) {
        TokenUserBO tokenUser = AuthUtils.getUser();
        SysUser user = sysUserMapper.selectById(tokenUser.getUserId());
        if (user == null) {
            DEException.throwException("User not found");
        }
        
        // Verify old password (if stored as BCrypt)
        // If legacy (MD5), we might need to handle migration or just support BCrypt from now on.
        // Assuming we start fresh or migrated.
        if (!passwordEncoder.matches(request.getPwd(), user.getPassword())) {
             // Fallback to MD5 check if migration is needed?
             // For now, strict BCrypt.
             DEException.throwException("Old password incorrect");
        }
        
        user.setPassword(passwordEncoder.encode(request.getNewPwd()));
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
        return "zh-CN";
    }
}
