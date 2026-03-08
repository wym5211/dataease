package io.dataease.extensions.view.dto;

import io.dataease.extensions.datasource.dto.DatasetTableFieldDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @Author Junjun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ChartFieldCustomFilterDTO extends ChartViewFieldBaseDTO {
    private List<ChartCustomFilterItemDTO> filter;
    private DatasetTableFieldDTO field;
    private List<String> enumCheckField;
}
