package com.manniwood.mpjw.converters;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class BaseConverter<T> implements Converter<T> {
    @Override
    public abstract void setItem(PreparedStatement pstmt, int i, T t) throws SQLException;
}
