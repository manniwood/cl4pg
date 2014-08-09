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

import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.ParsedSQLWithSimpleArgs;
import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.converters.Converter;
import com.manniwood.mpjw.converters.ConverterStore;

public class CallStoredProcReturnScalar<T, P> implements Command {

    protected CallableStatement cstmt;
    protected final String sql;
    protected final Connection conn;
    protected final ConverterStore converterStore;
    protected final Class<T> returnType;
    protected P p;

    private T t;  // result

    public CallStoredProcReturnScalar(
            ConverterStore converterStore,
            String sql,
            Connection conn,
            Class<T> returnType,
            P p) {
        super();
        this.converterStore = converterStore;
        this.sql = sql;
        this.conn = conn;
        this.returnType = returnType;
        this.p = p;
    }

    @Override
    public void execute() throws SQLException {
        ParsedSQLWithSimpleArgs tsql = SQLTransformer.transformSimply(sql);
        cstmt = conn.prepareCall(tsql.getSql());

        // The first arg is special; it is the return type, not a getter
        // used against the input parameter.
        List<String> args = tsql.getArgs();
        if (args == null || args.isEmpty()) {
            throw new MPJWException(this.getClass().getName() + " needs to return something, yet no return type was found in the SQL template.");
        }
        // Because we already have variable returnType, we can just throw
        // away the string representation of the return type.
        tsql.getArgs().remove(0);

        // we know the return type; find the correct converter for it
        @SuppressWarnings("unchecked")
        Converter<T> returnTypeConverter = (Converter<T>) converterStore.getConverters().get(returnType);

        // Because we have removed the first arg, when we set the
        // arguments, the column number we start at is 2, not 1
        converterStore.setSQLArguments(cstmt, p, args, 2);
        // Use our converter to register the type of the return value, which will be at column 1
        returnTypeConverter.registerOutParameter(cstmt, 1);
        cstmt.execute();
        // Use our converter to get the return value, which will be at column 1
        t = returnTypeConverter.getItem(cstmt, 1);
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

    public T getResult() {
        return t;
    }

}
