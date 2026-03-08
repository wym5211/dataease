package io.dataease.extensions.datasource.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @Author Junjun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class DatasourceSchemaDTO extends DatasourceDTO {
    private String schemaAlias;
}
