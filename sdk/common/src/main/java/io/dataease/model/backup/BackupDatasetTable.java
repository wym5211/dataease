package io.dataease.model.backup;

import lombok.Data;
import java.util.List;

/**
 * 数据集物理表备份信息
 */
@Data
public class BackupDatasetTable {

    private String id;
    private String name;
    private String tableName;
    private String datasourceId;   // 原始ID，备用
    private String datasourceName; // 数据源名称，导入时按名称匹配
    private String type;
    private String info;
    private String sqlVariableDetails;
    private List<BackupDatasetTableField> fields;
}
