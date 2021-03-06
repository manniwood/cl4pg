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
package com.manniwood.cl4pg.v1.typeconverters.types;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Gets and sets Java types from PreparedStatements
 * and CallableStatements. Note that because this
 * interface is parameterized, there are no java
 * primitive type converters. The advantage of this
 * is that the java box types support null, which maps
 * nicely to SQL null values. The disadvantage is
 * the overhead of boxing and unboxing from primitives.
 * If more control of getting and setting Java types
 * is required, consider implementing a ResultSetHandler.
 * @author mwood
 *
 * @param <T>
 */
public interface TypeConverter<T> {
    void setItem(PreparedStatement pstmt,
                 int i,
                 T t) throws SQLException;

    T getItem(ResultSet rs,
              int i) throws SQLException;

    void registerOutParameter(CallableStatement cstmt,
                              int i) throws SQLException;

    T getItem(CallableStatement cstmt,
              int i) throws SQLException;
}
