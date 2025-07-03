package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.container.constant.ContainerType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 容器类型转换器 */
@MappedTypes(ContainerType.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ContainerTypeConverter extends BaseTypeHandler<ContainerType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ContainerType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public ContainerType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : ContainerType.fromCode(value);
    }

    @Override
    public ContainerType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : ContainerType.fromCode(value);
    }

    @Override
    public ContainerType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : ContainerType.fromCode(value);
    }
}