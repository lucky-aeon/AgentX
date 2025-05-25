package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.scheduledtask.constant.RepeatType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 重复类型转换器 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(RepeatType.class)
public class RepeatTypeConverter extends BaseTypeHandler<RepeatType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RepeatType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public RepeatType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : RepeatType.fromCode(value);
    }

    @Override
    public RepeatType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : RepeatType.fromCode(value);
    }

    @Override
    public RepeatType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : RepeatType.fromCode(value);
    }
} 