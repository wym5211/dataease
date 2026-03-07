package io.dataease.api.chart.dto;

import io.dataease.extensions.datasource.dto.DatasetTableFieldDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DeSortField extends DatasetTableFieldDTO {

    private String orderDirection;
}
