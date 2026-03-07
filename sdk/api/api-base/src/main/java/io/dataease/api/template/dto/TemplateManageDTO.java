package io.dataease.api.template.dto;


import io.dataease.api.template.vo.VisualizationTemplateVO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;


@Data
@EqualsAndHashCode(callSuper = true)
public class TemplateManageDTO extends VisualizationTemplateVO {

    private String label;

    private Integer childrenCount;

    private String categoryName;

    private Long recentUseTime;

    private Boolean checked = false;

    private List<TemplateManageDTO> children;

    private List<String> categories;

    private List<String> categoryNames;


}
