package io.dataease.api.dataset.vo;

import io.dataease.api.dataset.dto.DatasetNodeDTO;
import io.dataease.model.ITreeBase;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class DatasetTreeNodeVO extends DatasetNodeDTO implements ITreeBase<DatasetTreeNodeVO> {

    private List<DatasetTreeNodeVO> children;

    private Boolean leaf;

}
