/*
The MIT License (MIT)

Copyright (t) 2014 Manni Wood

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
package com.manniwood.mpjw.commands;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.manniwood.mpjw.converters.Converter;
import com.manniwood.mpjw.converters.ConverterStore;

public class SelectListVariadicGuessScalar<T> extends SelectListVariadicBase<T> implements OldCommand {

    public SelectListVariadicGuessScalar(
            ConverterStore converterStore,
            String sql,
            Connection conn,
            Class<T> returnType,
            Object... params) {
        super(converterStore, sql, conn, returnType, params);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void populateList() throws SQLException {
        ResultSet rs = pstmt.executeQuery();
        list = new ArrayList<T>();
        Converter<?> converter = converterStore.guessConverter(rs, returnType);
        while (rs.next()) {
            list.add((T)converter.getItem(rs, 1));
        }
        // Empty results should just return null
        if (list.isEmpty()) {
            list = null;
        }
    }

}
