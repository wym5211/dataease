package io.dataease.model.backup;

import lombok.Data;

@Data
public class BackupOptions {

    /**
     * 是否包含关联的数据源
     */
    private boolean includeDatasource = true;

    /**
     * 是否包含关联的数据集
     */
    private boolean includeDataset = true;

    /**
     * 是否包含图表配置
     */
    private boolean includeChart = true;

    /**
     * 是否包含样式配置
     */
    private boolean includeStyle = true;

    /**
     * 是否包含权限配置
     */
    private boolean includePermission = false;

    /**
     * 是否压缩文件
     */
    private boolean compress = true;
}
