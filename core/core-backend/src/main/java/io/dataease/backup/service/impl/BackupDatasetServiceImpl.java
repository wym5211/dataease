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
                backup.setDescription(ds.getInfo());
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
    public void importDataset(BackupDataset dataset, boolean overwrite) {
        try {
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", dataset.getName());
            CoreDatasetGroup existing = coreDatasetGroupMapper.selectOne(queryWrapper);

            if (existing != null && overwrite) {
                existing.setType(dataset.getType());
                existing.setInfo(dataset.getModel());
                coreDatasetGroupMapper.updateById(existing);
            } else if (existing == null) {
                CoreDatasetGroup newDs = new CoreDatasetGroup();
                newDs.setName(dataset.getName() + "_imported");
                newDs.setPid(0L);
                newDs.setLevel(0);
                newDs.setNodeType("dataset");
                newDs.setType(dataset.getType());
                newDs.setInfo(dataset.getModel());
                newDs.setCreateTime(System.currentTimeMillis());
                coreDatasetGroupMapper.insert(newDs);
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dataset failed: " + dataset.getName(), e);
            throw e;
        }
    }
}
