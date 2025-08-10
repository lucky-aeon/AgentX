package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.rule.constant.RuleHandlerKey;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 规则处理器标识转换器 用于MyBatis-Plus在数据库和Java对象间转换RuleHandlerKey枚举 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(RuleHandlerKey.class)
public class RuleHandlerKeyConverter extends BaseTypeHandler<RuleHandlerKey> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, RuleHandlerKey parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getKey());
    }

    @Override
    public RuleHandlerKey getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String key = rs.getString(columnName);
        return key == null ? null : RuleHandlerKey.fromKey(key);
    }

    @Override
    public RuleHandlerKey getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String key = rs.getString(columnIndex);
        return key == null ? null : RuleHandlerKey.fromKey(key);
    }

    @Override
    public RuleHandlerKey getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String key = cs.getString(columnIndex);
        return key == null ? null : RuleHandlerKey.fromKey(key);
    }
}