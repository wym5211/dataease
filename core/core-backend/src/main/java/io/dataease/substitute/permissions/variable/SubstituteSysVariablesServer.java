package io.dataease.substitute.permissions.variable;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.dataease.api.permissions.variable.api.SysVariablesApi;
import io.dataease.api.permissions.variable.dto.SysVariableDto;
import io.dataease.api.permissions.variable.dto.SysVariableValueDto;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

/**
 * 社区版系统变量接口替补实现
 */
@Hidden
@Component
@RestController
@RequestMapping("/sysVariable")
@ConditionalOnMissingBean(name = "sysVariablesServer")
public class SubstituteSysVariablesServer implements SysVariablesApi {

    @Override
    public SysVariableDto create(SysVariableDto sysVariableDto) {
        return null;
    }

    @Override
    public SysVariableDto edit(SysVariableDto sysVariableDto) {
        return null;
    }

    @Override
    public void delete(Long id) {
    }

    @Override
    public SysVariableDto detail(Long id) {
        return null;
    }

    @Override
    public List<SysVariableDto> query(SysVariableDto sysVariableDto) {
        return Collections.emptyList();
    }

    @Override
    public SysVariableValueDto createValue(SysVariableValueDto sysVariableValueDto) {
        return null;
    }

    @Override
    public SysVariableValueDto editValue(SysVariableValueDto sysVariableValueDto) {
        return null;
    }

    @Override
    public void deleteValue(String id) {
    }

    @Override
    public List<SysVariableValueDto> selectVariableValue(Long id) {
        return Collections.emptyList();
    }

    @Override
    public IPage<SysVariableValueDto> selectPage(int goPage, int pageSize, SysVariableValueDto sysVariableValueDto) {
        return new Page<>(goPage, pageSize);
    }

    @Override
    public void batchDel(List<Long> ids) {
    }
}
