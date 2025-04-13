package org.xhy.domain.plugins.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.plugins.constant.PluginStatus;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PluginStatus枚举的TypeHandler
 */
@MappedTypes(PluginStatus.class)
public class PluginStatusTypeHandler extends BaseTypeHandler<PluginStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PluginStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setInt(i, parameter.getCode());
    }

    @Override
    public PluginStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int code = rs.getInt(columnName);
        return rs.wasNull() ? null : PluginStatus.fromCode(code);
    }

    @Override
    public PluginStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int code = rs.getInt(columnIndex);
        return rs.wasNull() ? null : PluginStatus.fromCode(code);
    }

    @Override
    public PluginStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        int code = cs.getInt(columnIndex);
        return cs.wasNull() ? null : PluginStatus.fromCode(code);
    }
}
