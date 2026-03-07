package io.dataease.api.visualization.request;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.dataease.api.visualization.dto.VisualizationLinkageDTO;
import io.dataease.api.visualization.vo.VisualizationLinkageVO;
import io.dataease.constant.CommonConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : WangJiaHao
 * @date : 2023/7/13
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VisualizationLinkageRequest extends VisualizationLinkageVO {

    /**
     * 仪表板 or 大屏ID
     * */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long dvId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceViewId;

    private Boolean ActiveStatus;

    private List<String> targetViewIds;

    private String resourceTable = CommonConstants.RESOURCE_TABLE.CORE;

    private List<VisualizationLinkageDTO> linkageInfo = new ArrayList<>();

}
