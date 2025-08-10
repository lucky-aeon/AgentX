package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.product.constant.BillingType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 计费类型转换器 用于MyBatis-Plus在数据库和Java对象间转换BillingType枚举 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(BillingType.class)
public class BillingTypeConverter extends BaseTypeHandler<BillingType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, BillingType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public BillingType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : BillingType.fromCode(code);
    }

    @Override
    public BillingType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : BillingType.fromCode(code);
    }

    @Override
    public BillingType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : BillingType.fromCode(code);
    }
}