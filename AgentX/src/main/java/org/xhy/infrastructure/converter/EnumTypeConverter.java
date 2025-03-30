package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 枚举类型转换器
 * 用于处理数据库VARCHAR类型和Java枚举之间的转换
 * @param <E> 要转换的枚举类型
 */
@MappedJdbcTypes(JdbcType.VARCHAR)
public abstract class EnumTypeConverter<E extends Enum<E>> extends BaseTypeHandler<E> {
    
    private final Class<E> type;
    
    protected EnumTypeConverter(Class<E> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.type = type;
    }
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, convertToString(parameter));
    }
    
    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : convertToEnum(value);
    }
    
    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : convertToEnum(value);
    }
    
    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : convertToEnum(value);
    }
    
    /**
     * 将枚举值转换为字符串
     * 默认使用枚举的name()方法
     */
    protected String convertToString(E parameter) {
        return parameter.name();
    }
    
    /**
     * 将字符串转换为枚举值
     * 默认使用Enum.valueOf()方法
     */
    protected E convertToEnum(String value) {
        return Enum.valueOf(type, value);
    }
} 