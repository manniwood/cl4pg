package com.manniwood.mpjw.converters;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class IntConverter extends BaseConverter<Integer>{

    @Override
    public void setItem(PreparedStatement pstmt, int i, Integer t) throws SQLException {
        int myInt = t.intValue();
        pstmt.setInt(i, myInt);
    }

}
