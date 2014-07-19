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
package com.manniwood.mpjw.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.TransformedSQL;
import com.manniwood.mpjw.converters.ConverterStore;

public class DeleteVariadic implements Command {

    private final ConverterStore converterStore;
    private final String sql;
    private final Connection conn;
    private PreparedStatement pstmt;
    private int numberOfRowsDeleted;
    private Object[] params;

    public DeleteVariadic(ConverterStore converterStore, String sql, Connection conn, Object... params) {
        super();
        this.converterStore = converterStore;
        this.sql = sql;
        this.conn = conn;
        this.params = params;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute() throws SQLException {
        TransformedSQL tsql = SQLTransformer.transform(sql);
        pstmt = conn.prepareStatement(tsql.getSql());
        List<String> types = tsql.getGetters();
        for (int i = 0; i < types.size(); i++) {
            converterStore.setBare(pstmt, i + 1, params[i], types.get(i));
        }
        numberOfRowsDeleted = pstmt.executeUpdate();
    }

    public int getNumberOfRowsDeleted() {
        return numberOfRowsDeleted;
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public PreparedStatement getPreparedStatement() {
        return pstmt;
    }

}
