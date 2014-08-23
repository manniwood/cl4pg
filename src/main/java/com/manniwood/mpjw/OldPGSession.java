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
package com.manniwood.mpjw;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.commands.CallStoredProc;
import com.manniwood.mpjw.commands.CallStoredProcReturnScalar;
import com.manniwood.mpjw.commands.Commit;
import com.manniwood.mpjw.commands.CopyFileIn;
import com.manniwood.mpjw.commands.CopyFileOut;
import com.manniwood.mpjw.commands.DDL;
import com.manniwood.mpjw.commands.Delete;
import com.manniwood.mpjw.commands.DeleteVariadic;
import com.manniwood.mpjw.commands.GetNotifications;
import com.manniwood.mpjw.commands.Insert;
import com.manniwood.mpjw.commands.Listen;
import com.manniwood.mpjw.commands.Notify;
import com.manniwood.mpjw.commands.OldCommand;
import com.manniwood.mpjw.commands.Rollback;
import com.manniwood.mpjw.commands.SPSelectListBase;
import com.manniwood.mpjw.commands.SPSelectListBeanGuessConstructor;
import com.manniwood.mpjw.commands.SelectListBase;
import com.manniwood.mpjw.commands.SelectListBeanGuessConstructor;
import com.manniwood.mpjw.commands.SelectListBeanGuessScalar;
import com.manniwood.mpjw.commands.SelectListBeanGuessSetters;
import com.manniwood.mpjw.commands.SelectListBeanSpecifyConstructor;
import com.manniwood.mpjw.commands.SelectListBeanSpecifyScalar;
import com.manniwood.mpjw.commands.SelectListBeanSpecifySetters;
import com.manniwood.mpjw.commands.SelectListVariadicGuessConstructor;
import com.manniwood.mpjw.commands.SelectListVariadicGuessScalar;
import com.manniwood.mpjw.commands.SelectListVariadicGuessSetters;
import com.manniwood.mpjw.commands.SelectListVariadicSpecifyConstructor;
import com.manniwood.mpjw.commands.SelectListVariadicSpecifyScalar;
import com.manniwood.mpjw.commands.SelectListVariadicSpecifySetters;
import com.manniwood.mpjw.commands.SelectUsingListenerBean;
import com.manniwood.mpjw.commands.SelectUsingListenerVariadic;
import com.manniwood.mpjw.commands.Update;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.mpjw.util.ResourceUtil;
import com.manniwood.pg4j.commands.Command;

public class OldPGSession {

    private final static Logger log                       = LoggerFactory
                                                                  .getLogger(OldPGSession.class);

    private Connection          conn                      = null;

    // XXX: make all of these dynamically settable
    private String              hostname                  = "localhost";
    private int                 dbPort                    = 5432;
    private String              dbName                    = "postgres";
    private String              dbUser                    = "postgres";
    private String              dbPassword                = "postgres";
    private int                 transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;
    private String              appName                   = "MPJW";

    private ConverterStore      converterStore            = new ConverterStore();

