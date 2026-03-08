package io.dataease.api.sync.task.dto;

import io.dataease.model.KeywordRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class TaskGridRequest extends KeywordRequest {
    private List<String> logStatus;
    private List<String> status;
    private List<String> lastExecuteTime;
    private List<String> nextExecuteTime;
}
