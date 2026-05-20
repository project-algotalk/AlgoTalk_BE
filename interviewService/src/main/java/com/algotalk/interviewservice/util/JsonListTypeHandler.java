package com.algotalk.interviewservice.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JsonListTypeHandler extends BaseTypeHandler<List<String>> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // List<String> -> JSON 문자열로 변환하여 DB 저장
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("JSON 직렬화 실패: " + e.getMessage(), e);
        }
    }

    // DB JSON 문자열 -> List<String>으로 변환 (컬럼명 기준 조회)
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    // DB JSON 문자열 -> List<String>으로 변환 (컬럼 인덱스 기준 조회)
    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJson(rs.getString(columnIndex));
    }

    // DB JSON 문자열 -> List<String>으로 변환 (저장 프로시저 결과 조회)
    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJson(cs.getString(columnIndex));
    }

     // JSON 문자열을 List<String>으로 변환하는 공통 메서드
    private List<String> parseJson(String json) throws SQLException {
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new SQLException("JSON 역직렬화 실패: " + e.getMessage(), e);
        }
    }
}