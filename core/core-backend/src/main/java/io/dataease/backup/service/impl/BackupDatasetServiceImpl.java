package io.dataease.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.backup.service.BackupDatasetService;
import io.dataease.dataset.dao.auto.entity.CoreDatasetGroup;
import io.dataease.dataset.dao.auto.entity.CoreDatasetTable;
import io.dataease.dataset.dao.auto.entity.CoreDatasetTableField;
import io.dataease.dataset.dao.auto.mapper.CoreDatasetGroupMapper;
import io.dataease.dataset.dao.auto.mapper.CoreDatasetTableMapper;
import io.dataease.dataset.dao.auto.mapper.CoreDatasetTableFieldMapper;
import io.dataease.datasource.dao.auto.entity.CoreDatasource;
import io.dataease.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.dataease.model.backup.BackupDataset;
import io.dataease.model.backup.BackupDatasetTable;
import io.dataease.model.backup.BackupDatasetTableField;
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

    @Autowired
    private CoreDatasetTableMapper coreDatasetTableMapper;

    @Autowired
    private CoreDatasetTableFieldMapper coreDatasetTableFieldMapper;

    @Autowired
    private CoreDatasourceMapper coreDatasourceMapper;

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

                // 查询关联的 tables
                QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
                tableQuery.eq("dataset_group_id", ds.getId());
                List<CoreDatasetTable> tables = coreDatasetTableMapper.selectList(tableQuery);

                List<BackupDatasetTable> backupTables = new ArrayList<>();
                for (CoreDatasetTable table : tables) {
                    BackupDatasetTable backupTable = new BackupDatasetTable();
                    backupTable.setId(String.valueOf(table.getId()));
                    backupTable.setName(table.getName());
                    backupTable.setTableName(table.getTableName());
                    backupTable.setDatasourceId(String.valueOf(table.getDatasourceId()));
                    // 按 dataSourceId 查询数据源名称并存储
                    if (table.getDatasourceId() != null) {
                        CoreDatasource ds = coreDatasourceMapper.selectById(table.getDatasourceId());
                        if (ds != null) {
                            backupTable.setDatasourceName(ds.getName());
                        }
                    }
                    backupTable.setType(table.getType());
                    backupTable.setInfo(table.getInfo());
                    backupTable.setSqlVariableDetails(table.getSqlVariableDetails());

                    // 查询关联的 fields
                    QueryWrapper<CoreDatasetTableField> fieldQuery = new QueryWrapper<>();
                    fieldQuery.eq("dataset_table_id", table.getId());
                    List<CoreDatasetTableField> fields = coreDatasetTableFieldMapper.selectList(fieldQuery);

                    List<BackupDatasetTableField> backupFields = new ArrayList<>();
                    for (CoreDatasetTableField field : fields) {
                        BackupDatasetTableField backupField = new BackupDatasetTableField();
                        backupField.setId(String.valueOf(field.getId()));
                        backupField.setOriginName(field.getOriginName());
                        backupField.setName(field.getName());
                        backupField.setDataeaseName(field.getDataeaseName());
                        backupField.setFieldShortName(field.getFieldShortName());
                        backupField.setGroupType(field.getGroupType());
                        backupField.setType(field.getType());
                        backupField.setSize(field.getSize());
                        backupField.setDeType(field.getDeType());
                        backupField.setDeExtractType(field.getDeExtractType());
                        backupField.setExtField(field.getExtField());
                        backupField.setChecked(field.getChecked());
                        backupField.setColumnIndex(field.getColumnIndex());
                        backupField.setAccuracy(field.getAccuracy());
                        backupField.setDateFormat(field.getDateFormat());
                        backupField.setDateFormatType(field.getDateFormatType());
                        backupField.setParams(field.getParams());
                        backupField.setOrderChecked(field.getOrderChecked());
                        backupField.setGroupList(field.getGroupList());
                        backupField.setOtherGroup(field.getOtherGroup());
                        backupFields.add(backupField);
                    }
                    backupTable.setFields(backupFields);
                    backupTables.add(backupTable);
                }
                backup.setTables(backupTables);
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
