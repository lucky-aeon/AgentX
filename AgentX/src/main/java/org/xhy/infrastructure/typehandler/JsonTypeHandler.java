package org.xhy.infrastructure.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;
import org.xhy.domain.agent.model.AgentTool;
import org.xhy.domain.agent.model.ModelConfig;
import org.xhy.infrastructure.util.JsonUtils;
import org.xhy.infrastructure.exception.ParamValidationException;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 自定义JSON类型处理器
 * 用于将Java对象与数据库中的JSON字符串互相转换
 * 
 * @param <T> 要处理的Java类型
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes({ Object.class, List.class, ModelConfig.class, AgentTool.class })
public class JsonTypeHandler<T> extends BaseTypeHandler<T> {

    private final Class<T> clazz;
    private final boolean isList;
    private final Class<?> itemClazz;

    public JsonTypeHandler(Class<T> clazz) {
        this(clazz, false, null);
    }

    public JsonTypeHandler(Class<T> clazz, boolean isList, Class<?> itemClazz) {
        if (clazz == null) {
            throw new ParamValidationException("clazz", "Type argument cannot be null");
        }
        this.clazz = clazz;
        this.isList = isList;
        this.itemClazz = itemClazz;
    }

    /**
     * 默认构造函数
     */
    public JsonTypeHandler() {
        this.clazz = (Class<T>) Object.class;
        this.isList = false;
        this.itemClazz = null;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
        // 创建PostgreSQL的JSON对象
        PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        jsonObject.setValue(JsonUtils.toJsonString(parameter));
        ps.setObject(i, jsonObject);
    }

    @Override
    public T getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public T getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public T getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private T parse(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }

        if (isList && itemClazz != null) {
            List<?> list = JsonUtils.parseArray(json, itemClazz);
            return (T) list;
        } else {
            return JsonUtils.parseObject(json, clazz);
        }
    }
}