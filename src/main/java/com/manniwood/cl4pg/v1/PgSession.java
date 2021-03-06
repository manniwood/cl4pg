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
import java.sql.SQLException;
import java.util.List;

import com.manniwood.cl4pg.v1.commands.*;
import com.manniwood.cl4pg.v1.datasourceadapters.DataSourceAdapter;
import com.manniwood.cl4pg.v1.resultsethandlers.RowResultSetHandlerBuilder;
import com.manniwood.cl4pg.v1.resultsethandlers.ScalarResultSetHandlerBuilder;
import org.postgresql.PGNotification;
import org.postgresql.util.PSQLException;

import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedCleanupException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedRollbackException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgPgSqlException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgSqlException;
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
public class PgSession implements Closeable {

    private final Connection conn;
    private final SqlCache sqlCache;
    private final DataSourceAdapter dataSourceAdapter;
    private final ExceptionConverter exceptionConverter;
    private final TypeConverterStore typeConverterStore;
    private final ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder;
    private final RowResultSetHandlerBuilder rowResultSetHandlerBuilder;

    public PgSession(Connection conn,
            DataSourceAdapter dataSourceAdapter,
            SqlCache sqlCache,
            ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder,
            RowResultSetHandlerBuilder rowResultSetHandlerBuilder) {
        this.conn = conn;
        this.dataSourceAdapter = dataSourceAdapter;
        this.exceptionConverter = dataSourceAdapter.getExceptionConverter();
        this.typeConverterStore = dataSourceAdapter.getTypeConverterStore();
        this.sqlCache = sqlCache;
        this.scalarResultSetHandlerBuilder = scalarResultSetHandlerBuilder;
        this.rowResultSetHandlerBuilder = rowResultSetHandlerBuilder;
    }

    public PgSession(DataSourceAdapter dataSourceAdapter) {
        this.conn = dataSourceAdapter.getConnection();
        this.dataSourceAdapter = dataSourceAdapter;
        this.exceptionConverter = dataSourceAdapter.getExceptionConverter();
        this.typeConverterStore = dataSourceAdapter.getTypeConverterStore();
        this.sqlCache = dataSourceAdapter.getSqlCache();
        this.scalarResultSetHandlerBuilder = dataSourceAdapter.getScalarResultSetHandlerBuilder();
        this.rowResultSetHandlerBuilder = dataSourceAdapter.getRowResultSetHandlerBuilder();
    }

