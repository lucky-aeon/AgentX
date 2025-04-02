package org.xhy.infrastructure.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.xhy.infrastructure.util.JsonUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JSON类型转换处理器
 * @param <T> 要转换的类型
 */
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {
    
    private final Class<T> clazz;
    
    public JsonTypeHandler(Class<T> clazz) {
        this.clazz = clazz;
    }
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, JsonUtils.toJsonString(parameter));
    }
    
    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return json == null ? null : JsonUtils.parseObject(json, clazz);
    }
    
    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return json == null ? null : JsonUtils.parseObject(json, clazz);
    }
    
    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return json == null ? null : JsonUtils.parseObject(json, clazz);
    }
}