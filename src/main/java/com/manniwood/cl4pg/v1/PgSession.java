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

import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.PGNotification;
import org.postgresql.util.PSQLException;

import com.manniwood.cl4pg.v1.commands.Command;
import com.manniwood.cl4pg.v1.commands.Commit;
import com.manniwood.cl4pg.v1.commands.DDL;
import com.manniwood.cl4pg.v1.commands.GetNotifications;
import com.manniwood.cl4pg.v1.commands.Listen;
import com.manniwood.cl4pg.v1.commands.Notify;
import com.manniwood.cl4pg.v1.commands.Rollback;
import com.manniwood.cl4pg.v1.commands.Select;
import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedCleanupException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedRollbackException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgPgSqlException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgSqlException;
import com.manniwood.cl4pg.v1.resultsethandlers.GuessScalarListHandler;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;
import com.manniwood.cl4pg.v1.util.SqlCache;

/**
 * The main way of interacting with PostgreSQL; instances of this class are used
 * to run all Commands.
 *
 * @author mwood
 *
 */
public class PgSession {

    private final Connection conn;
    private final SqlCache sqlCache;
    private final DataSourceAdapter dataSourceAdapter;
    private final ExceptionConverter exceptionConverter;
    private final TypeConverterStore typeConverterStore;

    public PgSession(Connection conn, DataSourceAdapter dataSourceAdapter, SqlCache sqlCache) {
        this.conn = conn;
        this.dataSourceAdapter = dataSourceAdapter;
        this.exceptionConverter = dataSourceAdapter.getExceptionConverter();
        this.typeConverterStore = dataSourceAdapter.getTypeConverterStore();
        this.sqlCache = sqlCache;
    }

    /**
     * Runs a Command, automatically rolling back and throwing a Cl4pgException
     * (or one of its children) in the event of an error, and automatically
     * closing any resources (such as open files for the Copy Command, or open
     * PreparedStatements for the Select Command) regardless of success or
     * failure.
     *
     * @param command
     */
    public void run(Command command) {
        try {
            command.execute(conn, typeConverterStore, sqlCache, dataSourceAdapter);
        } catch (Exception e) {
            rollback(e, command.getSQL());
            throw createPg4jException(e, command.getSQL());
        } finally {
            try {
                command.close();
            } catch (Exception e) {
                throw new Cl4pgFailedCleanupException("Could not clean up after running the following SQL command; resources may have been left open! SQL command is:\n"
                                                              + command.getSQL(),
                                                      e);
            }
        }
    }

    /**
     * Convenience method that calls a Notify Command.
     *
     * @param channel
     * @param payload
     */
    public void pgNotify(String channel,
                         String payload) {
        run(Notify.config().channel(channel).payload(payload).done());
    }

    /**
     * Convenience method that calls a Listen Command.
     *
     * @param channel
     * @param payload
     */
    public void pgListen(String channel) {
        run(new Listen(channel));
    }

    /**
     * Convenience method that calls a DDL Command.
     *
     * @param sql
     */
    public void ddl(String sql) {
        run(DDL.config().sql(sql).done());
    }

    /**
     * Convenience method that calls a DDL Command from a file in the classpath.
     *
     * @param sql
     */
    public void ddlFile(String file) {
        run(DDL.config().file(file).done());
    }

    /**
     * Convenience method to call a Select that takes no args and returns a one
     * colum, one row result, such as "select * from foo".
     *
     * @param sql
     * @return
     */
    public <T> T selectOneWithNoArgs(String sql) {
        GuessScalarListHandler<T> handler = new GuessScalarListHandler<T>();
        run(Select.usingVariadicArgs()
                .sql(sql)
                .resultSetHandler(handler)
                .done());
        return handler.getList().get(0);
    }

    /**
     * Convenience method that calls a GetNotifications Command.
     *
     * @param channel
     * @param payload
     */
    public PGNotification[] getNotifications() {
        GetNotifications getNotifications = new GetNotifications();
        run(getNotifications);
        return getNotifications.getNotifications();
    }

    /**
     * Convenience method that calls a Commit Command.
     *
     * @param channel
     * @param payload
     */
    public void commit() {
        run(new Commit(conn));
    }

    /**
     * Convenience method that calls a Rollback Command.
     *
     * @param channel
     * @param payload
     */
    public void rollback() {
        run(new Rollback(conn));
    }

    /**
     * Finishes this session, closing the underlying database connection.
     * (Closing the underlying database connection could mean actually closing
     * it, or just returning it to the DataSourceAdapter's connection pool; the
     * exact behaviour is left up to the DataSourceAdaper implementation.)
     */
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new Cl4pgFailedRollbackException("Could not close databse connection. Possible leaked resource!", e);
        }
    }

    private void rollback(Exception e,
                          String sql) {
        try {
            conn.rollback();
        } catch (Exception e1) {
            // put e inside e1, so the user has all of the exceptions
            e1.initCause(e);
            throw new Cl4pgFailedRollbackException("Could not roll back connection after catching exception trying to execute:\n" + sql, e1);
        }
    }

    private Cl4pgException createPg4jException(Exception e,
                                               String sql) {
        if (e instanceof PSQLException) {
            PSQLException psqle = (PSQLException) e;
            Cl4pgPgSqlException pe = new Cl4pgPgSqlException(psqle.getServerErrorMessage(),
                                                             "ROLLED BACK. Exception while trying to run this sql statement:\n" + sql,
                                                             e);
            return exceptionConverter.convert(pe);
        } else if (e instanceof SQLException) {
            return new Cl4pgSqlException("ROLLED BACK. Exception while trying to run this sql statement:\n" + sql, e);
        } else {
            return new Cl4pgException("ROLLED BACK. Exception while trying to run this sql statement:\n" + sql, e);
        }
    }
}
