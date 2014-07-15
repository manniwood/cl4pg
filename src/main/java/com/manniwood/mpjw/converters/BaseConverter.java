package com.manniwood.mpjw.converters;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class BaseConverter<T> implements Converter<T> {
    // XXX: handle setting null here?
    @Override
    public abstract void setItem(PreparedStatement pstmt, int i, T t) throws SQLException;

    public abstract T getItem(ResultSet rs, int i) throws SQLException;
}
