package io.dataease.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.backup.service.BackupDatasetService;
import io.dataease.dataset.dao.auto.entity.CoreDatasetGroup;
import io.dataease.dataset.dao.auto.mapper.CoreDatasetGroupMapper;
import io.dataease.model.backup.BackupDataset;
import io.dataease.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class BackupDatasetServiceImpl implements BackupDatasetService {

    @Autowired
    private CoreDatasetGroupMapper coreDatasetGroupMapper;

    @Override
    public List<BackupDataset> exportDatasets() {
        List<BackupDataset> result = new ArrayList<>();
        try {
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("node_type", "dataset");
            List<CoreDatasetGroup> datasets = coreDatasetGroupMapper.selectList(queryWrapper);

            for (CoreDatasetGroup ds : datasets) {
                BackupDataset backup = new BackupDataset();
                backup.setId(String.valueOf(ds.getId()));
                backup.setName(ds.getName());
                backup.setType(ds.getType());
                backup.setModel(ds.getInfo());
                backup.setUnionSql(ds.getUnionSql());
                backup.setCreateBy(ds.getCreateBy());
                backup.setCreateTime(ds.getCreateTime());
                backup.setUpdateTime(ds.getLastUpdateTime());
                result.add(backup);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Export datasets failed", e);
        }
        return result;
    }

    @Override
    public String importDataset(BackupDataset dataset, boolean overwrite, Map<String, String> idMapping) {
        try {
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", dataset.getName());
            CoreDatasetGroup existing = coreDatasetGroupMapper.selectOne(queryWrapper);

            if (existing != null && overwrite) {
                existing.setType(dataset.getType());
                existing.setInfo(dataset.getModel());
                coreDatasetGroupMapper.updateById(existing);
                return String.valueOf(existing.getId());
            } else {
                // overwrite=false: rename and create new
                String newName = dataset.getName();
                if (existing != null) {
                    newName = generateUniqueName(dataset.getName());
                }
                CoreDatasetGroup newDs = new CoreDatasetGroup();
                newDs.setName(newName);
                newDs.setPid(0L);
                newDs.setLevel(0);
                newDs.setNodeType("dataset");
                newDs.setType(dataset.getType());
                newDs.setInfo(dataset.getModel());
                newDs.setUnionSql(dataset.getUnionSql());
                newDs.setCreateBy("1");
                newDs.setCreateTime(System.currentTimeMillis());
                coreDatasetGroupMapper.insert(newDs);

                // MyBatis-Plus insert后需要重新查询获取带ID的完整实体
                newDs = coreDatasetGroupMapper.selectOne(new QueryWrapper<CoreDatasetGroup>().eq("name", newName));

                // 更新ID映射表，记录旧数据集ID到新数据集ID的映射
                String newDatasetId = String.valueOf(newDs.getId());
                idMapping.put(dataset.getId(), newDatasetId);

                LogUtil.getLogger().info("=== Backup import dataset: id={}, name={} ===", newDatasetId, newName);

                return newDatasetId;
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dataset failed: " + dataset.getName(), e);
            throw e;
        }
    }

    private String generateUniqueName(String baseName) {
        for (int i = 1; i <= 1000; i++) {
            String newName = baseName + "_" + i;
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", newName);
            if (coreDatasetGroupMapper.selectCount(queryWrapper) == 0) {
                return newName;
            }
        }
        return baseName + "_" + System.currentTimeMillis();
    }
}
