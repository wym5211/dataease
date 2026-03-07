package io.dataease.api.dataset.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author Junjun
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DataSetExportRequest extends DatasetNodeDTO {
    private String filename;
    private String expressionTree;
    private boolean dataEaseBi;
}
