package io.dataease.api.threshold.dto;

import io.dataease.model.KeywordRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class ThresholdInstanceRequest extends KeywordRequest {
    @Serial
    private static final long serialVersionUID = 7146083160815300271L;

    private Long thresholdId;
}
