package org.xhy.infrastructure.converter;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.xhy.domain.scheduledtask.constant.ScheduleTaskStatus;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 定时任务状态转换器 */
@MappedTypes(ScheduleTaskStatus.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class ScheduledTaskStatusConverter extends BaseTypeHandler<ScheduleTaskStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ScheduleTaskStatus parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.name());
    }

    @Override
    public ScheduleTaskStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : ScheduleTaskStatus.fromCode(value);
    }

    @Override
    public ScheduleTaskStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : ScheduleTaskStatus.fromCode(value);
    }

    @Override
    public ScheduleTaskStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : ScheduleTaskStatus.fromCode(value);
    }
} 