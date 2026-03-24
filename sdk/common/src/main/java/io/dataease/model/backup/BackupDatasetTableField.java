package io.dataease.model.backup;

import lombok.Data;

/**
 * 数据集表字段备份信息
 */
@Data
public class BackupDatasetTableField {

    private String id;
    private String originName;
    private String name;
    private String dataeaseName;
    private String fieldShortName;
    private String groupType;
    private String type;
    private Integer size;
    private Integer deType;
    private Integer deExtractType;
    private Integer extField;
    private Boolean checked;
    private Integer columnIndex;
    private Integer accuracy;
    private String dateFormat;
    private String dateFormatType;
    private String params;
    private Boolean orderChecked;
    private String groupList;
    private String otherGroup;
}
