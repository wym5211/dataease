package io.dataease.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.backup.service.BackupDatasourceService;
import io.dataease.datasource.dao.auto.entity.CoreDatasource;
import io.dataease.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.dataease.model.backup.BackupDatasource;
import io.dataease.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BackupDatasourceServiceImpl implements BackupDatasourceService {

    @Autowired
    private CoreDatasourceMapper coreDatasourceMapper;

    @Override
    public List<BackupDatasource> exportDatasources() {
        List<BackupDatasource> result = new ArrayList<>();
        try {
            QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("pid", 0);
            List<CoreDatasource> datasources = coreDatasourceMapper.selectList(queryWrapper);

            for (CoreDatasource ds : datasources) {
                BackupDatasource backup = new BackupDatasource();
                backup.setId(String.valueOf(ds.getId()));
                backup.setName(ds.getName());
                backup.setDescription(ds.getDescription());
                backup.setType(ds.getType());
                backup.setConfiguration(ds.getConfiguration());
                backup.setCreateTime(ds.getCreateTime());
                backup.setUpdateTime(ds.getUpdateTime());
                result.add(backup);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Export datasources failed", e);
        }
        return result;
    }

    @Override
    public void importDatasource(BackupDatasource datasource, boolean overwrite) {
        try {
            QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", datasource.getName());
            CoreDatasource existing = coreDatasourceMapper.selectOne(queryWrapper);

            if (existing != null && overwrite) {
                existing.setDescription(datasource.getDescription());
                existing.setType(datasource.getType());
                existing.setConfiguration(datasource.getConfiguration());
                existing.setUpdateTime(System.currentTimeMillis());
                coreDatasourceMapper.updateById(existing);
            } else {
                // overwrite=false: rename and create new
                String newName = datasource.getName();
                if (existing != null) {
                    // name exists, need to rename
                    newName = generateUniqueName(datasource.getName());
                }
                CoreDatasource newDs = new CoreDatasource();
                newDs.setName(newName);
                newDs.setDescription(datasource.getDescription());
                newDs.setType(datasource.getType());
                newDs.setConfiguration(datasource.getConfiguration());
                newDs.setPid(0L);
                newDs.setEditType("1");
                newDs.setCreateTime(System.currentTimeMillis());
                newDs.setUpdateTime(System.currentTimeMillis());
                coreDatasourceMapper.insert(newDs);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Import datasource failed: " + datasource.getName(), e);
            throw e;
        }
    }

    private String generateUniqueName(String baseName) {
        // Try baseName_1, baseName_2, ... until unique name found
        for (int i = 1; i <= 1000; i++) {
            String newName = baseName + "_" + i;
            QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", newName);
            if (coreDatasourceMapper.selectCount(queryWrapper) == 0) {
                return newName;
            }
        }
        // Fallback: use timestamp
        return baseName + "_" + System.currentTimeMillis();
    }
}
