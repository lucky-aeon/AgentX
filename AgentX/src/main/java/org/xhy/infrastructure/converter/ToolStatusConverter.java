package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.tool.constant.ToolStatus;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 工具状态转换器
 */
@MappedTypes(ToolStatus.class)
@MappedJdbcTypes(JdbcType.INTEGER)
public class ToolStatusConverter extends BaseTypeHandler<ToolStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ToolStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());

    }

    @Override
    public ToolStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {

        return rs.wasNull() ? null : ToolStatus.fromCode(rs.getString(columnName));
    }

    @Override
    public ToolStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.wasNull() ? null : ToolStatus.fromCode(rs.getString(columnIndex));
    }

    @Override
    public ToolStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.wasNull() ? null : ToolStatus.fromCode(cs.getString(columnIndex));
    }
}