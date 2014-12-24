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
package com.manniwood.cl4pg.v1;

import java.io.Closeable;
import java.sql.Connection;

import com.manniwood.cl4pg.v1.util.SqlCache;

/**
 * A thin wrapper around a DataSourceAdapter that provides PgSessions, which
 * wrap database Connections.
 *
 * @author mwood
 *
 */
public class PgSessionPool implements Closeable {

    private DataSourceAdapter dataSourceAdapter;
    private final SqlCache sqlCache = new SqlCache();
    private final ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder;
    private final RowResultSetHandlerBuilder rowResultSetHandlerBuilder;

    public PgSessionPool(DataSourceAdapter dataSourceAdapter) {
        this.dataSourceAdapter = dataSourceAdapter;
        this.scalarResultSetHandlerBuilder = new GuessScalarResultSetHandlerBuilder();
        this.rowResultSetHandlerBuilder = new GuessConstructorResultSetHandlerBuilder();
    }

    public PgSessionPool(DataSourceAdapter dataSourceAdapter,
            ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder,
            RowResultSetHandlerBuilder rowResultSetHandlerBuilder) {
        this.dataSourceAdapter = dataSourceAdapter;
        this.scalarResultSetHandlerBuilder = scalarResultSetHandlerBuilder;
        this.rowResultSetHandlerBuilder = rowResultSetHandlerBuilder;
    }

    public PgSession getSession() {
        Connection conn = dataSourceAdapter.getConnection();
        return new PgSession(conn,
                             dataSourceAdapter,
                             sqlCache,
                             scalarResultSetHandlerBuilder,
                             rowResultSetHandlerBuilder);
    }

    @Override
    public void close() {
        dataSourceAdapter.close();
    }

}
