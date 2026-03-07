package io.dataease.api.ds.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteExcelRequest extends ExcelConfiguration {
    private Long datasourceId;
    private int editType;
}
