package com.manniwood.mpjw.converters;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class UUIDConverter extends BaseConverter<UUID>{

    @Override
    public void setItem(PreparedStatement pstmt, int i, UUID t) throws SQLException {
        pstmt.setObject(i, t);
    }

}