    /**
     * Finishes this session, closing the underlying database connection.
     * (Closing the underlying database connection could mean actually closing
     * it, or just returning it to the DataSourceAdapter's connection pool; the
     * exact behaviour is left up to the DataSourceAdapter implementation.)
     */
    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new Cl4pgFailedRollbackException("Could not close database connection. Possible leaked resource!", e);
        }
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

    /**
     * Convenience method that calls a Commit Command.
     */
    public void commit() {
        run(new Commit(conn));
    }

    /**
     * Convenience method that calls a Rollback Command.
     */
    public void rollback() {
        run(new Rollback(conn));
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
     */
    public void pgListen(String channel) {
        run(new Listen(channel));
    }

    /**
     * Convenience method that calls a GetNotifications Command.
     */
    public PGNotification[] getNotifications() {
        GetNotifications getNotifications = new GetNotifications();
        run(getNotifications);
        return getNotifications.getNotifications();
    }

    /**
     * Convenience method that calls a CopyFileOut Command
     */
    public void qCopyOut(String sql, String outFile) {
        run(CopyFileOut.config()
                .copyFile(outFile)
                .sql(sql)
                .done());
    }

    /**
     * Convenience method that calls a CopyFileOut Command
     */
    public void copyOut(String file, String outFile) {
        run(CopyFileOut.config()
                .copyFile(outFile)
                .file(file)
                .done());
    }


    /**
     * Convenience method that calls a CopyFileIn Command
     */
    public void qCopyIn(String sql, String inFile) {
        run(CopyFileIn.config()
                .copyFile(inFile)
                .sql(sql)
                .done());
    }

    /**
     * Convenience method that calls a CopyFileIn Command
     */
    public void copyIn(String file, String inFile) {
        run(CopyFileIn.config()
                .copyFile(inFile)
                .file(file)
                .done());
    }

    /**
     * Convenience method that calls a DDL Command.
     *
     * @param sql
     */
    public void qDdl(String sql) {
        run(DDL.config().sql(sql).done());
    }

    /**
     * Convenience method that calls a DDL Command from a file in the classpath.
     *
     * @param file
     */
    public void ddl(String file) {
        run(DDL.config().file(file).done());
    }

    /**
     * Convenience method that calls an Insert Command using a bean argument.
     */
    public <A> void qInsert(A arg,
                            String sql) {
        run(Insert.<A> usingBeanArg()
                .sql(sql)
                .arg(arg)
                .done());
    }

    /**
     * Convenience method that calls an Insert Command using a bean argument and
     * a file in the classpath.
     */
    public <A> void insert(A arg,
                           String file) {
        run(Insert.<A> usingBeanArg()
                .file(file)
                .arg(arg)
                .done());
    }

    /**
     * Convenience method that calls an Insert Command using variadic args.
     */
    public void qInsert(String sql,
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
    public void insert(String file,
                       Object... args) {
        run(Insert.usingVariadicArgs()
                .file(file)
                .args(args)
                .done());
    }

    /**
     * Convenience method that calls a Select Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans.
     */
    public <R> List<R> qSelect(String sql,
                               Class<R> returnClass,
                               Object... args) {

        ResultSetHandler<R> handler = rowResultSetHandlerBuilder.build(returnClass);
        run(Select.<R> usingVariadicArgs()
                .sql(sql)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a Select Command using a bean to populate the args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans.
     */
    public <R, A> List<R> qSelect(A arg,
                                  String sql,
                                  Class<R> returnClass) {

        ResultSetHandler<R> handler = rowResultSetHandlerBuilder.build(returnClass);
        run(Select.<R, A> usingBeanArg()
                .sql(sql)
                .arg(arg)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a Select Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans, and returns the first row of the result set.
     */
    public <R> R qSelectOne(String sql,
                            Class<R> returnClass,
                            Object... args) {
        List<R> list = qSelect(sql, returnClass, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a Select Command using a bean to populate the args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans, and returns the first row of the result set.
     */
    public <R, A> R qSelectOne(A arg,
                               String sql,
                               Class<R> returnClass) {
        List<R> list = qSelect(arg, sql, returnClass);
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
    public <R> List<R> qSelectScalar(String sql,
                                     Object... args) {
        ResultSetHandler<R> handler = scalarResultSetHandlerBuilder.build();
        run(Select.<R> usingVariadicArgs()
                .sql(sql)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }


    /**
     * Convenience method to call a Select Command using a bean to populate the args, which
     * uses the type of the single returned column to return a list of scalar
     * objects (Integer, String, etc).
     *
     * @param sql
     * @return
     */
    public <R, A> List<R> qSelectScalar(A arg,
                                        String sql) {
        ResultSetHandler<R> handler = scalarResultSetHandlerBuilder.build();
        run(Select.<R, A> usingBeanArg()
                .sql(sql)
                .arg(arg)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method to call a Select Command using a bean to populate the args, which
     * uses the type of the single returned column to return a single (first
     * column, first row) scalar object (Integer, String, etc). Good for sql
     * queries such as "select * from foo".
     *
     * @param sql
     * @return
     */
    public <R, A> R qSelectOneScalar(A arg,
                                     String sql) {
        List<R> list = qSelectScalar(arg, sql);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
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
    public <R> R qSelectOneScalar(String sql,
                                  Object... args) {
        List<R> list = qSelectScalar(sql, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a Select Command using variadic args and a
     * file in the classpath, which uses the names of the returned columns to
     * guess the constructor for the returned beans.
     */
    public <R> List<R> select(String file,
                              Class<R> returnClass,
                              Object... args) {

        ResultSetHandler<R> handler = rowResultSetHandlerBuilder.build(returnClass);
        run(Select.<R> usingVariadicArgs()
                .file(file)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a Select Command using a bean to populate the args and a
     * file in the classpath, which uses the names of the returned columns to
     * guess the constructor for the returned beans.
     */
    public <R, A> List<R> select(A arg,
                                 String file,
                                 Class<R> returnClass) {

        ResultSetHandler<R> handler = rowResultSetHandlerBuilder.build(returnClass);
        run(Select.<R, A> usingBeanArg()
                .file(file)
                .arg(arg)
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
    public <R> R selectOne(String file,
                           Class<R> returnClass,
                           Object... args) {
        List<R> list = select(file, returnClass, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a Select Command using a bean to set the args and a
     * file in the classpath, which uses the names of the returned columns to
     * guess the constructor for the returned beans, and returns the first row
     * of the result set.
     */
    public <R, A> R selectOne(A arg,
                              String file,
                              Class<R> returnClass) {
        List<R> list = select(arg, file, returnClass);
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
     * @param file
     * @return
     */
    public <R> List<R> selectScalar(String file,
                                    Object... args) {
        ResultSetHandler<R> handler = scalarResultSetHandlerBuilder.build();
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
     * @param file
     * @return
     */
    public <R> R selectOneScalar(String file,
                                 Object... args) {
        List<R> list = selectScalar(file, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a StoredProcInOut Command using a single
     * bean argument and a file in the classpath. The names of the getters and
     * setters for bean A are used to perform IN and OUT duties for the parameters
     * of the stored procedure.
     */
    public <A> void procInOut(A a,
                              String file) {
        run(CallStoredProcInOut.<A> config()
                .file(file)
                .arg(a)
                .done());
    }


    /**
     * Convenience method that calls a StoredProcInOut Command using a single
     * bean argument and a SQL string. The names of the getters and
     * setters for bean A are used to perform IN and OUT duties for the parameters
     * of the stored procedure.
     */
    public <A> void qProcInOut(A a,
                              String sql) {
        run(CallStoredProcInOut.<A> config()
                .sql(sql)
                .arg(a)
                .done());
    }


    /**
     * Convenience method that calls a CallStoredProcRefCursor Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans.
     */
    public <R> List<R> qProcSelect(String sql,
                               Class<R> returnClass,
                               Object... args) {

        ResultSetHandler<R> handler = rowResultSetHandlerBuilder.build(returnClass);
        run(CallStoredProcRefCursor.<R> usingVariadicArgs()
                .sql(sql)
                .args(args)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a CallStoredProcRefCursor Command using an
     * argument bean to populate the arguments, and which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans.
     */
    public <R, A> List<R> qProcSelect(A arg,
                                  String sql,
                                  Class<R> returnClass) {

        ResultSetHandler<R> handler = rowResultSetHandlerBuilder.build(returnClass);
        run(CallStoredProcRefCursor.<R, A> usingBeanArg()
                .sql(sql)
                .arg(arg)
                .resultSetHandler(handler)
                .done());
        return handler.getList();
    }

    /**
     * Convenience method that calls a CallStoredProcRefCursor Command using variadic args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans, and returns the first row of the result set.
     */
    public <R> R qProcSelectOne(String sql,
                            Class<R> returnClass,
                            Object... args) {
        List<R> list = qProcSelect(sql, returnClass, args);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Convenience method that calls a CallStoredProcRefCursor Command using a bean to set the SQL args, which
     * uses the names of the returned columns to guess the constructor for the
     * returned beans, and returns the first row of the result set.
     */
    public <R, A> R qProcSelectOne(A arg,
                               String sql,
                               Class<R> returnClass) {
        List<R> list = qProcSelect(arg, sql, returnClass);
        if (Cllctn.isNullOrEmpty(list)) {
            return null;
        }
        return list.get(0);
    }

}
