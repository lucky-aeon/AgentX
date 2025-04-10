package org.xhy.infrastructure.converter;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

/**
 * @author zang
 * @date 17:06 <br/>
 */
@MappedTypes(float[].class)
public class PgVectorTypeHandler extends BaseTypeHandler<float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        // 将float[]转换为PostgreSQL vector类型的字符串表示："[0.1,0.2,0.3]"

        StringBuilder sb = new StringBuilder("[");

        for (int j = 0; j < parameter.length; j++) {
            sb.append(parameter[j]);
            if (j < parameter.length - 1) {
                sb.append(",");
            }
        }

        sb.append("]");

        String vectorStr = sb.toString();
        // 使用Types.OTHER类型将字符串设置为vector类型
        ps.setObject(i, vectorStr, Types.OTHER);
    }


    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String vectorStr = rs.getString(columnName);
        return parseVector(vectorStr);
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String vectorStr = rs.getString(columnIndex);
        return parseVector(vectorStr);
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String vectorStr = cs.getString(columnIndex);
        return parseVector(vectorStr);
    }

    /**
     * 将PostgreSQL vector类型的字符串表示解析为float[]
     */
    private float[] parseVector(String vectorStr) {
        if (vectorStr == null || vectorStr.isEmpty()) {
            return new float[0];
        }

        // 去除开头和结尾的方括号
        vectorStr = vectorStr.substring(1, vectorStr.length() - 1);

        Float[] floats = Arrays.stream(vectorStr.split(","))
                .filter(s -> !s.isEmpty())
                .map(Float::parseFloat) // 使用Float::parseFloat
                .toArray(Float[]::new);

        // 手动将Float[]转换为float[]
        float[] result = new float[floats.length];
        for (int i = 0; i < floats.length; i++) {
            result[i] = floats[i];
        }
        return result;
    }


}
