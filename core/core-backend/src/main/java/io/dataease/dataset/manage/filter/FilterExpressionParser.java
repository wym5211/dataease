package io.dataease.dataset.manage.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dataease.exception.DEException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FilterExpressionParser {

    private final ObjectMapper objectMapper;
    private final FieldValidator fieldValidator;

    /**
     * 判断是否为结构化 JSON 表达式（以 { 开头）
     */
    public boolean isJsonExpression(String expression) {
        if (StringUtils.isBlank(expression)) return false;
        String trimmed = expression.trim();
        return trimmed.startsWith("{");
    }

    /**
     * 验证表达式合法性（保存前调用）
     */
    public void validateExpression(String jsonExpression) {
        if (StringUtils.isBlank(jsonExpression)) {
            DEException.throwException("过滤表达式不能为空");
        }
        try {
            FilterExpressionDTO dto = objectMapper.readValue(jsonExpression, FilterExpressionDTO.class);
            validateExpressionDTO(dto);
        } catch (DEException e) {
            throw e;
        } catch (Exception e) {
            DEException.throwException("过滤表达式格式错误: " + e.getMessage());
        }
    }

    private void validateExpressionDTO(FilterExpressionDTO dto) {
        if (dto == null) return;

        // 校验 logic
        if (StringUtils.isNotBlank(dto.getLogic())) {
            String logic = dto.getLogic().toUpperCase();
            if (!"AND".equals(logic) && !"OR".equals(logic)) {
                DEException.throwException("logic 只能为 AND 或 OR");
            }
        }

        // 校验每个条件
        if (CollectionUtils.isNotEmpty(dto.getConditions())) {
            for (FilterExpressionDTO.FilterConditionDTO cond : dto.getConditions()) {
                if (cond == null) continue;
                fieldValidator.validateFieldName(cond.getField());
                fieldValidator.validateOperator(cond.getOperator());
                fieldValidator.validateValueType(cond.getValueType());
                fieldValidator.validateValue(cond.getValue());
            }
        }

        // 递归校验子表达式
        if (CollectionUtils.isNotEmpty(dto.getChildren())) {
            for (FilterExpressionDTO child : dto.getChildren()) {
                validateExpressionDTO(child);
            }
        }
    }

    /**
     * 将 JSON 表达式解析为 FilterExpressionDTO
     */
    public FilterExpressionDTO parse(String jsonExpression) {
        if (StringUtils.isBlank(jsonExpression)) return null;
        try {
            return objectMapper.readValue(jsonExpression, FilterExpressionDTO.class);
        } catch (Exception e) {
            DEException.throwException("解析过滤表达式失败: " + e.getMessage());
            return null;
        }
    }
}
