package io.dataease.api.sync.datasource.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GetDatasourceRequest extends SyncDatasourceDTO {

    /**
     * 查询sql
     */
    private String query;
    /**
     * 表名
     */
    private String table;
    /**
     * 表格的抽取数据方式
     */
    private boolean tableExtract;

    private String targetDbId;

}
