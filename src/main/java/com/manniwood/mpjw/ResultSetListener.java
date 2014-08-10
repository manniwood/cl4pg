package com.manniwood.mpjw;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface ResultSetListener {
    void processRow(ResultSet rs) throws SQLException;
}
