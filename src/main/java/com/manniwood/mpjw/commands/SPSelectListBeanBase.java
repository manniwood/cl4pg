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
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.ParsedSQLWithSimpleArgs;
import com.manniwood.mpjw.converters.ConverterStore;

public abstract class SPSelectListBeanBase<T, P> extends SPSelectListBase<T> implements Command {

    private final P p;

    public SPSelectListBeanBase(
            ConverterStore converterStore,
            String sql,
            Connection conn,
            Class<T> returnType,
            P p) {
        super(converterStore, sql, conn, returnType);
        this.p = p;
    }

    @Override
    protected void setSQLArguments(ParsedSQLWithSimpleArgs tsql) throws SQLException {
        List<String> types = tsql.getArgs();
        // The very first type will be the refcursor, which needs to be registered.
        // If it is not there, error out.
        if (types == null || types.isEmpty()) {
            throw new MPJWException("First argument needs to exist, and needs to be of type refcursor.");
        }
        types.remove(0);
        cstmt.registerOutParameter(1, Types.OTHER);
        // if there are any other args left, set them.
        if ( ! types.isEmpty()) {
            // Starting with column 2, set the sql arguments
            converterStore.setSQLArguments(cstmt, p, tsql.getArgs(), 2);
        }
    }

    @Override
    protected abstract void populateList() throws SQLException;

}
