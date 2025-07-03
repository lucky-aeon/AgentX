package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.container.constant.ContainerStatus;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 容器状态转换器 */
@MappedTypes(ContainerStatus.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class ContainerStatusConverter extends BaseTypeHandler<ContainerStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ContainerStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public ContainerStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Integer value = rs.getInt(columnName);
        return rs.wasNull() ? null : ContainerStatus.fromCode(value);
    }

    @Override
    public ContainerStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Integer value = rs.getInt(columnIndex);
        return rs.wasNull() ? null : ContainerStatus.fromCode(value);
    }

    @Override
    public ContainerStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Integer value = cs.getInt(columnIndex);
        return cs.wasNull() ? null : ContainerStatus.fromCode(value);
    }
}