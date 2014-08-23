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
import java.util.List;

import com.manniwood.mpjw.ParsedSQLWithSimpleArgs;
import com.manniwood.mpjw.ResultSetListener;
import com.manniwood.mpjw.converters.ConverterStore;

public class SelectUsingListenerVariadic extends SelectUsingListenerBase implements OldCommand {

    private final Object[] params;

    public SelectUsingListenerVariadic(
            ConverterStore converterStore,
            String sql,
            Connection conn,
            ResultSetListener listener,
            Object... params) {
        super(converterStore, sql, conn, listener);
        this.params = params;
    }

    @Override
    protected void setSQLArguments(ParsedSQLWithSimpleArgs tsql) throws SQLException {
        List<String> types = tsql.getArgs();
        if (types == null || types.isEmpty()) {
            return;
        }
        for (int i = 0; i < types.size(); i++) {
            converterStore.setSQLArgument(pstmt, i + 1, params[i], types.get(i));
        }
    }

}
