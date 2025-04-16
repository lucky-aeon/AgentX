package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.postgresql.util.PGobject;
import org.xhy.infrastructure.utils.JsonUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * JSON类型转换器
 * 用于处理数据库JSONB类型和Java对象之间的转换
 * @param <T> 要转换的对象类型
 */
@MappedJdbcTypes(JdbcType.OTHER)
public abstract class JsonToStringConverter<T> extends BaseTypeHandler<T> {
    
    private final Class<T> type;
    
    protected JsonToStringConverter(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(JsonUtils.toJsonString(parameter));
        ps.setObject(i, jsonObject);
    }
    
    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }
    
    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }
    
    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }
    
    private T parseJson(String json) throws SQLException {
        if (json == null) {
            return null;
        }
        return JsonUtils.parseObject(json,type);
    }
} 