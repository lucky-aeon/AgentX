package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.conversation.constant.Role;
import org.xhy.infrastructure.llm.protocol.enums.ProviderProtocol;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 模型类型转换器 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(ProviderProtocol.class)
public class ProviderProtocolConverter extends BaseTypeHandler<ProviderProtocol> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ProviderProtocol parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public ProviderProtocol getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : ProviderProtocol.fromCode(value);
    }

    @Override
    public ProviderProtocol getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : ProviderProtocol.fromCode(value);
    }

    @Override
    public ProviderProtocol getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : ProviderProtocol.fromCode(value);
    }
}