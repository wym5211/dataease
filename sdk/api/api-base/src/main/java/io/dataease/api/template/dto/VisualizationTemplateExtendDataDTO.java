package io.dataease.api.template.dto;

import io.dataease.api.template.vo.VisualizationTemplateExtendDataVO;
import io.dataease.utils.IDUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author : WangJiaHao
 * @date : 2023/11/13 10:25
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class VisualizationTemplateExtendDataDTO extends VisualizationTemplateExtendDataVO {


    public VisualizationTemplateExtendDataDTO(Long dvId, Long viewId, String viewDetails) {
        super();
        super.setId(IDUtils.snowID());
        super.setDvId(dvId);
        super.setViewId(viewId);
        super.setViewDetails(viewDetails);
    }
}
