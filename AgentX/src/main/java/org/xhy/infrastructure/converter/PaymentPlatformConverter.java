package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.order.constant.PaymentPlatform;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 支付平台枚举转换器 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(PaymentPlatform.class)
public class PaymentPlatformConverter extends BaseTypeHandler<PaymentPlatform> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, PaymentPlatform parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public PaymentPlatform getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return PaymentPlatform.fromCode(code);
    }

    @Override
    public PaymentPlatform getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return PaymentPlatform.fromCode(code);
    }

    @Override
    public PaymentPlatform getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return PaymentPlatform.fromCode(code);
    }
}