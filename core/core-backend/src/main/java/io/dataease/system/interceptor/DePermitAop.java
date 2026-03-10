package io.dataease.system.interceptor;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.auth.DeApiPath;
import io.dataease.auth.DePermit;
import io.dataease.auth.bo.TokenUserBO;
import io.dataease.constant.AuthResourceEnum;
import io.dataease.exception.DEException;
import io.dataease.result.ResultCode;
import io.dataease.system.dao.auto.entity.SysResourcePermission;
import io.dataease.system.dao.auto.entity.SysRoleMenu;
import io.dataease.system.dao.auto.entity.SysUserRole;
import io.dataease.system.dao.auto.mapper.SysResourcePermissionMapper;
import io.dataease.system.dao.auto.mapper.SysRoleMenuMapper;
import io.dataease.system.dao.auto.mapper.SysUserRoleMapper;
import io.dataease.utils.AuthUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
public class DePermitAop {

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysResourcePermissionMapper sysResourcePermissionMapper;

    public DePermitAop(SysUserRoleMapper sysUserRoleMapper,
                       SysRoleMenuMapper sysRoleMenuMapper,
                       SysResourcePermissionMapper sysResourcePermissionMapper) {
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysResourcePermissionMapper = sysResourcePermissionMapper;
    }

    @Around("execution(public * *(..)) && @within(org.springframework.web.bind.annotation.RequestMapping)")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Method targetMethod = AopUtils.getMostSpecificMethod(method, pjp.getTarget().getClass());

        DePermit dePermit = resolveDePermit(targetMethod, pjp.getTarget().getClass());
        if (dePermit == null) {
            return pjp.proceed();
        }

        TokenUserBO user = AuthUtils.getUser();
        if (user == null) {
            DEException.throwException(ResultCode.USER_NOT_LOGGED_IN.code(), ResultCode.USER_NOT_LOGGED_IN.message());
        }
        if (AuthUtils.isSysAdmin(user.getUserId())) {
            return pjp.proceed();
        }

        AuthResourceEnum rt = resolveResourceType(pjp.getTarget().getClass());
        Object[] args = pjp.getArgs();

        for (String expr : dePermit.value()) {
            String permit = evalPermitExpr(expr, args);
            if (!checkPermit(user, rt, dePermit, permit, args)) {
                DEException.throwException(ResultCode.INTERFACE_FORBID_VISIT.code(), ResultCode.INTERFACE_FORBID_VISIT.message());
            }
        }

