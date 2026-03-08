package io.dataease.api.permissions.user.dto;

import io.dataease.model.KeywordRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserGridRequest extends KeywordRequest {
    private List<Boolean> statusList;

    private List<Integer> originList;

    private List<Long> roleIdList;

    private Boolean timeDesc;
}
