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
import io.dataease.model.backup.BackupFolder;
import io.dataease.utils.LogUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        return exportDatasets(null);
    }

    @Override
    public List<BackupDataset> exportDatasets(List<String> ids) {
        List<BackupDataset> result = new ArrayList<>();
        try {
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("node_type", "dataset");
            if (ids != null && !ids.isEmpty()) {
                queryWrapper.in("id", ids);
            }
            List<CoreDatasetGroup> datasets = coreDatasetGroupMapper.selectList(queryWrapper);

            for (CoreDatasetGroup ds : datasets) {
                BackupDataset backup = new BackupDataset();
                backup.setId(String.valueOf(ds.getId()));
                backup.setName(ds.getName());
                backup.setType(ds.getType());
                backup.setModel(ds.getInfo());
                backup.setUnionSql(ds.getUnionSql());
                backup.setIsCross(ds.getIsCross());
                backup.setCreateBy(ds.getCreateBy());
                backup.setCreateTime(ds.getCreateTime());
                backup.setUpdateTime(ds.getLastUpdateTime());
                backup.setPid(ds.getPid());
                backup.setLevel(ds.getLevel());
                backup.setNodeType(ds.getNodeType());

                // 填充目录名称（用于导入时按名称匹配）
                if (ds.getPid() != null && ds.getPid() != 0L) {
                    CoreDatasetGroup parentFolder = coreDatasetGroupMapper.selectById(ds.getPid());
                    if (parentFolder != null) {
                        backup.setFolderName(parentFolder.getName());
                        if (parentFolder.getPid() != null && parentFolder.getPid() != 0L) {
                            CoreDatasetGroup grandParentFolder = coreDatasetGroupMapper.selectById(parentFolder.getPid());
                            if (grandParentFolder != null) {
                                backup.setParentFolderName(grandParentFolder.getName());
                            }
                        }
                    }
                }

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
                        CoreDatasource datasource = coreDatasourceMapper.selectById(table.getDatasourceId());
                        if (datasource != null) {
                            backupTable.setDatasourceName(datasource.getName());
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
                        backupField.setDatasourceId(field.getDatasourceId() != null ? String.valueOf(field.getDatasourceId()) : null);
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
    public List<BackupFolder> collectDatasetFolders() {
        return collectDatasetFolders(null);
    }

    @Override
    public List<BackupFolder> collectDatasetFolders(List<String> ids) {
        List<BackupFolder> result = new ArrayList<>();
        Map<String, BackupFolder> folderMap = new HashMap<>();
        try {
            // Query all dataset groups (folders and datasets)
            List<CoreDatasetGroup> allGroups;
            if (ids != null && !ids.isEmpty()) {
                QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("id", ids);
                allGroups = coreDatasetGroupMapper.selectList(queryWrapper);
            } else {
                allGroups = coreDatasetGroupMapper.selectList(null);
            }

            // First pass: collect all folders directly (including empty folders)
            if (ids == null || ids.isEmpty()) {
                for (CoreDatasetGroup group : allGroups) {
                    if ("folder".equals(group.getNodeType())) {
                        String key = buildFolderKey(group, folderMap);
                        if (!folderMap.containsKey(key)) {
                            BackupFolder bf = new BackupFolder();
                            bf.setId(String.valueOf(group.getId()));
                            bf.setName(group.getName());
                            bf.setPid(group.getPid());
                            bf.setLevel(group.getLevel() != null ? group.getLevel() : 1);
                            bf.setNodeType("folder");
                            bf.setResourceType("dataset");
                            if (group.getPid() != null && group.getPid() != 0L) {
                                bf.setParentName(getDatasetGroupNameById(group.getPid()));
                            }
                            folderMap.put(key, bf);
                            result.add(bf);
                        }
                    }
                }
            }

            // Second pass: collect parent folders for datasets
            for (CoreDatasetGroup group : allGroups) {
                if ("dataset".equals(group.getNodeType()) && group.getPid() != null && group.getPid() != 0L) {
                    result.addAll(collectDatasetFolders(group.getId(), folderMap));
                }
            }
        } catch (Exception e) {
            LogUtil.getLogger().error("Collect dataset folders failed", e);
        }
        return result;
    }

    private String buildFolderKey(CoreDatasetGroup group, Map<String, BackupFolder> folderMap) {
        return group.getName() + "_" + (group.getPid() != null && group.getPid() != 0L ?
            getDatasetGroupNameById(group.getPid()) : "root");
    }

    private String getDatasetGroupNameById(Long groupId) {
        if (groupId == null || groupId == 0L) {
            return null;
        }
        CoreDatasetGroup group = coreDatasetGroupMapper.selectById(groupId);
        return group != null ? group.getName() : null;
    }

    @Override
    public List<BackupDataset> exportDatasetsByTableIds(List<Long> tableIds) {
        List<BackupDataset> result = new ArrayList<>();
        if (tableIds == null || tableIds.isEmpty()) {
            return result;
        }
        try {
            // 从 CoreDatasetTable 获取 dataset_group_id
            QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
            tableQuery.in("id", tableIds);
            List<CoreDatasetTable> tables = coreDatasetTableMapper.selectList(tableQuery);

            // 收集唯一的 dataset_group_id
            Set<Long> datasetGroupIds = tables.stream()
                .map(CoreDatasetTable::getDatasetGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            if (datasetGroupIds.isEmpty()) {
                return result;
            }

            // 导出这些数据集
            List<String> ids = datasetGroupIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
            return exportDatasets(ids);
        } catch (Exception e) {
            LogUtil.getLogger().error("Export datasets by tableIds failed", e);
        }
        return result;
    }

    @Override
    public List<BackupFolder> collectDatasetFoldersByTableIds(List<Long> tableIds) {
        List<BackupFolder> result = new ArrayList<>();
        if (tableIds == null || tableIds.isEmpty()) {
            return result;
        }
        try {
            // 从 CoreDatasetTable 获取 dataset_group_id
            QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
            tableQuery.in("id", tableIds);
            List<CoreDatasetTable> tables = coreDatasetTableMapper.selectList(tableQuery);

            // 收集唯一的 dataset_group_id
            Set<Long> datasetGroupIds = tables.stream()
                .map(CoreDatasetTable::getDatasetGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            if (datasetGroupIds.isEmpty()) {
                return result;
            }

            // 收集这些数据集的目录
            List<String> ids = datasetGroupIds.stream()
                .map(String::valueOf)
                .collect(Collectors.toList());
            return collectDatasetFolders(ids);
        } catch (Exception e) {
            LogUtil.getLogger().error("Collect dataset folders by tableIds failed", e);
        }
        return result;
    }

    @Override
    public List<String> collectDatasourceIds(List<BackupDataset> datasets) {
        Set<String> datasourceIds = new HashSet<>();
        if (datasets == null || datasets.isEmpty()) {
            return new ArrayList<>(datasourceIds);
        }
        for (BackupDataset dataset : datasets) {
            if (dataset.getTables() != null) {
                for (BackupDatasetTable table : dataset.getTables()) {
                    if (table.getDatasourceId() != null) {
                        datasourceIds.add(table.getDatasourceId());
                    }
                }
            }
        }
        LogUtil.getLogger().info("collectDatasourceIds: 从 {} 个数据集中收集到 {} 个数据源ID", datasets.size(), datasourceIds.size());
        return new ArrayList<>(datasourceIds);
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
                newDs.setIsCross(dataset.getIsCross());
                newDs.setCreateBy("1");
                newDs.setCreateTime(System.currentTimeMillis());
                coreDatasetGroupMapper.insert(newDs);

                // MyBatis-Plus insert后需要重新查询获取带ID的完整实体
                newDs = coreDatasetGroupMapper.selectOne(new QueryWrapper<CoreDatasetGroup>().eq("name", newName).eq("node_type", "dataset"));

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

    /**
     * 递归收集数据集的目录链
     */
    private List<BackupFolder> collectDatasetFolders(Long datasetId, Map<String, BackupFolder> folderMap) {
        List<BackupFolder> result = new ArrayList<>();
        CoreDatasetGroup group = coreDatasetGroupMapper.selectById(datasetId);
        if (group == null || group.getPid() == null || group.getPid() == 0L) {
            return result;
        }
        CoreDatasetGroup parent = coreDatasetGroupMapper.selectById(group.getPid());
        if (parent == null) {
            return result;
        }
        if ("folder".equals(parent.getNodeType())) {
            // 获取父目录名称用于key构建
            String parentName = null;
            if (parent.getPid() != null && parent.getPid() != 0L) {
                CoreDatasetGroup grandParent = coreDatasetGroupMapper.selectById(parent.getPid());
                if (grandParent != null) {
                    parentName = grandParent.getName();
                }
            }
            // 使用 name + parentName 作为唯一key，与createOrFindDatasetFolder保持一致
            String key = parent.getName() + "_" + (parentName != null ? parentName : "root");
            if (folderMap.containsKey(key)) {
                return result;
            }
            BackupFolder folder = new BackupFolder();
            folder.setId(String.valueOf(parent.getId()));
            folder.setName(parent.getName());
            folder.setPid(parent.getPid());
            folder.setLevel(parent.getLevel());
            folder.setNodeType(parent.getNodeType());
            folder.setResourceType("dataset");
            folder.setParentName(parentName);
            folderMap.put(key, folder);
            result.add(folder);
            result.addAll(collectDatasetFolders(parent.getId(), folderMap));
        }
        return result;
    }

    /**
     * 导入数据集及其关联的 tables 和 fields
     * @param dataset 数据集备份信息
     * @param overwrite 是否覆盖
     * @param datasourceIdMapping 数据源 ID 映射表 (old_id → new_id)
     * @return 新数据集 ID
     */
    public String importDatasetWithTables(BackupDataset dataset, boolean overwrite,
                                          Map<String, String> datasourceIdMapping) {
        Map<String, String> datasetIdMapping = new HashMap<>();
        return importDatasetWithTables(dataset, overwrite, datasourceIdMapping, datasetIdMapping);
    }

    private String importDatasetWithTables(BackupDataset dataset, boolean overwrite,
                                            Map<String, String> datasourceIdMapping,
                                            Map<String, String> datasetIdMapping) {
        try {
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", dataset.getName()).eq("node_type", "dataset");
            CoreDatasetGroup existing = coreDatasetGroupMapper.selectOne(queryWrapper);

            String newDatasetId;
            if (existing != null && overwrite) {
                existing.setType(dataset.getType());
                existing.setInfo(dataset.getModel());
                if (dataset.getIsCross() != null) {
                    existing.setIsCross(dataset.getIsCross());
                }
                coreDatasetGroupMapper.updateById(existing);
                newDatasetId = String.valueOf(existing.getId());
                // 覆盖模式下，先删除原有的 tables 和 fields
                deleteExistingTablesAndFields(Long.parseLong(newDatasetId));
            } else {
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
                if (dataset.getIsCross() != null) {
                    newDs.setIsCross(dataset.getIsCross());
                }
                newDs.setCreateBy("1");
                newDs.setCreateTime(System.currentTimeMillis());
                coreDatasetGroupMapper.insert(newDs);
                newDs = coreDatasetGroupMapper.selectOne(new QueryWrapper<CoreDatasetGroup>().eq("name", newName).eq("node_type", "dataset"));
                newDatasetId = String.valueOf(newDs.getId());
            }

            // 更新 dataset ID 映射
            datasetIdMapping.put(dataset.getId(), newDatasetId);

            // 导入 tables 和 fields
            if (dataset.getTables() != null) {
                Map<String, String> tableIdMapping = new HashMap<>();
                for (BackupDatasetTable backupTable : dataset.getTables()) {
                    importDatasetTable(backupTable, newDatasetId, datasourceIdMapping, tableIdMapping, null);
                }
            }

            LogUtil.getLogger().info("=== Backup import dataset: id={}, name={}, newId={} ===", dataset.getId(), dataset.getName(), newDatasetId);
            return newDatasetId;
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dataset failed: " + dataset.getName(), e);
            throw e;
        }
    }

    /**
     * 覆盖模式下，先删除目标数据集中原有的 tables 和 fields
     */
    private void deleteExistingTablesAndFields(Long datasetId) {
        // 查询该数据集下的所有 table
        QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
        tableQuery.eq("dataset_group_id", datasetId);
        List<CoreDatasetTable> existingTables = coreDatasetTableMapper.selectList(tableQuery);

        for (CoreDatasetTable table : existingTables) {
            // 删除关联的 fields
            QueryWrapper<CoreDatasetTableField> fieldQuery = new QueryWrapper<>();
            fieldQuery.eq("dataset_table_id", table.getId());
            coreDatasetTableFieldMapper.delete(fieldQuery);
        }
        // 删除 tables
        coreDatasetTableMapper.delete(tableQuery);
    }

    private void importDatasetTable(BackupDatasetTable backupTable, String newDatasetId,
                                    Map<String, String> datasourceIdMapping,
                                    Map<String, String> tableIdMapping,
                                    Map<String, Long> datasourceNameToId) {
        // 1. datasourceId 从预查询的 Map 中查找，如果失败则使用ID映射
        Long newDatasourceId = null;
        if (backupTable.getDatasourceName() != null && datasourceNameToId != null) {
            newDatasourceId = datasourceNameToId.get(backupTable.getDatasourceName());
            if (newDatasourceId != null) {
                LogUtil.getLogger().info("=== Found datasource by name from cache: {} -> {} ===", backupTable.getDatasourceName(), newDatasourceId);
            }
        }

        // 2. 如果按名称查找失败，尝试使用ID映射
        if (newDatasourceId == null && backupTable.getDatasourceId() != null && datasourceIdMapping != null) {
            String mappedDsId = datasourceIdMapping.get(backupTable.getDatasourceId());
            if (mappedDsId != null) {
                try {
                    newDatasourceId = Long.parseLong(mappedDsId);
                    LogUtil.getLogger().info("=== Found datasource by ID mapping: {} -> {} ===", backupTable.getDatasourceId(), newDatasourceId);
                } catch (NumberFormatException e) {
                    LogUtil.getLogger().warn("=== Invalid datasource ID in mapping: {} ===", mappedDsId);
                }
            } else {
                LogUtil.getLogger().warn("=== Datasource ID not found in mapping: {} ===", backupTable.getDatasourceId());
            }
        }

        if (newDatasourceId == null) {
            LogUtil.getLogger().warn("=== Could not resolve datasource for table: {}, datasourceName: {}, datasourceId: {} ===",
                backupTable.getName(), backupTable.getDatasourceName(), backupTable.getDatasourceId());
        }

        // 2. 创建或更新 table
        QueryWrapper<CoreDatasetTable> tableQuery = new QueryWrapper<>();
        tableQuery.eq("name", backupTable.getName()).eq("dataset_group_id", Long.parseLong(newDatasetId));
        CoreDatasetTable existingTable = coreDatasetTableMapper.selectOne(tableQuery);

        String newTableId;
        if (existingTable != null) {
            existingTable.setTableName(backupTable.getTableName());
            existingTable.setDatasourceId(newDatasourceId);
            existingTable.setType(backupTable.getType());
            existingTable.setInfo(backupTable.getInfo());
            existingTable.setSqlVariableDetails(backupTable.getSqlVariableDetails());
            coreDatasetTableMapper.updateById(existingTable);
            newTableId = String.valueOf(existingTable.getId());
        } else {
            CoreDatasetTable newTable = new CoreDatasetTable();
            newTable.setName(backupTable.getName());
            newTable.setTableName(backupTable.getTableName());
            newTable.setDatasourceId(newDatasourceId);
            newTable.setDatasetGroupId(Long.parseLong(newDatasetId));
            newTable.setType(backupTable.getType());
            newTable.setInfo(backupTable.getInfo());
            newTable.setSqlVariableDetails(backupTable.getSqlVariableDetails());
            coreDatasetTableMapper.insert(newTable);
            newTableId = String.valueOf(newTable.getId());
        }

        // 更新 table ID 映射
        tableIdMapping.put(backupTable.getId(), newTableId);

        // 3. 导入 fields
        if (backupTable.getFields() != null) {
            for (BackupDatasetTableField backupField : backupTable.getFields()) {
                importDatasetTableField(backupField, newTableId, newDatasetId, newDatasourceId, tableIdMapping);
            }
        }
    }

    private void importDatasetTableField(BackupDatasetTableField backupField, String newTableId,
                                          String newDatasetId, Long newDatasourceId, Map<String, String> tableIdMapping) {
        // 查询是否已存在（按 dataeaseName 和 dataset_table_id）
        QueryWrapper<CoreDatasetTableField> fieldQuery = new QueryWrapper<>();
        fieldQuery.eq("dataease_name", backupField.getDataeaseName())
                  .eq("dataset_table_id", Long.parseLong(newTableId));
        CoreDatasetTableField existingField = coreDatasetTableFieldMapper.selectOne(fieldQuery);

        if (existingField != null) {
            // 覆盖
            existingField.setDatasetGroupId(Long.parseLong(newDatasetId));
            existingField.setDatasourceId(newDatasourceId);
            existingField.setOriginName(backupField.getOriginName());
            existingField.setName(backupField.getName());
            existingField.setFieldShortName(backupField.getFieldShortName());
            existingField.setGroupType(backupField.getGroupType());
            existingField.setType(backupField.getType());
            existingField.setSize(backupField.getSize());
            existingField.setDeType(backupField.getDeType());
            existingField.setDeExtractType(backupField.getDeExtractType());
            existingField.setExtField(backupField.getExtField());
            existingField.setChecked(backupField.getChecked());
            existingField.setColumnIndex(backupField.getColumnIndex());
            existingField.setAccuracy(backupField.getAccuracy());
            existingField.setDateFormat(backupField.getDateFormat());
            existingField.setDateFormatType(backupField.getDateFormatType());
            existingField.setParams(backupField.getParams());
            existingField.setOrderChecked(backupField.getOrderChecked());
            existingField.setGroupList(backupField.getGroupList());
            existingField.setOtherGroup(backupField.getOtherGroup());
            coreDatasetTableFieldMapper.updateById(existingField);
        } else {
            // 新建
            CoreDatasetTableField newField = new CoreDatasetTableField();
            newField.setDatasetGroupId(Long.parseLong(newDatasetId));
            newField.setDatasourceId(newDatasourceId);
            newField.setDatasetTableId(Long.parseLong(newTableId));
            newField.setOriginName(backupField.getOriginName());
            newField.setName(backupField.getName());
            newField.setDataeaseName(backupField.getDataeaseName());
            newField.setFieldShortName(backupField.getFieldShortName());
            newField.setGroupType(backupField.getGroupType());
            newField.setType(backupField.getType());
            newField.setSize(backupField.getSize());
            newField.setDeType(backupField.getDeType());
            newField.setDeExtractType(backupField.getDeExtractType());
            newField.setExtField(backupField.getExtField());
            newField.setChecked(backupField.getChecked());
            newField.setColumnIndex(backupField.getColumnIndex());
            newField.setAccuracy(backupField.getAccuracy());
            newField.setDateFormat(backupField.getDateFormat());
            newField.setDateFormatType(backupField.getDateFormatType());
            newField.setParams(backupField.getParams());
            newField.setOrderChecked(backupField.getOrderChecked());
            newField.setGroupList(backupField.getGroupList());
            newField.setOtherGroup(backupField.getOtherGroup());
            coreDatasetTableFieldMapper.insert(newField);
        }
    }

    /**
     * 导入数据集列表（包含目录创建）
     */
    @Override
    public void importDatasets(List<BackupDataset> datasets, List<BackupFolder> folders, boolean overwrite, Map<String, String> idMapping, Map<String, String> datasourceIdMapping) {
        if (datasets == null || datasets.isEmpty()) {
            return;
        }

        // 按level排序，先创建低级目录
        Map<String, Long> folderMapping = new HashMap<>();
        if (folders != null && !folders.isEmpty()) {
            List<BackupFolder> sortedFolders = folders.stream()
                .sorted(java.util.Comparator.comparingInt(f -> f.getLevel() != null ? f.getLevel() : 0))
                .toList();

            for (BackupFolder folder : sortedFolders) {
                createOrFindDatasetFolder(folder, folderMapping);
            }
        }

        // 批量查询数据源名称，消除 N+1 查询
        Map<String, Long> datasourceNameToId = batchQueryDatasourceNames(datasets);

        // 导入数据集（跳过 nodeType = "folder" 的记录，它们已在 folders 中处理）
        for (BackupDataset dataset : datasets) {
            if ("folder".equals(dataset.getNodeType())) {
                continue;
            }
            try {
                String originalId = dataset.getId();
                String newId = importDatasetWithTablesAndFolders(dataset, overwrite, idMapping, folderMapping, datasourceIdMapping, datasourceNameToId);
                idMapping.put(originalId, newId);
            } catch (Exception e) {
                LogUtil.getLogger().error("Import dataset failed: " + dataset.getName(), e);
            }
        }
    }

    /**
     * 收集所有数据集中引用的数据源名称，一次批量查询
     */
    private Map<String, Long> batchQueryDatasourceNames(List<BackupDataset> datasets) {
        Set<String> datasourceNames = new HashSet<>();
        for (BackupDataset dataset : datasets) {
            if (dataset.getTables() != null) {
                for (BackupDatasetTable table : dataset.getTables()) {
                    if (table.getDatasourceName() != null) {
                        datasourceNames.add(table.getDatasourceName());
                    }
                }
            }
        }
        Map<String, Long> result = new HashMap<>();
        if (datasourceNames.isEmpty()) {
            return result;
        }
        QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("name", datasourceNames);
        List<CoreDatasource> datasources = coreDatasourceMapper.selectList(queryWrapper);
        for (CoreDatasource ds : datasources) {
            result.put(ds.getName(), ds.getId());
        }
        LogUtil.getLogger().info("=== Batch queried {} datasource names, found {} ===", datasourceNames.size(), result.size());
        return result;
    }

    /**
     * 导入数据集及其关联的 tables 和 fields（带文件夹支持）
     */
    private String importDatasetWithTablesAndFolders(BackupDataset dataset, boolean overwrite,
                                                      Map<String, String> idMapping,
                                                      Map<String, Long> folderMapping,
                                                      Map<String, String> datasourceIdMapping,
                                                      Map<String, Long> datasourceNameToId) {
        try {
            QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", dataset.getName()).eq("node_type", "dataset");
            CoreDatasetGroup existing = coreDatasetGroupMapper.selectOne(queryWrapper);

            // 计算新pid：使用目录名称在folderMapping中查找
            Long newPid = 0L;
            if (dataset.getFolderName() != null) {
                String folderKey = dataset.getFolderName() + "_" + (dataset.getParentFolderName() != null ? dataset.getParentFolderName() : "root");
                newPid = folderMapping.getOrDefault(folderKey, 0L);
            }

            String newDatasetId;
            if (existing != null && overwrite) {
                existing.setType(dataset.getType());
                existing.setInfo(dataset.getModel());
                if (dataset.getIsCross() != null) {
                    existing.setIsCross(dataset.getIsCross());
                }
                existing.setPid(newPid);
                coreDatasetGroupMapper.updateById(existing);
                newDatasetId = String.valueOf(existing.getId());
                // 覆盖模式下，先删除原有的 tables 和 fields
                deleteExistingTablesAndFields(Long.parseLong(newDatasetId));
            } else {
                String newName = dataset.getName();
                if (existing != null) {
                    newName = generateUniqueName(dataset.getName());
                }
                CoreDatasetGroup newDs = new CoreDatasetGroup();
                newDs.setName(newName);
                newDs.setPid(newPid);
                newDs.setLevel(dataset.getLevel() != null ? dataset.getLevel() : 0);
                newDs.setNodeType("dataset");
                newDs.setType(dataset.getType());
                newDs.setInfo(dataset.getModel());
                newDs.setUnionSql(dataset.getUnionSql());
                if (dataset.getIsCross() != null) {
                    newDs.setIsCross(dataset.getIsCross());
                }
                newDs.setCreateBy("1");
                newDs.setCreateTime(System.currentTimeMillis());
                coreDatasetGroupMapper.insert(newDs);
                newDs = coreDatasetGroupMapper.selectOne(new QueryWrapper<CoreDatasetGroup>().eq("name", newName).eq("node_type", "dataset"));
                newDatasetId = String.valueOf(newDs.getId());
            }

            // 更新 dataset ID 映射
            idMapping.put(dataset.getId(), newDatasetId);

            // 导入 tables 和 fields
            if (dataset.getTables() != null) {
                Map<String, String> tableIdMapping = new HashMap<>();
                for (BackupDatasetTable backupTable : dataset.getTables()) {
                    importDatasetTable(backupTable, newDatasetId, datasourceIdMapping, tableIdMapping, datasourceNameToId);
                }
            }

            LogUtil.getLogger().info("=== Backup import dataset: id={}, name={}, newId={} ===", dataset.getId(), dataset.getName(), newDatasetId);
            return newDatasetId;
        } catch (Exception e) {
            LogUtil.getLogger().error("Import dataset failed: " + dataset.getName(), e);
            throw e;
        }
    }

    /**
     * 按 name + parentId 查找目录
     */
    private CoreDatasetGroup findDatasetFolderByNameAndParent(String name, Long parentId) {
        QueryWrapper<CoreDatasetGroup> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name).eq("node_type", "folder");
        if (parentId != null && parentId != 0L) {
            queryWrapper.eq("pid", parentId);
        } else {
            // 根目录：精确匹配 pid = 0（不包括 NULL，避免匹配多条记录）
            queryWrapper.eq("pid", 0L);
        }
        // 查找单条记录，如果有多条则取第一条（避免数据库中有重复数据的问题）
        List<CoreDatasetGroup> list = coreDatasetGroupMapper.selectList(queryWrapper);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }

    /**
     * 创建或查找目录，返回目录ID
     * 如果目录在目标数据库中已存在，直接使用现有的，不创建新的
     */
    public Long createOrFindDatasetFolder(BackupFolder folder, Map<String, Long> folderMapping) {
        String key = folder.getName() + "_" + (folder.getParentName() != null ? folder.getParentName() : "root");
        LogUtil.getLogger().info("=== createOrFindDatasetFolder: key={}, folderName={}, parentName={} ===", key, folder.getName(), folder.getParentName());
        if (folderMapping.containsKey(key)) {
            LogUtil.getLogger().info("=== createOrFindDatasetFolder: found in folderMapping ===");
            return folderMapping.get(key);
        }

        // 先查找父目录ID
        Long parentId = 0L;
        if (folder.getParentName() != null) {
            // 先在folderMapping中查找父目录
            String parentKey = folder.getParentName() + "_root";
            parentId = folderMapping.getOrDefault(parentKey, 0L);
            LogUtil.getLogger().info("=== createOrFindDatasetFolder: looking for parent, parentKey={}, parentId={} ===", parentKey, parentId);

            // 如果在folderMapping中没找到，尝试在目标数据库中查找
            if (parentId == 0L) {
                CoreDatasetGroup parentFolder = findDatasetFolderByNameAndParent(folder.getParentName(), 0L);
                LogUtil.getLogger().info("=== createOrFindDatasetFolder: find in DB, parentFolder={} ===", parentFolder);
                if (parentFolder != null) {
                    parentId = parentFolder.getId();
                    folderMapping.put(parentKey, parentId);
                }
            }
        }

        // 查找是否已存在（按名称和父目录ID查找）
        CoreDatasetGroup existing = findDatasetFolderByNameAndParent(folder.getName(), parentId);
        LogUtil.getLogger().info("=== createOrFindDatasetFolder: finding existing, name={}, parentId={}, existing={} ===", folder.getName(), parentId, existing);
        if (existing != null) {
            folderMapping.put(key, existing.getId());
            // 同时存储oldFolderId -> newFolderId的映射
            if (folder.getId() != null) {
                folderMapping.put("id_" + folder.getId(), existing.getId());
            }
            LogUtil.getLogger().info("=== createOrFindDatasetFolder: reusing existing id={} ===", existing.getId());
            return existing.getId();
        }

        // 创建新目录
        CoreDatasetGroup newFolder = new CoreDatasetGroup();
        newFolder.setName(folder.getName());
        newFolder.setPid(parentId);
        newFolder.setLevel(folder.getLevel() != null ? folder.getLevel() : 0);
        newFolder.setNodeType("folder");
        newFolder.setCreateBy("1");
        newFolder.setCreateTime(System.currentTimeMillis());
        coreDatasetGroupMapper.insert(newFolder);

        // 重新查询获取ID
        newFolder = coreDatasetGroupMapper.selectOne(new QueryWrapper<CoreDatasetGroup>()
            .eq("name", folder.getName())
            .eq("node_type", "folder")
            .eq("pid", parentId));

        folderMapping.put(key, newFolder.getId());
        // 同时存储oldFolderId -> newFolderId的映射
        if (folder.getId() != null) {
            folderMapping.put("id_" + folder.getId(), newFolder.getId());
        }
        return newFolder.getId();
    }
}
