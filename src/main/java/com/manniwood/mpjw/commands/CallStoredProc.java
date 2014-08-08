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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.manniwood.mpjw.ParsedSQLWithComplexArgs;
import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.mpjw.converters.SetterAndConverter;

public class CallStoredProc<T> implements Command {

    protected CallableStatement cstmt;
    protected final String sql;
    protected final Connection conn;
    protected final ConverterStore converterStore;
    protected T t;

    public CallStoredProc(
            ConverterStore converterStore,
            String sql,
            Connection conn,
            T t) {
        super();
        this.converterStore = converterStore;
        this.sql = sql;
        this.conn = conn;
        this.t = t;
    }

    @Override
    public void execute() throws SQLException {
        ParsedSQLWithComplexArgs tsql = SQLTransformer.transformWithInOut(sql);
        cstmt = conn.prepareCall(tsql.getSql());
        // XXX I'm already finding the setters here, so...
        // And the only reason why I'm finding the setter is so that
        // I can registerOutParameter; is there a better way?
        converterStore.setSQLArguments(cstmt, t, tsql.getArgs());
        cstmt.execute();
        // XXX ...it's a shame to have to do it again here. Is there a better way?
        List<SetterAndConverter> settersAndConverters = converterStore.specifySetters(cstmt, t.getClass(), tsql.getArgs());
        converterStore.populateBeanUsingSetters(cstmt, t, settersAndConverters);
    }


    @Override
    public void cleanUp() throws Exception {
        if (cstmt != null) {
            cstmt.close();
        }
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

}
