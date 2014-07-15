package com.manniwood.mpjw.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StringConverter extends BaseConverter<String>{

    @Override
    public void setItem(PreparedStatement pstmt, int i, String t) throws SQLException {
        pstmt.setString(i, t);
    }

    @Override
    public String getItem(ResultSet rs, int i) throws SQLException {
        return rs.getString(i);
    }
}
