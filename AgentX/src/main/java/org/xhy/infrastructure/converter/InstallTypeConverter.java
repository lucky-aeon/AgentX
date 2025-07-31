package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.xhy.domain.rag.constant.InstallType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** InstallType枚举类型转换器
 * @author xhy
 * @date 2025-07-19 <br/>
 */
public class InstallTypeConverter extends BaseTypeHandler<InstallType> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, InstallType parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public InstallType getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String code = rs.getString(columnName);
        return code == null ? null : InstallType.fromCodeOrDefault(code, InstallType.SNAPSHOT);
    }

    @Override
    public InstallType getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String code = rs.getString(columnIndex);
        return code == null ? null : InstallType.fromCodeOrDefault(code, InstallType.SNAPSHOT);
    }

    @Override
    public InstallType getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String code = cs.getString(columnIndex);
        return code == null ? null : InstallType.fromCodeOrDefault(code, InstallType.SNAPSHOT);
    }
}