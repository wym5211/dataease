package io.dataease.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.backup.service.BackupDatasourceService;
import io.dataease.datasource.dao.auto.entity.CoreDatasource;
import io.dataease.datasource.dao.auto.mapper.CoreDatasourceMapper;
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

    private Logger logger = LogUtil.getLogger();

    private static final int MAX_WAIT_RETRIES = 10;
    private static final long WAIT_INTERVAL_MS = 500;

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
                existing.setDescription(datasource.getDescription());
                existing.setType(datasource.getType());
                existing.setConfiguration(datasource.getConfiguration());
                existing.setUpdateTime(System.currentTimeMillis());
                coreDatasourceMapper.updateById(existing);

                // 更新Calcite连接池
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, existing);
                logger.info("=== Backup import (overwrite): updating datasource to calcite, id={}, name={}, type={} ===", existing.getId(), existing.getName(), existing.getType());
                calciteProvider.update(datasourceDTO);
                // 等待异步schema更新完成
                waitForSchemaReady(existing.getId());
                logger.info("=== Backup import (overwrite): calcite update completed ===");

                return String.valueOf(existing.getId());
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

                // MyBatis-Plus insert后需要重新查询获取带ID的完整实体
                newDs = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>().eq("name", newName));

                // 注册Calcite连接池
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, newDs);
                logger.info("=== Backup import: registering datasource, id={}, name={}, type={} ===", newDs.getId(), newDs.getName(), newDs.getType());
                try {
                    calciteProvider.update(datasourceDTO);
                    // 等待异步schema构建完成
                    waitForSchemaReady(newDs.getId());
                    logger.info("=== Backup import: calcite registration completed ===");
                } catch (Exception e) {
                    logger.error("=== Backup import: calcite registration failed ===", e);
                    throw e;
                }

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

    private void waitForSchemaReady(Long datasourceId) {
        // calciteProvider.update是异步的，需要等待schema构建完成
        // 等待最多5秒，每500ms检查一次
        for (int i = 0; i < MAX_WAIT_RETRIES; i++) {
            try {
                Thread.sleep(WAIT_INTERVAL_MS);
                logger.debug("=== Waiting for schema to be ready for datasource {}, attempt {} ===", datasourceId, i + 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.info("=== Waited {} ms for schema to be ready for datasource {} ===", MAX_WAIT_RETRIES * WAIT_INTERVAL_MS, datasourceId);
    }

    /**
     * Collect all datasource folders (pid != 0) and build folder chain for exported datasources
     */
    public List<BackupFolder> collectDatasourceFolders() {
        List<BackupFolder> result = new ArrayList<>();
        Map<String, BackupFolder> folderMap = new HashMap<>();
        try {
            // Query all datasources (not just root) to collect their parent folders
            List<CoreDatasource> allDatasources = coreDatasourceMapper.selectList(null);

            for (CoreDatasource ds : allDatasources) {
                if (ds.getPid() != null && ds.getPid() != 0L) {
                    result.addAll(collectDatasourceParentFolders(ds.getId(), folderMap, 1));
                }
            }
        } catch (Exception e) {
            logger.error("Collect datasource folders failed", e);
        }
        return result;
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
     * 按 name + parentName 查找目录
     */
    private CoreDatasource findDatasourceFolderByNameAndParent(String name, Long parentId) {
        QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", name).eq("type", "folder");
        if (parentId != null && parentId != 0L) {
            queryWrapper.eq("pid", parentId);
        } else {
            queryWrapper.and(w -> w.eq("pid", 0L).or().isNull("pid"));
        }
        return coreDatasourceMapper.selectOne(queryWrapper);
    }

    /**
     * 创建或查找目录，返回目录ID
     */
    public Long createOrFindDatasourceFolder(BackupFolder folder, Map<String, Long> folderMapping) {
        String key = folder.getName() + "_" + (folder.getParentName() != null ? folder.getParentName() : "root");
        if (folderMapping.containsKey(key)) {
            return folderMapping.get(key);
        }

        // 先查找父目录ID
        Long parentId = 0L;
        if (folder.getParentName() != null) {
            // 通过父目录名称查找父目录
            String parentKey = folder.getParentName() + "_root";
            parentId = folderMapping.getOrDefault(parentKey, 0L);
        }

        // 查找是否已存在
        CoreDatasource existing = findDatasourceFolderByNameAndParent(folder.getName(), parentId);
        if (existing != null) {
            folderMapping.put(key, existing.getId());
            // 同时存储oldFolderId -> newFolderId的映射
            if (folder.getId() != null) {
                folderMapping.put("id_" + folder.getId(), existing.getId());
            }
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
        if (datasources == null || datasources.isEmpty()) {
            return;
        }

        // 按level排序，先创建低级目录
        Map<String, Long> folderMapping = new HashMap<>();
        if (folders != null && !folders.isEmpty()) {
            List<BackupFolder> sortedFolders = folders.stream()
                .sorted(Comparator.comparingInt(f -> f.getLevel() != null ? f.getLevel() : 0))
                .toList();

            for (BackupFolder folder : sortedFolders) {
                createOrFindDatasourceFolder(folder, folderMapping);
            }
        }

        // 导入数据源
        for (BackupDatasource datasource : datasources) {
            try {
                String originalId = datasource.getId();
                String newId = importDatasource(datasource, overwrite, idMapping, folderMapping);
                idMapping.put(originalId, newId);
            } catch (Exception e) {
                logger.error("Import datasource failed: " + datasource.getName(), e);
            }
        }
    }

    /**
     * 导入数据源（带ID映射表和文件夹映射）
     */
    private String importDatasource(BackupDatasource datasource, boolean overwrite, Map<String, String> idMapping, Map<String, Long> folderMapping) {
        try {
            QueryWrapper<CoreDatasource> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", datasource.getName());
            CoreDatasource existing = coreDatasourceMapper.selectOne(queryWrapper);

            // 计算新pid：使用文件夹映射将原pid转换为新pid
            Long newPid = 0L;
            if (datasource.getPid() != null && datasource.getPid() != 0L) {
                // 在folderMapping中查找原pid对应的新pid
                newPid = folderMapping.getOrDefault("id_" + datasource.getPid(), 0L);
            }

            if (existing != null && overwrite) {
                existing.setDescription(datasource.getDescription());
                existing.setType(datasource.getType());
                existing.setConfiguration(datasource.getConfiguration());
                existing.setUpdateTime(System.currentTimeMillis());
                coreDatasourceMapper.updateById(existing);

                // 更新Calcite连接池
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, existing);
                logger.info("=== Backup import (overwrite): updating datasource to calcite, id={}, name={}, type={} ===", existing.getId(), existing.getName(), existing.getType());
                calciteProvider.update(datasourceDTO);
                // 等待异步schema更新完成
                waitForSchemaReady(existing.getId());
                logger.info("=== Backup import (overwrite): calcite update completed ===");

                return String.valueOf(existing.getId());
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
                newDs.setPid(newPid);
                newDs.setEditType("1");
                newDs.setCreateTime(System.currentTimeMillis());
                newDs.setUpdateTime(System.currentTimeMillis());
                coreDatasourceMapper.insert(newDs);

                // MyBatis-Plus insert后需要重新查询获取带ID的完整实体
                newDs = coreDatasourceMapper.selectOne(new QueryWrapper<CoreDatasource>().eq("name", newName));

                // 注册Calcite连接池
                DatasourceDTO datasourceDTO = new DatasourceDTO();
                BeanUtils.copyBean(datasourceDTO, newDs);
                logger.info("=== Backup import: registering datasource, id={}, name={}, type={} ===", newDs.getId(), newDs.getName(), newDs.getType());
                try {
                    calciteProvider.update(datasourceDTO);
                    // 等待异步schema构建完成
                    waitForSchemaReady(newDs.getId());
                    logger.info("=== Backup import: calcite registration completed ===");
                } catch (Exception e) {
                    logger.error("=== Backup import: calcite registration failed ===", e);
                    throw e;
                }

                return String.valueOf(newDs.getId());
            }
        } catch (Exception e) {
            logger.error("Import datasource failed: " + datasource.getName(), e);
            throw e;
        }
    }
}
