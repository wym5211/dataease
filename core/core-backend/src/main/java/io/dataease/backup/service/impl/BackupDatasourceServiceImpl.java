package io.dataease.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.backup.service.BackupDatasourceService;
import io.dataease.datasource.dao.auto.entity.CoreDatasource;
import io.dataease.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.dataease.datasource.manage.DataSourceManage;
import io.dataease.extensions.datasource.dto.DatasourceDTO;
import io.dataease.datasource.provider.CalciteProvider;
import io.dataease.model.backup.BackupDatasource;
import io.dataease.model.backup.BackupFolder;
import io.dataease.utils.BeanUtils;
import io.dataease.utils.LogUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackupDatasourceServiceImpl implements BackupDatasourceService {

    @Autowired
    private CoreDatasourceMapper coreDatasourceMapper;

    @Autowired
    private CalciteProvider calciteProvider;

    @Autowired
    private DataSourceManage dataSourceManage;

    private Logger logger = LogUtil.getLogger();

    private static final int MAX_WAIT_RETRIES = 10;
    private static final long WAIT_INTERVAL_MS = 500;

    @Override
    public List<BackupDatasource> exportDatasources() {
        return exportDatasources(null);
    }

    @Override
    public List<BackupDatasource> exportDatasources(List<String> ids) {
        List<BackupDatasource> result = new ArrayList<>();
        try {
            QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
            // 如果指定了 ids，则按 ID 列表过滤；否则只导出根目录的数据源
            if (ids != null && !ids.isEmpty()) {
                queryWrapper.in("id", ids);
            } else {
                queryWrapper.eq("pid", 0);
            }
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
                backup.setPid(ds.getPid());
                result.add(backup);
            }
        } catch (Exception e) {
            logger.error("Export datasources failed", e);
        }
        return result;
    }

    @Override
    public String importDatasource(BackupDatasource datasource, boolean overwrite) {
        try {
            QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", datasource.getName());
            CoreDatasource existing = coreDatasourceMapper.selectOne(queryWrapper);

            if (existing != null && overwrite) {
                // 覆盖模式：更新现有数据源
                existing.setDescription(datasource.getDescription());
                existing.setType(datasource.getType());
                existing.setConfiguration(datasource.getConfiguration());
                existing.setUpdateTime(System.currentTimeMillis());
                coreDatasourceMapper.updateById(existing);

                // 更新Calcite连接池
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, existing);
                logger.info("=== Backup import (overwrite): updating datasource, id={}, name={} ===", existing.getId(), existing.getName());
                calciteProvider.update(datasourceDTO);
                waitForSchemaReady(existing);

                return String.valueOf(existing.getId());
            } else if (existing != null) {
                // 非覆盖模式：重命名后创建新数据源
                String newName = generateUniqueName(datasource.getName());
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

                newDs = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>().eq("name", newName));

                // 注册Calcite连接池
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, newDs);
                logger.info("=== Backup import: registering datasource (renamed), id={}, name={} ===", newDs.getId(), newDs.getName());
                calciteProvider.update(datasourceDTO);
                waitForSchemaReady(newDs);

                return String.valueOf(newDs.getId());
            } else {
                // 不存在：直接创建
                CoreDatasource newDs = new CoreDatasource();
                newDs.setName(datasource.getName());
                newDs.setDescription(datasource.getDescription());
                newDs.setType(datasource.getType());
                newDs.setConfiguration(datasource.getConfiguration());
                newDs.setPid(0L);
                newDs.setEditType("1");
                newDs.setCreateTime(System.currentTimeMillis());
                newDs.setUpdateTime(System.currentTimeMillis());
                coreDatasourceMapper.insert(newDs);

                newDs = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>().eq("name", datasource.getName()));

                // 注册Calcite连接池
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, newDs);
                logger.info("=== Backup import: registering datasource, id={}, name={} ===", newDs.getId(), newDs.getName());
                calciteProvider.update(datasourceDTO);
                waitForSchemaReady(newDs);

                return String.valueOf(newDs.getId());
            }
        } catch (Exception e) {
            logger.error("Import datasource failed: " + datasource.getName(), e);
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

    private void waitForSchemaReady(CoreDatasource coreDatasource) {
        Long datasourceId = coreDatasource.getId();
        // calciteProvider.update是异步的，需要等待schema构建完成
        // 等待最多5秒，每500ms检查一次，实际验证schema是否准备好
        for (int i = 0; i < MAX_WAIT_RETRIES; i++) {
            try {
                Thread.sleep(WAIT_INTERVAL_MS);
                if (calciteProvider.isSchemaReady(datasourceId)) {
                    logger.info("=== Schema is ready for datasource {} after {} ms ===", datasourceId, (i + 1) * WAIT_INTERVAL_MS);
                    coreDatasource.setStatus("Success");
                    dataSourceManage.innerEditStatus(coreDatasource);
                    return;
                }
                logger.debug("=== Waiting for schema to be ready for datasource {}, attempt {} ===", datasourceId, i + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warn("=== Error checking schema status for datasource {}, attempt {}: {} ===", datasourceId, i + 1, e.getMessage());
            }
        }
        logger.warn("=== Schema may not be ready for datasource {} after {} ms ===", datasourceId, MAX_WAIT_RETRIES * WAIT_INTERVAL_MS);
        coreDatasource.setStatus("Error");
        dataSourceManage.innerEditStatus(coreDatasource);
    }

    /**
     * Collect all datasource folders (pid != 0) and build folder chain for exported datasources
     */
    @Override
    public List<BackupFolder> collectDatasourceFolders() {
        return collectDatasourceFolders(null);
    }

    /**
     * Collect datasource folders for specified datasource IDs
     */
    @Override
    public List<BackupFolder> collectDatasourceFolders(List<String> ids) {
        List<BackupFolder> result = new ArrayList<>();
        Map<String, BackupFolder> folderMap = new HashMap<>();
        try {
            // Query all folders (type = "folder")
            QueryWrapper<CoreDatasource> folderQuery = new QueryWrapper<>();
            folderQuery.eq("type", "folder");
            List<CoreDatasource> allFolders = coreDatasourceMapper.selectList(folderQuery);

            // Collect folders that contain datasources (for parent folder chain)
            List<CoreDatasource> allDatasources;
            if (ids != null && !ids.isEmpty()) {
                QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
                queryWrapper.in("id", ids);
                allDatasources = coreDatasourceMapper.selectList(queryWrapper);
            } else {
                allDatasources = coreDatasourceMapper.selectList(null);
            }

            for (CoreDatasource ds : allDatasources) {
                if ("folder".equals(ds.getType())) {
                    // 这是一个文件夹，添加到结果中
                    String key = ds.getName() + "_" + (ds.getPid() != null && ds.getPid() != 0L ?
                        getFolderNameById(ds.getPid()) : "root");
                    if (!folderMap.containsKey(key)) {
                        BackupFolder bf = new BackupFolder();
                        bf.setId(String.valueOf(ds.getId()));
                        bf.setName(ds.getName());
                        bf.setPid(ds.getPid());
                        bf.setLevel(1);
                        bf.setNodeType("folder");
                        bf.setResourceType("datasource");
                        if (ds.getPid() != null && ds.getPid() != 0L) {
                            bf.setParentName(getFolderNameById(ds.getPid()));
                        }
                        folderMap.put(key, bf);
                        result.add(bf);
                    }
                } else if (ds.getPid() != null && ds.getPid() != 0L) {
                    // 这是一个数据源，收集其父文件夹
                    result.addAll(collectDatasourceParentFolders(ds.getId(), folderMap, 1));
                }
            }

            // Also add all folders directly (including empty folders) when not filtering by ids
            if (ids == null || ids.isEmpty()) {
                for (CoreDatasource folder : allFolders) {
                    String key = folder.getName() + "_" + (folder.getPid() != null && folder.getPid() != 0L ?
                        getFolderNameById(folder.getPid()) : "root");
                    if (!folderMap.containsKey(key)) {
                        BackupFolder bf = new BackupFolder();
                        bf.setId(String.valueOf(folder.getId()));
                        bf.setName(folder.getName());
                        bf.setPid(folder.getPid());
                        bf.setLevel(1);
                        bf.setNodeType("folder");
                        bf.setResourceType("datasource");
                        if (folder.getPid() != null && folder.getPid() != 0L) {
                            bf.setParentName(getFolderNameById(folder.getPid()));
                        }
                        folderMap.put(key, bf);
                        result.add(bf);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Collect datasource folders failed", e);
        }
        return result;
    }

    private String getFolderNameById(Long folderId) {
        if (folderId == null || folderId == 0L) {
            return null;
        }
        CoreDatasource folder = coreDatasourceMapper.selectById(folderId);
        return folder != null ? folder.getName() : null;
    }

    /**
     * Recursively collect parent folders for a datasource
     */
    private List<BackupFolder> collectDatasourceParentFolders(Long datasourceId, Map<String, BackupFolder> folderMap, int currentLevel) {
        List<BackupFolder> result = new ArrayList<>();
        CoreDatasource ds = coreDatasourceMapper.selectById(datasourceId);
        if (ds == null || ds.getPid() == null || ds.getPid() == 0L) {
            return result;
        }
        CoreDatasource parent = coreDatasourceMapper.selectById(ds.getPid());
        if (parent == null) {
            return result;
        }
        if ("folder".equals(parent.getType())) {
            String key = parent.getName() + "_" + (parent.getPid() != null && parent.getPid() != 0L ?
                coreDatasourceMapper.selectById(parent.getPid()).getName() : "root");
            if (folderMap.containsKey(key)) {
                return result;
            }
            BackupFolder folder = new BackupFolder();
            folder.setId(String.valueOf(parent.getId()));
            folder.setName(parent.getName());
            folder.setPid(parent.getPid());
            folder.setLevel(currentLevel);
            folder.setNodeType("folder");
            folder.setResourceType("datasource");
            // 设置父目录名称用于跨环境匹配
            if (parent.getPid() != null && parent.getPid() != 0L) {
                CoreDatasource grandParent = coreDatasourceMapper.selectById(parent.getPid());
                if (grandParent != null) {
                    folder.setParentName(grandParent.getName());
                }
            }
            folderMap.put(key, folder);
            result.add(folder);
            result.addAll(collectDatasourceParentFolders(parent.getId(), folderMap, currentLevel + 1));
        }
        return result;
    }

    /**
     * 按 name + parentId 查找目录
     */
    private CoreDatasource findDatasourceFolderByNameAndParent(String name, Long parentId) {
        QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name).eq("type", "folder");
        if (parentId != null && parentId != 0L) {
            queryWrapper.eq("pid", parentId);
        } else {
            // 根目录：同时匹配 pid = 0 和 pid IS NULL，因为不同系统/版本存储方式可能不同
            queryWrapper.and(w -> w.eq("pid", 0L).or().isNull("pid"));
        }
        return coreDatasourceMapper.selectOne(queryWrapper);
    }

    /**
     * 创建或查找目录，返回目录ID
     * 如果目录在目标数据库中已存在，直接使用现有的，不创建新的
     */
    public Long createOrFindDatasourceFolder(BackupFolder folder, Map<String, Long> folderMapping) {
        String key = folder.getName() + "_" + (folder.getParentName() != null ? folder.getParentName() : "root");
        logger.info("=== createOrFindDatasourceFolder: key={}, folderName={}, parentName={} ===", key, folder.getName(), folder.getParentName());
        if (folderMapping.containsKey(key)) {
            logger.info("=== createOrFindDatasourceFolder: found in folderMapping, returning existing id ===");
            return folderMapping.get(key);
        }

        // 先查找父目录ID
        Long parentId = 0L;
        if (folder.getParentName() != null) {
            // 先在folderMapping中查找父目录
            String parentKey = folder.getParentName() + "_root";
            parentId = folderMapping.getOrDefault(parentKey, 0L);
            logger.info("=== createOrFindDatasourceFolder: looking for parent, parentKey={}, parentId={} ===", parentKey, parentId);

            // 如果在folderMapping中没找到，尝试在目标数据库中查找
            if (parentId == 0L) {
                CoreDatasource parentFolder = findDatasourceFolderByNameAndParent(folder.getParentName(), 0L);
                logger.info("=== createOrFindDatasourceFolder: find in DB, parentFolder={} ===", parentFolder);
                if (parentFolder != null) {
                    parentId = parentFolder.getId();
                    folderMapping.put(parentKey, parentId);
                }
            }
        }

        // 查找是否已存在（按名称和父目录ID查找）
        CoreDatasource existing = findDatasourceFolderByNameAndParent(folder.getName(), parentId);
        logger.info("=== createOrFindDatasourceFolder: finding existing folder, name={}, parentId={}, existing={} ===", folder.getName(), parentId, existing);
        if (existing != null) {
            folderMapping.put(key, existing.getId());
            // 同时存储oldFolderId -> newFolderId的映射
            if (folder.getId() != null) {
                folderMapping.put("id_" + folder.getId(), existing.getId());
            }
            logger.info("=== createOrFindDatasourceFolder: reusing existing folder id={} ===", existing.getId());
            return existing.getId();
        }

        // 创建新目录
        CoreDatasource newFolder = new CoreDatasource();
        newFolder.setName(folder.getName());
        newFolder.setPid(parentId);
        newFolder.setType("folder");
        newFolder.setEditType("1");
        newFolder.setCreateTime(System.currentTimeMillis());
        newFolder.setUpdateTime(System.currentTimeMillis());
        coreDatasourceMapper.insert(newFolder);

        // 重新查询获取ID
        newFolder = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>()
            .eq("name", folder.getName())
            .eq("type", "folder")
            .eq("pid", parentId));

        folderMapping.put(key, newFolder.getId());
        // 同时存储oldFolderId -> newFolderId的映射
        if (folder.getId() != null) {
            folderMapping.put("id_" + folder.getId(), newFolder.getId());
        }
        return newFolder.getId();
    }

    @Override
    public void importDatasources(List<BackupDatasource> datasources, List<BackupFolder> folders, boolean overwrite, Map<String, String> idMapping) {
        logger.info("=== importDatasources called: datasources count={}, folders count={} ===", datasources != null ? datasources.size() : 0, folders != null ? folders.size() : 0);
        if (datasources == null || datasources.isEmpty()) {
            return;
        }

        // 按level排序，先创建低级目录
        Map<String, Long> folderMapping = new HashMap<>();
        if (folders != null && !folders.isEmpty()) {
            logger.info("=== importDatasources: processing {} folders ===", folders.size());
            List<BackupFolder> sortedFolders = folders.stream()
                .sorted(Comparator.comparingInt(f -> f.getLevel() != null ? f.getLevel() : 0))
                .toList();

            for (BackupFolder folder : sortedFolders) {
                createOrFindDatasourceFolder(folder, folderMapping);
            }
        } else {
            logger.info("=== importDatasources: folders is null or empty ===");
        }

        // 第一阶段：创建所有数据源（DB操作，串行），收集需要注册 Calcite 的数据源
        List<CoreDatasource> pendingCalcite = new ArrayList<>();
        for (BackupDatasource datasource : datasources) {
            if ("folder".equals(datasource.getType())) {
                continue;
            }
            try {
                String originalId = datasource.getId();
                CoreDatasource created = createDatasourceInDb(datasource, overwrite, idMapping, folderMapping);
                idMapping.put(originalId, String.valueOf(created.getId()));
                pendingCalcite.add(created);
            } catch (Exception e) {
                logger.error("Import datasource failed: " + datasource.getName(), e);
            }
        }

        // 第二阶段：并行注册 Calcite schema + 统一等待
        if (!pendingCalcite.isEmpty()) {
            registerCalciteSchemasParallel(pendingCalcite);
        }
    }

    /**
     * 仅创建数据源到数据库，不注册 Calcite
     */
    private CoreDatasource createDatasourceInDb(BackupDatasource datasource, boolean overwrite, Map<String, String> idMapping, Map<String, Long> folderMapping) {
        QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", datasource.getName());
        CoreDatasource existing = coreDatasourceMapper.selectOne(queryWrapper);

        Long newPid = 0L;
        if (datasource.getPid() != null && datasource.getPid() != 0L) {
            newPid = folderMapping.getOrDefault("id_" + datasource.getPid(), 0L);
        }

        if (existing != null && overwrite) {
            existing.setDescription(datasource.getDescription());
            existing.setType(datasource.getType());
            existing.setConfiguration(datasource.getConfiguration());
            existing.setPid(newPid);
            existing.setUpdateTime(System.currentTimeMillis());
            coreDatasourceMapper.updateById(existing);
            logger.info("=== Backup import (overwrite): updated datasource in DB, id={}, name={} ===", existing.getId(), existing.getName());
            return existing;
        } else if (existing != null) {
            String newName = generateUniqueName(datasource.getName());
            CoreDatasource newDs = new CoreDatasource();
            newDs.setName(newName);
            newDs.setDescription(datasource.getDescription());
            newDs.setType(datasource.getType());
            newDs.setConfiguration(datasource.getConfiguration());
            newDs.setPid(newPid);
            newDs.setEditType("1");
            newDs.setCreateTime(System.currentTimeMillis());
            newDs.setUpdateTime(System.currentTimeMillis());
            coreDatasourceMapper.insert(newDs);
            newDs = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>().eq("name", newName));
            logger.info("=== Backup import: created datasource in DB (renamed), id={}, name={} ===", newDs.getId(), newDs.getName());
            return newDs;
        } else {
            CoreDatasource newDs = new CoreDatasource();
            newDs.setName(datasource.getName());
            newDs.setDescription(datasource.getDescription());
            newDs.setType(datasource.getType());
            newDs.setConfiguration(datasource.getConfiguration());
            newDs.setPid(newPid);
            newDs.setEditType("1");
            newDs.setCreateTime(System.currentTimeMillis());
            newDs.setUpdateTime(System.currentTimeMillis());
            coreDatasourceMapper.insert(newDs);
            newDs = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>().eq("name", datasource.getName()));
            logger.info("=== Backup import: created datasource in DB, id={}, name={}, type={} ===", newDs.getId(), newDs.getName(), newDs.getType());
            return newDs;
        }
    }

    /**
     * 注册所有数据源的 Calcite schema（update 内部异步提交到线程池），
     * 然后统一等待就绪并更新状态
     */
    private void registerCalciteSchemasParallel(List<CoreDatasource> datasources) {
        // 触发所有数据源的 Calcite schema 异步构建
        for (CoreDatasource ds : datasources) {
            try {
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, ds);
                calciteProvider.update(datasourceDTO);
            } catch (Exception e) {
                logger.error("=== Calcite update failed for datasource {}, marking as Error ===", ds.getName(), e);
                ds.setStatus("Error");
                dataSourceManage.innerEditStatus(ds);
            }
        }

        // 等待所有 schema 构建完成（统一等待，所有数据源并行构建）
        for (int i = 0; i < MAX_WAIT_RETRIES; i++) {
            try {
                Thread.sleep(WAIT_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 检查每个数据源的 schema 状态并更新
        for (CoreDatasource ds : datasources) {
            if (ds.getStatus() != null) {
                continue;
            }
            if (calciteProvider.isSchemaReady(ds.getId())) {
                ds.setStatus("Success");
            } else {
                ds.setStatus("Error");
                logger.warn("=== Schema NOT ready for datasource {} ===", ds.getId());
            }
            dataSourceManage.innerEditStatus(ds);
        }
        logger.info("=== Calcite schema registration completed: {} datasources ===", datasources.size());
    }

    /**
     * 导入数据源（带ID映射表和文件夹映射）
     */
    private String importDatasource(BackupDatasource datasource, boolean overwrite, Map<String, String> idMapping, Map<String, Long> folderMapping) {
        CoreDatasource ds = createDatasourceInDb(datasource, overwrite, idMapping, folderMapping);
        DatasourceDTO datasourceDTO = new DatasourceDTO();
        BeanUtils.copyBean(datasourceDTO, ds);
        calciteProvider.update(datasourceDTO);
        waitForSchemaReady(ds);
        return String.valueOf(ds.getId());
    }
}
