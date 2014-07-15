package com.manniwood.mpjw.converters;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface Converter<T> {
    void setItem(PreparedStatement pstmt, int i, T t) throws SQLException;
}
