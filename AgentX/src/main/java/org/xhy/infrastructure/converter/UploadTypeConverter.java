package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.tool.constant.UploadType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 上传类型转换器 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(UploadType.class)
public class UploadTypeConverter extends BaseTypeHandler<UploadType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UploadType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public UploadType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : UploadType.fromCode(value);
    }

    @Override
    public UploadType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : UploadType.fromCode(value);
    }

    @Override
    public UploadType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : UploadType.fromCode(value);
    }
}