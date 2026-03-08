package io.dataease.api.permissions.org.dto;

import io.dataease.model.KeywordRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "组织列表过滤器")
@EqualsAndHashCode(callSuper = true)
@Data
public class OrgLazyRequest extends KeywordRequest {
    @Schema(description = "上级节点ID")
    private Long pid;
    @Schema(description = "是否降序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean desc = true;
}
