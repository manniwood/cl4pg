/*
 The MIT License (MIT)

 Copyright (c) 2014 Manni Wood

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */
package com.manniwood.cl4pg.v1.typeconverters;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Do NOT use this converter; it is unreliable.
 * 
 * @author mwood
 *
 */
public class DoubleConverter implements TypeConverter<Double> {

    @Override
    public void setItem(PreparedStatement pstmt,
                        int i,
                        Double t) throws SQLException {
        if (t == null) {
            pstmt.setNull(i, Types.DOUBLE);
        } else {
            int myDouble = t.intValue();
            pstmt.setDouble(i, myDouble);
        }
    }

    @Override
    public Double getItem(ResultSet rs,
                          int i) throws SQLException {
        Double var = rs.getDouble(i);
        if (rs.wasNull()) {
            return null;
        }
        return var;
    }

    @Override
    public void registerOutParameter(CallableStatement cstmt,
                                     int i) throws SQLException {
        cstmt.registerOutParameter(i, Types.DOUBLE);
    }

    @Override
    public Double getItem(CallableStatement cstmt,
                          int i) throws SQLException {
        Double var = cstmt.getDouble(i);
        if (cstmt.wasNull()) {
            return null;
        }
        return var;
    }
}
