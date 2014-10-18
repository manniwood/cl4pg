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
import com.manniwood.cl4pg.v1.commands.GetNotifications;
import com.manniwood.cl4pg.v1.commands.Listen;
import com.manniwood.cl4pg.v1.commands.Notify;
import com.manniwood.cl4pg.v1.commands.Rollback;
import com.manniwood.cl4pg.v1.converters.ConverterStore;
import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedCleanupException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedRollbackException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgPgSqlException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgSqlException;
import com.manniwood.cl4pg.v1.util.SqlCache;

public class PgSession {

    private Connection conn = null;
    private final SqlCache sqlCache;

    private DataSourceAdapter dataSourceAdapter = null;
    private ExceptionConverter exceptionConverter = null;
    private ConverterStore converterStore = new ConverterStore();

    public PgSession(Connection conn, DataSourceAdapter dataSourceAdapter, SqlCache sqlCache) {
        this.conn = conn;
        this.dataSourceAdapter = dataSourceAdapter;
        this.exceptionConverter = dataSourceAdapter.getExceptionConverter();
        this.sqlCache = sqlCache;
    }

    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new Cl4pgFailedRollbackException("Could not close databse connection. Possible leaked resource!", e);
        }
    }

    public void pgNotify(String channel,
                         String payload) {
        run(Notify.config().channel(channel).payload(payload).done());
    }

    public void pgListen(String channel) {
        run(new Listen(channel));
    }

    public PGNotification[] getNotifications() {
        GetNotifications getNotifications = new GetNotifications();
        run(getNotifications);
        return getNotifications.getNotifications();
    }

    public void commit() {
        run(new Commit(conn));
    }

    public void rollback() {
        run(new Rollback(conn));
    }

    public void run(Command command) {
        try {
            command.execute(conn, converterStore, sqlCache, dataSourceAdapter);
        } catch (Exception e) {
            rollback(e, command.getSQL());
            throw createPg4jException(e, command.getSQL());
        } finally {
            try {
                command.cleanUp();
            } catch (Exception e) {
                throw new Cl4pgFailedCleanupException("Could not clean up after running the following SQL command; resources may have been left open! SQL command is:\n"
                                                              + command.getSQL(),
                                                      e);
            }
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
