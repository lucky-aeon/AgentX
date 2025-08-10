package org.xhy.infrastructure.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/** 用量数据JSON字段转换器 专门处理用量记录中用量数据的序列化和反序列化 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes({Map.class})
public class QuantityDataConverter extends BaseTypeHandler<Map<String, Object>> {

    private static final Logger logger = LoggerFactory.getLogger(QuantityDataConverter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            String json = objectMapper.writeValueAsString(parameter);
            // 对于PostgreSQL的JSONB类型，需要使用setObject而不是setString
            ps.setObject(i, json, java.sql.Types.OTHER);
        } catch (JsonProcessingException e) {
            logger.error("用量数据JSON序列化失败", e);
            throw new SQLException("用量数据JSON序列化失败", e);
        }
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private Map<String, Object> parseJson(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            logger.error("用量数据JSON反序列化失败: {}", json, e);
            throw new SQLException("用量数据JSON反序列化失败", e);
        }
    }
}