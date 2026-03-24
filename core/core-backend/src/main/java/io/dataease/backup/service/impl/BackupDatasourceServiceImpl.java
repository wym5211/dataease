package io.dataease.backup.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.dataease.backup.service.BackupDatasourceService;
import io.dataease.datasource.dao.auto.entity.CoreDatasource;
import io.dataease.datasource.dao.auto.mapper.CoreDatasourceMapper;
import io.dataease.extensions.datasource.dto.DatasourceDTO;
import io.dataease.datasource.provider.CalciteProvider;
import io.dataease.model.backup.BackupDatasource;
import io.dataease.utils.BeanUtils;
import io.dataease.utils.LogUtil;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
}
