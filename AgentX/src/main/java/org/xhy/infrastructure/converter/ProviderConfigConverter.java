package org.xhy.infrastructure.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.internal.Json;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhy.domain.llm.model.config.ProviderConfig;
import org.xhy.infrastructure.util.JsonUtils;
import org.xhy.infrastructure.utils.EncryptUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 服务商配置转换器
 * 处理加密存储的配置信息
 */
@MappedTypes(ProviderConfig.class)
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER})
public class ProviderConfigConverter extends BaseTypeHandler<ProviderConfig> {

    private static final Logger log = LoggerFactory.getLogger(ProviderConfigConverter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, ProviderConfig parameter, JdbcType jdbcType)
            throws SQLException {
        String jsonStr = JsonUtils.toJsonString(parameter);
        String encryptedStr = EncryptUtils.encrypt(jsonStr);
        ps.setString(i, encryptedStr);
    }

    @Override
    public ProviderConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encryptedStr = rs.getString(columnName);
        return parseEncryptedJson(encryptedStr);
    }

    @Override
    public ProviderConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public ProviderConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encryptedStr = cs.getString(columnIndex);
        return parseEncryptedJson(encryptedStr);
    }

    private ProviderConfig parseEncryptedJson(String encryptedStr) throws SQLException {
        if (encryptedStr == null || encryptedStr.isEmpty()) {
            return new ProviderConfig();
        }

        String jsonStr = EncryptUtils.decrypt(encryptedStr);;

        return  JsonUtils.parseObject(jsonStr,ProviderConfig.class);

    }
}