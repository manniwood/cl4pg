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

import com.manniwood.mpjw.ComplexArg;
import com.manniwood.mpjw.ParsedSQLWithComplexArgs;
import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.converters.ConverterStore;

public abstract class CallStoredProc<T> implements Command {

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

        // XXX: this needs to register out parameters
        // as well as set sql arguments
        // cstmt..registerOutParameter(1, Types.VARCHAR);
        // cstmt.setString(2, "test string");
        setSQLArguments(tsql);
        cstmt.execute();
        // XXX: write a method that calls cstmt.getString(1) or
        // whatever to get the out param, and t.setSomething()
        // to set the param.
        // callBeanSetters(cstmt, t)
    }

    protected void setSQLArguments(ParsedSQLWithComplexArgs tsql) throws SQLException {
        List<ComplexArg> args = tsql.getArgs();
        // XXX: now go through args and for each getter/setter that
        // is not null, do the appropriate registeroutparameter and/or
        // setstring

        /*if (types == null || types.isEmpty()) {
            return;
        }
        converterStore.setSQLArguments(cstmt, t, tsql.getArgs());*/
    }

    @Override
    public void cleanUp() throws Exception {
        if (cstmt != null) {
            cstmt.close();
        }
    }

}
