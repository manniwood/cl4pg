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

import com.manniwood.mpjw.converters.ConverterStore;

public class Notify implements Command {

    private final ConverterStore converterStore;
    private final String sql = "select pg_notify(?, ?)";
    private final Connection conn;
    private final String channel;
    private final String payload;
    private PreparedStatement pstmt;

    public Notify(ConverterStore converterStore, Connection conn, String channel, String payload) {
        super();
        this.converterStore = converterStore;
        this.conn = conn;
        this.channel = channel;
        this.payload = payload;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute() throws SQLException {
        pstmt = conn.prepareStatement(sql);
        converterStore.setSQLArgument(pstmt, 1, channel, "java.lang.String");
        converterStore.setSQLArgument(pstmt, 2, payload, "java.lang.String");
        pstmt.execute();
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
