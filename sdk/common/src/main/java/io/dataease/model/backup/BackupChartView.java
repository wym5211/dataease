package io.dataease.model.backup;

import lombok.Data;

/**
 * 图表备份信息
 */
@Data
public class BackupChartView {
    private String id;                    // 原 ID (Long -> String)
    private String title;                 // 标题
    private Long sceneId;                 // 场景ID（导入时替换为新仪表板ID）
    private Long tableId;                 // 数据集ID（导入时替换为新数据集ID）
    private String datasetName;            // 数据集名称（导出时填充，导入时用于按名称匹配）
    private String type;                  // 图表类型
    private String render;                // 渲染方式
    private Integer resultCount;
    private String resultMode;
    private String xAxis;
    private String xAxisExt;
    private String yAxis;
    private String yAxisExt;

    // 手动实现getter/setter以匹配CoreChartView的命名约定
    public String getxAxis() {
        return xAxis;
    }
    public void setxAxis(String xAxis) {
        this.xAxis = xAxis;
    }
    public String getxAxisExt() {
        return xAxisExt;
    }
    public void setxAxisExt(String xAxisExt) {
        this.xAxisExt = xAxisExt;
    }
    public String getyAxis() {
        return yAxis;
    }
    public void setyAxis(String yAxis) {
        this.yAxis = yAxis;
    }
    public String getyAxisExt() {
        return yAxisExt;
    }
    public void setyAxisExt(String yAxisExt) {
        this.yAxisExt = yAxisExt;
    }
    private String extStack;
    private String extBubble;
    private String extLabel;
    private String extTooltip;
    private String customAttr;
    private String customStyle;
    private String customFilter;
    private String drillFields;
    private String senior;
    private String createBy;
    private Long createTime;
    private Long updateTime;
    private String snapshot;
    private String stylePriority;
    private String chartType;
    private Boolean isPlugin;
    private String dataFrom;
    private String viewFields;
    private Boolean refreshViewEnable;
    private String refreshUnit;
    private Integer refreshTime;
    private Boolean linkageActive;
    private Boolean jumpActive;
    private Long copyFrom;
    private Long copyId;
    private Boolean aggregate;
    private String flowMapStartName;
    private String flowMapEndName;
    private String extColor;
    private String customAttrMobile;
    private String customStyleMobile;
    private String sortPriority;
}
