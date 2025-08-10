package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.order.constant.OrderType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 订单类型枚举转换器 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(OrderType.class)
public class OrderTypeConverter extends BaseTypeHandler<OrderType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, OrderType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public OrderType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : OrderType.fromCode(code);
    }

    @Override
    public OrderType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : OrderType.fromCode(code);
    }

    @Override
    public OrderType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : OrderType.fromCode(code);
    }
}