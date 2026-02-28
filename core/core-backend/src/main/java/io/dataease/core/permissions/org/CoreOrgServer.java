package io.dataease.core.permissions.org;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.api.permissions.org.api.OrgApi;
import io.dataease.api.permissions.org.dto.OrgCreator;
import io.dataease.api.permissions.org.dto.OrgEditor;
import io.dataease.api.permissions.org.dto.OrgLazyRequest;
import io.dataease.api.permissions.org.dto.OrgRequest;
import io.dataease.api.permissions.org.vo.*;
import io.dataease.exception.DEException;
import io.dataease.model.KeywordRequest;
import io.dataease.system.dao.auto.entity.SysOrg;
import io.dataease.system.dao.auto.mapper.SysOrgMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@Service("orgServer")
@Primary
@RestController
@RequestMapping("/org")
public class CoreOrgServer implements OrgApi {

    @Autowired
    private SysOrgMapper sysOrgMapper;

    @Override
    public List<OrgPageVO> pageTree(OrgRequest request) {
        List<SysOrg> orgs = listAll();
        return buildPageTree(orgs);
    }

    @Override
    public LazyTreeVO lazyPageTree(OrgLazyRequest request) {
        LazyTreeVO vo = new LazyTreeVO();
        vo.setNodes(Collections.emptyList());
        vo.setExpandKeyList(Collections.emptyList());
        return vo;
    }

    @Override
    public Long create(OrgCreator creator) {
        SysOrg org = new SysOrg();
        org.setPid(creator.getPid() == null ? 0L : creator.getPid());
        org.setName(creator.getName());
        org.setDescription(null);
        org.setSort(0);
        org.setCreateTime(System.currentTimeMillis());
        sysOrgMapper.insert(org);
        return org.getId();
    }

    @Override
    public void edit(OrgEditor editor) {
        SysOrg org = sysOrgMapper.selectById(editor.getId());
        if (org == null) {
            DEException.throwException("组织不存在");
        }
        org.setName(editor.getName());
        sysOrgMapper.updateById(org);
    }

    @Override
    public void delete(Long id) {
        QueryWrapper<SysOrg> childQ = new QueryWrapper<>();
        childQ.eq("pid", id);
        if (sysOrgMapper.selectCount(childQ) > 0) {
            DEException.throwException("存在子组织，无法删除");
        }
        sysOrgMapper.deleteById(id);
    }

    @Override
    public List<MountedVO> mounted(KeywordRequest request) {
        List<SysOrg> orgs = listAll();
        return buildMountedTree(orgs);
    }

    @Override
    public LazyMountedVO lazyMounted(OrgLazyRequest request) {
        LazyMountedVO vo = new LazyMountedVO();
        vo.setNodes(Collections.emptyList());
        vo.setExpandKeyList(Collections.emptyList());
        return vo;
    }

    @Override
    public boolean resourceExist(Long oid) {
        return true;
    }

    @Override
    public OrgDetailVO detail(Long oid) {
        SysOrg org = sysOrgMapper.selectById(oid);
        if (org == null) {
            return null;
        }
        OrgDetailVO vo = new OrgDetailVO();
        vo.setId(org.getId());
        vo.setName(org.getName());
        vo.setPid(org.getPid());
        vo.setRootPath(String.valueOf(org.getId()));
        return vo;
    }

    @Override
    public List<String> subOrgs() {
        return Collections.emptyList();
    }

    private List<SysOrg> listAll() {
        QueryWrapper<SysOrg> qw = new QueryWrapper<>();
        qw.orderByAsc("sort", "id");
        return sysOrgMapper.selectList(qw);
    }

    private List<OrgPageVO> buildPageTree(List<SysOrg> orgs) {
        Map<Long, List<SysOrg>> children = orgs.stream().collect(Collectors.groupingBy(o -> o.getPid() == null ? 0L : o.getPid()));
        List<SysOrg> roots = children.getOrDefault(0L, Collections.emptyList());
        return roots.stream().map(root -> toOrgPage(root, children)).toList();
    }

    private OrgPageVO toOrgPage(SysOrg org, Map<Long, List<SysOrg>> children) {
        OrgPageVO vo = new OrgPageVO();
        vo.setId(org.getId());
        vo.setName(org.getName());
        vo.setCreateTime(org.getCreateTime());
        List<SysOrg> child = children.get(org.getId());
        if (CollectionUtils.isNotEmpty(child)) {
            vo.setChildren(child.stream().map(o -> toOrgPage(o, children)).toList());
        }
        return vo;
    }

    private List<MountedVO> buildMountedTree(List<SysOrg> orgs) {
        Map<Long, List<SysOrg>> children = orgs.stream().collect(Collectors.groupingBy(o -> o.getPid() == null ? 0L : o.getPid()));
        List<SysOrg> roots = children.getOrDefault(0L, Collections.emptyList());
        return roots.stream().map(root -> toMounted(root, children)).toList();
    }

    private MountedVO toMounted(SysOrg org, Map<Long, List<SysOrg>> children) {
        MountedVO vo = new MountedVO();
        vo.setId(org.getId());
        vo.setName(org.getName());
        vo.setReadOnly(false);
        List<SysOrg> child = children.get(org.getId());
        if (CollectionUtils.isEmpty(child)) {
            vo.setLeaf(true);
            return vo;
        }
        vo.setChildren(child.stream().map(o -> toMounted(o, children)).toList());
        return vo;
    }
}
