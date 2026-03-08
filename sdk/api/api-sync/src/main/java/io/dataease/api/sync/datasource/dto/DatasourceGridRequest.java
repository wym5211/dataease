package io.dataease.api.sync.datasource.dto;

import io.dataease.model.KeywordRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class DatasourceGridRequest extends KeywordRequest {
    private List<String> status;
    private List<String> dsType;
    private List<String> createTime;
}
