package org.xhy.infrastructure.converter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.llm.model.enums.ModelType;

/** 模型类型转换器 */
@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(ModelType.class)
public class ModelTypeConverter extends BaseTypeHandler<ModelType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ModelType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public ModelType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : ModelType.fromCode(value);
    }

    @Override
    public ModelType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : ModelType.fromCode(value);
    }

    @Override
    public ModelType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : ModelType.fromCode(value);
    }
}
