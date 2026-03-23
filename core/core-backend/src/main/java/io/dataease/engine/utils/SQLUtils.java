package io.dataease.engine.utils;

import java.util.Optional;

/**
 * @Author Junjun
 */
public class SQLUtils {
    public static String transKeyword(String value) {
        return Optional.ofNullable(value).orElse("").replaceAll("'", "''").replaceAll("\\\\","\\\\\\\\").replace("\n", "\\n");
    }

    public static String buildOriginPreviewSql(String sql, int limit, int offset) {
        return "SELECT * FROM (" + sql + ") tmp LIMIT " + limit + " OFFSET " + offset;
    }

    public static String buildOriginPreviewSqlWithOrderBy(String sql, int limit, int offset, String orderBy) {
        // Validate orderBy is a valid identifier (alphanumeric, underscore, dot for table.column)
        if (!orderBy.matches("^[a-zA-Z0-9_,\\.\\s]+$")) {
            throw new IllegalArgumentException("Invalid orderBy identifier");
        }
        // Escape backticks in identifier and wrap with backticks
        String escapedOrderBy = orderBy.replace("`", "``");
        return "SELECT * FROM (" + sql + ") tmp ORDER BY `" + escapedOrderBy + "` LIMIT " + limit + " OFFSET " + offset;
    }
}