    public OldPGSession() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new MPJWException("Could not find PostgreSQL JDBC Driver", e);
        }
        String url = "jdbc:postgresql://" + hostname + ":" + dbPort + "/"
                + dbName;
        Properties props = new Properties();
        props.setProperty("user", dbUser);
        props.setProperty("password", dbPassword);
        props.setProperty("ApplicationName", appName);
        log.info("Application Name: {}", appName);
        try {
            conn = DriverManager.getConnection(url, props);
            conn.setTransactionIsolation(transactionIsolationLevel);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new MPJWException("Could not connect to db", e);
        }
    }

    public <T> void insert(T t,
                           String insert) {
        String sql = resolveSQL(insert);
        CommandRunner.execute(new Insert<T>(converterStore, sql, conn, t));
    }

    public <T> int update(T t,
                          String insert) {
        String sql = resolveSQL(insert);
        Update<T> d = new Update<T>(converterStore, sql, conn, t);
        CommandRunner.execute(d);
        return d.getNumberOfRowsUpdated();
    }

    public <T> int delete(T t,
                          String insert) {
        String sql = resolveSQL(insert);
        Delete<T> d = new Delete<T>(converterStore, sql, conn, t);
        CommandRunner.execute(d);
        return d.getNumberOfRowsDeleted();
    }

    public int delete(String insert,
                      Object... params) {
        String sql = resolveSQL(insert);
        DeleteVariadic d = new DeleteVariadic(converterStore, sql, conn, params);
        CommandRunner.execute(d);
        return d.getNumberOfRowsDeleted();
    }

    public void select(String sqlFile,
                       ResultSetListener listener,
                       Object... params) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectUsingListenerVariadic(converterStore,
                                                                sql,
                                                                conn,
                                                                listener,
                                                                params);
        CommandRunner.execute(oldCommand);
    }

    public <P> void select(String sqlFile,
                           P p,
                           ResultSetListener listener) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectUsingListenerBean<P>(converterStore,
                                                               sql,
                                                               conn,
                                                               listener,
                                                               p);
        CommandRunner.execute(oldCommand);
    }

    public void run(Command command) {
        CommandRunner.execute(command, conn, converterStore);
    }

    public <T> T selectOne(ReturnStyle returnStyle,
                           String sqlFile,
                           Class<T> returnType,
                           Object... params) {
        switch (returnStyle) {
        case SCALAR_EXPLICIT:
            return selectOneVSpecifyScalar(sqlFile, returnType, params);
        case SCALAR_GUESSED:
            return selectOneVGuessScalar(sqlFile, returnType, params);
        case BEAN_EXPLICIT_CONS_ARGS:
            return selectOneVSpecifyConstructor(sqlFile, returnType, params);
        case BEAN_EXPLICIT_SETTERS:
            return selectOneVSpecifySetters(sqlFile, returnType, params);
        case BEAN_GUESSED_CONS_ARGS:
            return selectOneVGuessConstructor(sqlFile, returnType, params);
        case BEAN_GUESSED_SETTERS:
            return selectOneVGuessSetters(sqlFile, returnType, params);
        default:
            throw new MPJWException("Invalid returnStyle: " + returnStyle);
        }
    }

    public <T, P> T selectOne(String sqlFile,
                              Class<T> returnType,
                              P p,
                              ReturnStyle returnStyle) {
        switch (returnStyle) {
        case SCALAR_EXPLICIT:
            return selectOneBSpecifyScalar(sqlFile, returnType, p);
        case SCALAR_GUESSED:
            return selectOneBGuessScalar(sqlFile, returnType, p);
        case BEAN_EXPLICIT_CONS_ARGS:
            return selectOneBSpecifyConstructor(sqlFile, returnType, p);
        case BEAN_EXPLICIT_SETTERS:
            return selectOneBSpecifySetters(sqlFile, returnType, p);
        case BEAN_GUESSED_CONS_ARGS:
            return selectOneBGuessConstructor(sqlFile, returnType, p);
        case BEAN_GUESSED_SETTERS:
            return selectOneBGuessSetters(sqlFile, returnType, p);
        default:
            throw new MPJWException("Invalid returnStyle: " + returnStyle);
        }
    }

    public <T> List<T> selectList(ReturnStyle returnStyle,
                                  String sqlFile,
                                  Class<T> returnType,
                                  Object... params) {
        switch (returnStyle) {
        case SCALAR_EXPLICIT:
            return selectListVSpecifyScalar(sqlFile, returnType, params);
        case SCALAR_GUESSED:
            return selectListVGuessScalar(sqlFile, returnType, params);
        case BEAN_EXPLICIT_CONS_ARGS:
            return selectListVSpecifyConstructor(sqlFile, returnType, params);
        case BEAN_EXPLICIT_SETTERS:
            return selectListVSpecifySetters(sqlFile, returnType, params);
        case BEAN_GUESSED_CONS_ARGS:
            return selectListVGuessConstructor(sqlFile, returnType, params);
        case BEAN_GUESSED_SETTERS:
            return selectListVGuessSetters(sqlFile, returnType, params);
        default:
            throw new MPJWException("Invalid returnStyle: " + returnStyle);
        }
    }

    public <T, P> List<T> selectList(String sqlFile,
                                     Class<T> returnType,
                                     P p,
                                     ReturnStyle returnStyle) {
        switch (returnStyle) {
        case SCALAR_EXPLICIT:
            return selectListBSpecifyScalar(sqlFile, returnType, p);
        case SCALAR_GUESSED:
            return selectListBGuessScalar(sqlFile, returnType, p);
        case BEAN_EXPLICIT_CONS_ARGS:
            return selectListBSpecifyConstructor(sqlFile, returnType, p);
        case BEAN_EXPLICIT_SETTERS:
            return selectListBSpecifySetters(sqlFile, returnType, p);
        case BEAN_GUESSED_CONS_ARGS:
            return selectListBGuessConstructor(sqlFile, returnType, p);
        case BEAN_GUESSED_SETTERS:
            return selectListBGuessSetters(sqlFile, returnType, p);
        default:
            throw new MPJWException("Invalid returnStyle: " + returnStyle);
        }
    }

    public <T, P> T spSelectOne(String sqlFile,
                                Class<T> returnType,
                                P p,
                                ReturnStyle returnStyle) {
        switch (returnStyle) {
        case SCALAR_EXPLICIT: // TODO
            return selectOneBSpecifyScalar(sqlFile, returnType, p);
        case SCALAR_GUESSED: // TODO
            return selectOneBGuessScalar(sqlFile, returnType, p);
        case BEAN_EXPLICIT_CONS_ARGS: // TODO
            return selectOneBSpecifyConstructor(sqlFile, returnType, p);
        case BEAN_EXPLICIT_SETTERS: // TODO
            return selectOneBSpecifySetters(sqlFile, returnType, p);
        case BEAN_GUESSED_CONS_ARGS:
            return spSelectOneBGuessConstructor(sqlFile, returnType, p);
        case BEAN_GUESSED_SETTERS: // TODO
            return selectOneBGuessSetters(sqlFile, returnType, p);
        default:
            throw new MPJWException("Invalid returnStyle: " + returnStyle);
        }
    }

    private <T> T selectOneVGuessSetters(String sqlFile,
                                         Class<T> returnType,
                                         Object... params) {
        List<T> list = selectListVGuessSetters(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T> T selectOneVGuessConstructor(String sqlFile,
                                             Class<T> returnType,
                                             Object... params) {
        List<T> list = selectListVGuessConstructor(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T> T selectOneVSpecifySetters(String sqlFile,
                                           Class<T> returnType,
                                           Object... params) {
        List<T> list = selectListVSpecifySetters(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T> T selectOneVSpecifyConstructor(String sqlFile,
                                               Class<T> returnType,
                                               Object... params) {
        List<T> list = selectListVSpecifyConstructor(sqlFile,
                                                     returnType,
                                                     params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T, P> T selectOneBGuessSetters(String sqlFile,
                                            Class<T> returnType,
                                            P p) {
        List<T> list = selectListBGuessSetters(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T, P> T selectOneBGuessConstructor(String sqlFile,
                                                Class<T> returnType,
                                                P p) {
        List<T> list = selectListBGuessConstructor(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T, P> T spSelectOneBGuessConstructor(String sqlFile,
                                                  Class<T> returnType,
                                                  P p) {
        List<T> list = spSelectListBGuessConstructor(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T, P> T selectOneBSpecifySetters(String sqlFile,
                                              Class<T> returnType,
                                              P p) {
        List<T> list = selectListBSpecifySetters(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T, P> T selectOneBSpecifyConstructor(String sqlFile,
                                                  Class<T> returnType,
                                                  P p) {
        List<T> list = selectListBSpecifyConstructor(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> selectListVGuessSetters(String sqlFile,
                                                Class<T> returnType,
                                                Object... params) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListVariadicGuessSetters<T>(converterStore,
                                                                      sql,
                                                                      conn,
                                                                      returnType,
                                                                      params);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> selectListVGuessConstructor(String sqlFile,
                                                    Class<T> returnType,
                                                    Object... params) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListVariadicGuessConstructor<T>(converterStore,
                                                                          sql,
                                                                          conn,
                                                                          returnType,
                                                                          params);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> selectListVSpecifySetters(String sqlFile,
                                                  Class<T> returnType,
                                                  Object... params) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListVariadicSpecifySetters<T>(converterStore,
                                                                        sql,
                                                                        conn,
                                                                        returnType,
                                                                        params);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> selectListVSpecifyConstructor(String sqlFile,
                                                      Class<T> returnType,
                                                      Object... params) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListVariadicSpecifyConstructor<T>(converterStore,
                                                                            sql,
                                                                            conn,
                                                                            returnType,
                                                                            params);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T, P> List<T> selectListBGuessSetters(String sqlFile,
                                                   Class<T> returnType,
                                                   P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListBeanGuessSetters<T, P>(converterStore,
                                                                     sql,
                                                                     conn,
                                                                     returnType,
                                                                     p);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T, P> List<T> selectListBGuessConstructor(String sqlFile,
                                                       Class<T> returnType,
                                                       P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListBeanGuessConstructor<T, P>(converterStore,
                                                                         sql,
                                                                         conn,
                                                                         returnType,
                                                                         p);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T, P> List<T> spSelectListBGuessConstructor(String sqlFile,
                                                         Class<T> returnType,
                                                         P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SPSelectListBeanGuessConstructor<T, P>(converterStore,
                                                                           sql,
                                                                           conn,
                                                                           returnType,
                                                                           p);
        CommandRunner.execute(oldCommand);
        return ((SPSelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T, P> List<T> selectListBSpecifySetters(String sqlFile,
                                                     Class<T> returnType,
                                                     P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListBeanSpecifySetters<T, P>(converterStore,
                                                                       sql,
                                                                       conn,
                                                                       returnType,
                                                                       p);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T, P> List<T> selectListBSpecifyConstructor(String sqlFile,
                                                         Class<T> returnType,
                                                         P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListBeanSpecifyConstructor<T, P>(converterStore,
                                                                           sql,
                                                                           conn,
                                                                           returnType,
                                                                           p);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> selectListVGuessScalar(String sqlFile,
                                               Class<T> returnType,
                                               Object... params) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListVariadicGuessScalar<T>(converterStore,
                                                                     sql,
                                                                     conn,
                                                                     returnType,
                                                                     params);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> selectListVSpecifyScalar(String sqlFile,
                                                 Class<T> returnType,
                                                 Object... params) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListVariadicSpecifyScalar<T>(converterStore,
                                                                       sql,
                                                                       conn,
                                                                       returnType,
                                                                       params);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T, P> List<T> selectListBGuessScalar(String sqlFile,
                                                  Class<T> returnType,
                                                  P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListBeanGuessScalar<T, P>(converterStore,
                                                                    sql,
                                                                    conn,
                                                                    returnType,
                                                                    p);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    @SuppressWarnings("unchecked")
    private <T, P> List<T> selectListBSpecifyScalar(String sqlFile,
                                                    Class<T> returnType,
                                                    P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new SelectListBeanSpecifyScalar<T, P>(converterStore,
                                                                      sql,
                                                                      conn,
                                                                      returnType,
                                                                      p);
        CommandRunner.execute(oldCommand);
        return ((SelectListBase<T>) oldCommand).getResult();
    }

    private <T> T selectOneVGuessScalar(String sqlFile,
                                        Class<T> returnType,
                                        Object... params) {
        List<T> list = selectListVGuessScalar(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T> T selectOneVSpecifyScalar(String sqlFile,
                                          Class<T> returnType,
                                          Object... params) {
        List<T> list = selectListVSpecifyScalar(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T, P> T selectOneBGuessScalar(String sqlFile,
                                           Class<T> returnType,
                                           P p) {
        List<T> list = selectListBGuessScalar(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    private <T, P> T selectOneBSpecifyScalar(String sqlFile,
                                             Class<T> returnType,
                                             P p) {
        List<T> list = selectListBSpecifyScalar(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n"
                    + sqlFile);
        }
        return list.get(0);
    }

    public void pgNotify(String channel,
                         String payload) {
        CommandRunner
                .execute(new Notify(converterStore, conn, channel, payload));
    }

    public void pgListen(String channel) {
        Listen listen = new Listen(conn, channel);
        CommandRunner.execute(listen);
    }

    public PGNotification[] getNotifications() {
        GetNotifications n = new GetNotifications(conn);
        CommandRunner.execute(n);
        return n.getNotifications();
    }

    public void ddl(String ddl) {
        String sql = resolveSQL(ddl);
        CommandRunner.execute(new DDL(sql, conn));
    }

    public void dml(String dml) {
        String sql = resolveSQL(dml);
        CommandRunner.execute(new DDL(sql, conn));
    }

    public void commit() {
        CommandRunner.execute(new Commit(conn));
    }

    public void rollback() {
        CommandRunner.execute(new Rollback(conn));
    }

    // TODO: make a version of this that just takes a reader, too
    public void copyIn(String copyFileName,
                       String sql) {
        CommandRunner.execute(new CopyFileIn(conn, copyFileName, sql));
    }

    // TODO: make a version of this that just takes a reader, too
    public void copyOut(String copyFileName,
                        String sql) {
        CommandRunner.execute(new CopyFileOut(conn, copyFileName, sql));
    }

    // TODO: make copy between two connections

    public <T> void callProc(String sqlFile,
                             T t) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new CallStoredProc<T>(converterStore, sql, conn, t);
        CommandRunner.execute(oldCommand);
    }

    @SuppressWarnings("unchecked")
    public <T, P> T callProcReturnScalar(String sqlFile,
                                         Class<T> returnType,
                                         P p) {
        String sql = resolveSQL(sqlFile);
        OldCommand oldCommand = new CallStoredProcReturnScalar<T, P>(converterStore,
                                                                     sql,
                                                                     conn,
                                                                     returnType,
                                                                     p);
        CommandRunner.execute(oldCommand);
        return ((CallStoredProcReturnScalar<T, P>) oldCommand).getResult();
    }

    /**
     * Resolves str to either a plain sql statement, or a file in the classpath
     * that contains sql; which is slurped in an returned as sql.
     *
     * @param str
     * @return
     */
    public static String resolveSQL(String str) {
        // XXX: Keep a cache of file contents based on their file names
        // so that you do not read them off disk
        // whenever they are requested.
        if (str == null || str.length() < 2 /* leave room for '@' */) {
            throw new MPJWException("SQL string null or too short.");
        }
        if (str.startsWith("@")) {
            str = ResourceUtil.slurpFileFromClasspath(str.substring(1) /*
                                                                        * remove
                                                                        * leading
                                                                        * '@'
                                                                        */);
        }
        return str;
    }

}