package io.dataease.datasource.type;

import io.dataease.exception.DEException;
import io.dataease.extensions.datasource.vo.DatasourceConfiguration;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

@Data
@Component("ck")
public class CK extends DatasourceConfiguration {
    private String driver = "com.clickhouse.jdbc.ClickHouseDriver";
    private String extraParams = "";
    private String compressAlgorithm = "none"; // 默认设置为none以避免HTTP压缩问题
    private List<String> illegalParameters = Arrays.asList("autoDeserialize", "queryInterceptors", "statementInterceptors", "detectCustomCollations", "maxAllowedPacket", "allowloadlocalinfile", "allowUrlInLocalInfile", "allowLoadLocalInfileInPath");

    public String getJdbc() {
        if (StringUtils.isNoneEmpty(getUrlType()) && !getUrlType().equalsIgnoreCase("hostName")) {
            if (!getJdbcUrl().startsWith("jdbc:clickhouse")) {
                DEException.throwException("Illegal jdbcUrl: " + getJdbcUrl());
            }
            // 检查危险参数
            String lowerUrl = URLDecoder.decode(getJdbcUrl()).toLowerCase();
            for (String illegalParameter : illegalParameters) {
                if (lowerUrl.contains(illegalParameter.toLowerCase())) {
                    DEException.throwException("Illegal parameter: " + illegalParameter);
                }
            }
            // 如果用户提供的JDBC URL中没有压缩算法设置，添加默认设置
            if (!getJdbcUrl().contains("compress_algorithm") && !getJdbcUrl().contains("enable_http_compression")) {
                if (getJdbcUrl().contains("?")) {
                    return getJdbcUrl() + "&compress_algorithm=" + compressAlgorithm;
                } else {
                    return getJdbcUrl() + "?compress_algorithm=" + compressAlgorithm;
                }
            }
            return getJdbcUrl();
        }
        StringBuilder jdbcUrl = new StringBuilder();
        if (StringUtils.isEmpty(extraParams.trim())) {
            jdbcUrl.append("jdbc:clickhouse://")
                    .append(getLHost().trim())
                    .append(":")
                    .append(getLPort().toString().trim())
                    .append("/")
                    .append(getDataBase().trim());
        } else {
            jdbcUrl.append("jdbc:clickhouse://")
                    .append(getLHost().trim())
                    .append(":")
                    .append(getLPort().toString().trim())
                    .append("/")
                    .append(getDataBase().trim())
                    .append("?")
                    .append(getExtraParams().trim());
        }
        
        // 如果URL中还没有压缩算法设置，添加默认设置
        if (!jdbcUrl.toString().contains("compress_algorithm") && !jdbcUrl.toString().contains("enable_http_compression")) {
            if (jdbcUrl.toString().contains("?")) {
                jdbcUrl.append("&compress_algorithm=").append(compressAlgorithm);
            } else {
                jdbcUrl.append("?compress_algorithm=").append(compressAlgorithm);
            }
        }
        
        return jdbcUrl.toString();
    }
}
