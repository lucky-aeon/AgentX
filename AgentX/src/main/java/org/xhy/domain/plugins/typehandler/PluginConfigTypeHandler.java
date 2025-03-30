package org.xhy.domain.plugins.typehandler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.plugins.config.PluginConfig;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 插件配置类型处理器
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(PluginConfig.class)
public class PluginConfigTypeHandler extends BaseTypeHandler<PluginConfig> {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PluginConfig parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting PluginConfig to JSON", e);
        }
    }
    
    @Override
    public PluginConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PluginConfig.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting JSON to PluginConfig", e);
        }
    }
    
    @Override
    public PluginConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PluginConfig.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting JSON to PluginConfig", e);
        }
    }
    
    @Override
    public PluginConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, PluginConfig.class);
        } catch (JsonProcessingException e) {
            throw new SQLException("Error converting JSON to PluginConfig", e);
        }
    }
}
