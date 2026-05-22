package io.dataease.dataset.manage.filter;

import lombok.Data;
import java.util.List;

@Data
public class FilterExpressionDTO {
    /** AND / OR */
    private String logic;
    /** 条件列表 */
    private List<FilterConditionDTO> conditions;
    /** 嵌套子组 */
    private List<FilterExpressionDTO> children;

    @Data
    public static class FilterConditionDTO {
        /** 字段名（展示用，校验依赖 fieldId） */
        private String field;
        /** 字段 ID（必须是数据集中真实存在的字段） */
        private Long fieldId;
        /**
         * 操作符白名单：
         * eq, ne, gt, lt, gte, lte, in, not_in, like, not_like, is_null, is_not_null
         */
        private String operator;
        /** 值：可以是字符串、数字、字符串数组（in/not_in 时） */
        private Object value;
        /** 值类型：string / number / date */
        private String valueType;
    }
}
