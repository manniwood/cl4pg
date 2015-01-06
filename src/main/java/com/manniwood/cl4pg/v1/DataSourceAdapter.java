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
package com.manniwood.cl4pg.v1;

import java.io.Closeable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.manniwood.cl4pg.v1.util.SqlCache;
import org.postgresql.PGConnection;
import org.postgresql.PGStatement;

import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;

/**
 * Adapts a data source for use by the rest of Cl4Pg. For the most part, Cl4Pg
 * is best off not knowing the implementation details of any particular data
 * source it is using. That way, it can use data sources from different
 * connection pool implementations, for instance. However, seeing as Cl4Pg is a
 * library that encourages the full use of PostgreSQL, each implementation of
 * this interface needs to provide ways of unwrapping the underlying parts of
 * PgJDBC, so that PostgreSQL-specific commands like listen/notify or copy will
 * work.
 *
 * @author mwood
 *
 */
public interface DataSourceAdapter extends Closeable {

    /**
     * Get a connection from the DataSource. Each DataSource gets to determine
     * what getting a connection means: Pooling DataSources will get a
     * connection from a pool, whereas simpler DataSources will return a new
     * connection to PostgreSQL with each call to this method.
     *
     * @return
     */
    Connection getConnection();

    /**
     * Get a PgSession, which wraps a DataSource.
     */
    PgSession getSession();


    /**
     * Get the ExceptionConverter used by this DataSourceAdapter.
     * @return
     */
    ExceptionConverter getExceptionConverter();

    /**
     * Get the SqlCache used by this DataSourceAdapter
     * @return
     */
    SqlCache getSqlCache();

    /**
     * Get the ScalarResultSetHandlerBuilder used by this DataSourceAdapter
     * @return
     */
    ScalarResultSetHandlerBuilder getScalarResultSetHandlerBuilder();

    /**
     * Get the RowResultSetHandlerBuilder used by this DataSourceAdapter
     * @return
     */
    RowResultSetHandlerBuilder getRowResultSetHandlerBuilder();

    /**
     * Returns the underlying PGConnection wrapped by this Connection.
     *
     * @param conn
     * @return
     * @throws SQLException
     */
    PGConnection unwrapPgConnection(Connection conn) throws SQLException;

    /**
     * Returns the underlying PGStatement wrapped by this PreparedStatement.
     *
     * @param pstmt
     * @return
     * @throws SQLException
     */
    PGStatement unwrapPgPreparedStatement(PreparedStatement pstmt) throws SQLException;

    /**
     * Returns the underlying PGStatement wrapped by this CallableStatement.
     *
     * @param cstmt
     * @return
     * @throws SQLException
     */
    PGStatement unwrapPgCallableStatement(CallableStatement cstmt) throws SQLException;

    /**
     * Close the DataSource. Each DataSource gets to determine
     * what closing the DataSource means: Pooling DataSources will close all connections
     * in the connection pool, whereas simpler DataSources will probably do nothing.
     *
     * @return
     */
    void close();

    TypeConverterStore getTypeConverterStore();
}
