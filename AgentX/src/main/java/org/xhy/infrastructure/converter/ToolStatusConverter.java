package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.domain.tool.constant.ToolStatus;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 工具状态转换器 */
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

        String value = rs.getString(columnName);
        return value == null ? null : ToolStatus.fromCode(value);
    }

    @Override
    public ToolStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : ToolStatus.fromCode(value);
    }

    @Override
    public ToolStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : ToolStatus.fromCode(value);
    }
}