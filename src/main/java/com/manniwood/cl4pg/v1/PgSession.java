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
import java.util.List;

import org.postgresql.PGNotification;
import org.postgresql.util.PSQLException;

import com.manniwood.cl4pg.v1.commands.Command;
import com.manniwood.cl4pg.v1.commands.Commit;
import com.manniwood.cl4pg.v1.commands.DDL;
import com.manniwood.cl4pg.v1.commands.GetNotifications;
import com.manniwood.cl4pg.v1.commands.Insert;
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
import com.manniwood.cl4pg.v1.resultsethandlers.GuessConstructorListHandler;
import com.manniwood.cl4pg.v1.resultsethandlers.GuessScalarListHandler;
import com.manniwood.cl4pg.v1.resultsethandlers.ResultSetHandler;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;
import com.manniwood.cl4pg.v1.util.Cllctn;
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
    public void ddlF(String file) {
        run(DDL.config().file(file).done());
    }

    /**
     * Convenience method that calls an Insert Command using a bean argument and
     * a file in the classpath.
     */
    public <A> void insert(A a,
                           String sql) {
        run(Insert.<A> usingBeanArg()
                .sql(sql)
                .arg(a)
                .done());
    }

    /**
     * Convenience method that calls an Insert Command using a bean argument and
     * a file in the classpath.
     */
    public <A> void insertF(A a,
                            String file) {
        run(Insert.<A> usingBeanArg()
                .file(file)
                .arg(a)
                .done());
    }

    /**
     * Convenience method that calls an Insert Command using variadic args and a
     * file in the classpath.
     */
    public void insert(String sql,
                       Object... args) {
        run(Insert.usingVariadicArgs()
                .sql(sql)
                .args(args)
                .done());
    }

    /**
     * Convenience method that calls an Insert Command using variadic args and a
     * file in the classpath.
     */
    public void insertF(String file,
                        Object... args) {
        run(Insert.usingVariadicArgs()
                .file(file)
                .args(args)
                .done());
    }

    /**
     * Convenience method that calls a Select Command using variadic args and a
     * file in the classpath, which uses the names of the returned columns to
     * guess the constructor for the returned beans.
     */
    public <R> List<R> selectF(String file,
                               Class<R> clazz,
                               Object... args) {

        ResultSetHandler<R> handler = new GuessConstructorListHandler<R>(clazz);
        run(Select.<R> usingVariadicArgs()
                .file(file)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a Select Command using variadic args and a
     * file in the classpath, which uses the names of the returned columns to
     * guess the constructor for the returned beans.
     */
    public <R, A> List<R> selectF(A a,
                                  String file,
                                  Class<R> clazz) {

        ResultSetHandler<R> handler = new GuessConstructorListHandler<R>(clazz);
        run(Select.<A, R> usingBeanArg()
                .file(file)
                .arg(a)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a Select Command using variadic args and a
     * file in the classpath, which uses the names of the returned columns to
     * guess the constructor for the returned beans, and returns the first row
     * of the result set.
     */
    public <R> R selectOneF(String file,
                            Class<R> clazz,
                            Object... args) {
        List<R> list = selectF(file, clazz, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a Select Command using variadic args and a
     * file in the classpath, which uses the names of the returned columns to
     * guess the constructor for the returned beans, and returns the first row
     * of the result set.
     */
    public <R, A> R selectOneF(A a,
                               String file,
                               Class<R> clazz) {
        List<R> list = selectF(a, file, clazz);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a Select Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans.
     */
    public <R> List<R> select(String sql,
                              Class<R> clazz,
                              Object... args) {

        ResultSetHandler<R> handler = new GuessConstructorListHandler<R>(clazz);
        run(Select.<R> usingVariadicArgs()
                .sql(sql)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a Select Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans.
     */
    public <R, A> List<R> select(A a,
                                 String sql,
                                 Class<R> clazz) {

        ResultSetHandler<R> handler = new GuessConstructorListHandler<R>(clazz);
        run(Select.<A, R> usingBeanArg()
                .sql(sql)
                .arg(a)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a Select Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans, and returns the first row of the result set.
     */
    public <R> R selectOne(String sql,
                           Class<R> clazz,
                           Object... args) {
        List<R> list = select(sql, clazz, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a Select Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans, and returns the first row of the result set.
     */
    public <R, A> R selectOne(A a,
                              String sql,
                              Class<R> clazz) {
        List<R> list = select(a, sql, clazz);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method to call a Select Command using variadic args and a
     * file in the classpath, which uses the type of the single returned column
     * to return a list of scalar objects (Integer, String, etc).
     *
     * @param sql
     * @return
     */
    public <R> List<R> selectScalarF(String file,
                                     Object... args) {
        ResultSetHandler<R> handler = new GuessScalarListHandler<R>();
        run(Select.<R> usingVariadicArgs()
                .file(file)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method to call a Select Command using variadic args and a
     * file in the classpath, which uses the type of the single returned column
     * to return a single (first column, first row) scalar object (Integer,
     * String, etc). Good for sql queries such as "select * from foo".
     *
     * @param sql
     * @return
     */
    public <R> R selectOneScalarF(String file,
                                  Object... args) {
        List<R> list = selectScalarF(file, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method to call a Select Command using variadic args, which
     * uses the type of the single returned column to return a list of scalar
     * objects (Integer, String, etc).
     *
     * @param sql
     * @return
     */
    public <R> List<R> selectScalar(String sql,
                                    Object... args) {
        ResultSetHandler<R> handler = new GuessScalarListHandler<R>();
        run(Select.<R> usingVariadicArgs()
                .sql(sql)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method to call a Select Command using variadic args, which
     * uses the type of the single returned column to return a single (first
     * column, first row) scalar object (Integer, String, etc). Good for sql
     * queries such as "select * from foo".
     *
     * @param sql
     * @return
     */
    public <R> R selectOneScalar(String sql,
                                 Object... args) {
        List<R> list = selectScalar(sql, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
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

    /**
     * Examine an Exception and use the exceptionConverter to return either a
     * Cl4pgException, or a more specific sub-class of Cl4pgException.
     *
     * @param e
     * @param sql
     * @return
     */
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