        return pjp.proceed();
    }

    private boolean checkPermit(TokenUserBO user, AuthResourceEnum rt, DePermit dePermit, String permit, Object[] args) {
        if (StringUtils.isBlank(permit)) {
            return false;
        }
        if (StringUtils.startsWith(permit, "m:")) {
            return checkMenuPermit(user, rt);
        }
        int idx = StringUtils.indexOf(permit, ':');
        if (idx <= 0) {
            return false;
        }
        String idPart = StringUtils.substring(permit, 0, idx);
        String action = StringUtils.substring(permit, idx + 1);
        if (rt == AuthResourceEnum.USER) {
            try {
                Long targetUid = Long.parseLong(idPart);
                if (Objects.equals(targetUid, user.getUserId())) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        String resourceType = resolveBusiFlag(dePermit, args, rt);
        int required = actionToPermission(action);
        return checkResourcePermit(user, resourceType, idPart, required);
    }

    private boolean checkMenuPermit(TokenUserBO user, AuthResourceEnum rt) {
        if (rt == null) {
            return false;
        }
        long menuId = rt.getMenuId();
        Set<Long> roleIds = getRoleIds(user.getUserId());
        if (roleIds.isEmpty()) {
            return false;
        }
        QueryWrapper<SysRoleMenu> qw = new QueryWrapper<>();
        qw.eq("menu_id", menuId);
        qw.in("role_id", roleIds);
        return sysRoleMenuMapper.selectCount(qw) > 0;
    }

    private boolean checkResourcePermit(TokenUserBO user, String resourceType, String resourceId, int required) {
        List<String> resourceTypes = resolveResourceTypes(resourceType);
        QueryWrapper<SysResourcePermission> qw = new QueryWrapper<>();
        qw.eq("resource_id", resourceId);
        if (CollectionUtils.isNotEmpty(resourceTypes)) {
            qw.in("resource_type", resourceTypes);
        }
        qw.and(w -> w.eq("owner_type", 0).eq("owner_id", user.getUserId()));
        List<SysResourcePermission> userPerms = sysResourcePermissionMapper.selectList(qw);
        if (CollectionUtils.isNotEmpty(userPerms) && userPerms.stream().anyMatch(p -> hasPermission(p.getPermission(), required))) {
            return true;
        }

        Set<Long> roleIds = getRoleIds(user.getUserId());
        if (roleIds.isEmpty()) {
            return false;
        }
        QueryWrapper<SysResourcePermission> rq = new QueryWrapper<>();
        rq.eq("resource_id", resourceId);
        if (CollectionUtils.isNotEmpty(resourceTypes)) {
            rq.in("resource_type", resourceTypes);
        }
        rq.eq("owner_type", 1);
        rq.in("owner_id", roleIds);
        List<SysResourcePermission> rolePerms = sysResourcePermissionMapper.selectList(rq);
        return CollectionUtils.isNotEmpty(rolePerms) && rolePerms.stream().anyMatch(p -> hasPermission(p.getPermission(), required));
    }

    private boolean hasPermission(Integer actual, int required) {
        if (actual == null) {
            return false;
        }
        if (required == 1 && (actual & 2) == 2) {
            return true;
        }
        return (actual & required) == required;
    }

    private int actionToPermission(String action) {
        if (StringUtils.containsIgnoreCase(action, "manage") || StringUtils.containsIgnoreCase(action, "write")) {
            return 2;
        }
        if (StringUtils.containsIgnoreCase(action, "share")) {
            return 4;
        }
        return 1;
    }

    private Set<Long> getRoleIds(Long uid) {
        QueryWrapper<SysUserRole> qw = new QueryWrapper<>();
        qw.eq("user_id", uid);
        return sysUserRoleMapper.selectList(qw).stream().map(SysUserRole::getRoleId).collect(Collectors.toSet());
    }

    private String evalPermitExpr(String expr, Object[] args) {
        if (StringUtils.isBlank(expr)) {
            return null;
        }
        if (!StringUtils.contains(expr, "#")) {
            return expr;
        }
        StandardEvaluationContext ctx = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            ctx.setVariable("p" + i, args[i]);
            ctx.setVariable("a" + i, args[i]);
        }
        Expression e = expressionParser.parseExpression(expr);
        Object val = e.getValue(ctx);
        return val == null ? null : val.toString();
    }

    private String resolveBusiFlag(DePermit dePermit, Object[] args, AuthResourceEnum rt) {
        String rawFlag;
        if (StringUtils.isBlank(dePermit.busiFlag())) {
            rawFlag = rt == null ? null : rt.name().toLowerCase();
        } else {
            rawFlag = evalPermitExpr(dePermit.busiFlag(), args);
        }
        return normalizeResourceType(rawFlag);
    }

    private List<String> resolveResourceTypes(String resourceType) {
        if (StringUtils.isBlank(resourceType)) {
            return null;
        }
        List<String> result = new ArrayList<>();
        String normalizedType = normalizeResourceType(resourceType);
        result.add(normalizedType);
        if (StringUtils.equals(normalizedType, "dashboard")) {
            result.add("panel");
        } else if (StringUtils.equals(normalizedType, "dataV")) {
            result.add("screen");
        } else if (StringUtils.equals(normalizedType, "panel")) {
            result.add("dashboard");
        } else if (StringUtils.equals(normalizedType, "screen")) {
            result.add("dataV");
        }
        return result.stream().filter(StringUtils::isNotBlank).distinct().toList();
    }

    private String normalizeResourceType(String type) {
        if (StringUtils.isBlank(type)) {
            return type;
        }
        if (StringUtils.equalsAnyIgnoreCase(type, "dashboard", "dashboard-copy")) {
            return "dashboard";
        }
        if (StringUtils.equalsAnyIgnoreCase(type, "dataV", "dataV-copy")) {
            return "dataV";
        }
        return type;
    }

    private DePermit resolveDePermit(Method method, Class<?> targetClass) {
        DePermit ann = AnnotatedElementUtils.findMergedAnnotation(method, DePermit.class);
        if (ann != null) {
            return ann;
        }
        Class<?> c = targetClass;
        while (c != null) {
            for (Class<?> itf : c.getInterfaces()) {
                try {
                    Method im = itf.getMethod(method.getName(), method.getParameterTypes());
                    ann = AnnotatedElementUtils.findMergedAnnotation(im, DePermit.class);
                    if (ann != null) {
                        return ann;
                    }
                } catch (NoSuchMethodException ignored) {
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    private AuthResourceEnum resolveResourceType(Class<?> targetClass) {
        Class<?> c = targetClass;
        while (c != null) {
            DeApiPath apiPath = AnnotatedElementUtils.findMergedAnnotation(c, DeApiPath.class);
            if (apiPath != null) {
                return apiPath.rt();
            }
            for (Class<?> itf : c.getInterfaces()) {
                apiPath = AnnotatedElementUtils.findMergedAnnotation(itf, DeApiPath.class);
                if (apiPath != null) {
                    return apiPath.rt();
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
