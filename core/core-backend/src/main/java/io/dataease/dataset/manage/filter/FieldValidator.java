package io.dataease.dataset.manage.filter;

import io.dataease.exception.DEException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class FieldValidator {

    // 仅允许字母、数字、下划线（防止字段名注入）
    private static final Pattern SAFE_FIELD_PATTERN = Pattern.compile("^[a-zA-Z0-9_\u4e00-\u9fa5]+$");

    private static final Set<String> ALLOWED_OPERATORS = Set.of(
        "eq", "ne", "gt", "lt", "gte", "lte",
        "in", "not_in", "like", "not_like",
        "is_null", "is_not_null"
    );

    private static final Set<String> ALLOWED_VALUE_TYPES = Set.of("string", "number", "date");

    /**
     * 校验字段名安全性（防特殊字符注入）
     */
    public void validateFieldName(String fieldName) {
        if (StringUtils.isBlank(fieldName)) {
            DEException.throwException("字段名不能为空");
        }
        if (!SAFE_FIELD_PATTERN.matcher(fieldName).matches()) {
            DEException.throwException("字段名包含非法字符: " + fieldName);
        }
    }

    /**
     * 校验操作符是否在白名单中
     */
    public void validateOperator(String operator) {
        if (!ALLOWED_OPERATORS.contains(operator)) {
            DEException.throwException("不支持的操作符: " + operator);
        }
    }

    /**
     * 校验值类型是否合法
     */
    public void validateValueType(String valueType) {
        if (StringUtils.isNotBlank(valueType) && !ALLOWED_VALUE_TYPES.contains(valueType)) {
            DEException.throwException("不支持的值类型: " + valueType);
        }
    }

    /**
     * 校验值不含 SQL 关键字注入
     */
    public void validateValue(Object value) {
        if (value instanceof String) {
            String strValue = (String) value;
            // 拒绝包含 SQL 注入特征的值
            if (containsSqlInjection(strValue)) {
                DEException.throwException("值包含非法 SQL 字符");
            }
        }
    }

    private boolean containsSqlInjection(String value) {
        if (StringUtils.isBlank(value)) return false;
        String upper = value.toUpperCase();
        // 检测常见 SQL 注入模式
        return upper.contains("--") ||
               upper.contains("/*") ||
               upper.contains("*/") ||
               upper.contains("';") ||
               upper.contains("DROP ") ||
               upper.contains("DELETE ") ||
               upper.contains("INSERT ") ||
               upper.contains("UPDATE ") ||
               upper.contains("EXEC ") ||
               upper.contains("EXECUTE ") ||
               upper.contains("UNION ") ||
               upper.contains("SELECT ");
    }
}
