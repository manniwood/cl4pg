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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.commands.Command;
import com.manniwood.mpjw.commands.Commit;
import com.manniwood.mpjw.commands.DDL;
import com.manniwood.mpjw.commands.Delete;
import com.manniwood.mpjw.commands.DeleteVariadic;
import com.manniwood.mpjw.commands.Insert;
import com.manniwood.mpjw.commands.Rollback;
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
import com.manniwood.mpjw.commands.Update;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.mpjw.util.ResourceUtil;

public class PGSession {

    private final static Logger log = LoggerFactory.getLogger(PGSession.class);

    private Connection conn = null;
    private String hostname = "localhost";
    private int dbPort = 5432;
    private String dbName = "postgres";
    private String dbUser = "postgres";
    private String dbPassword = "postgres";
    private int transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;
    private String appName = "MPJW";

    private ConverterStore converterStore = new ConverterStore();

    public PGSession() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new MPJWException("Could not find PostgreSQL JDBC Driver", e);
        }
        String url = "jdbc:postgresql://" + hostname + ":" + dbPort + "/" + dbName;
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

    public <T> void insertB(String insert, T t) {
        String sql = resolveSQL(insert);
        CommandRunner.execute(new Insert<T>(converterStore, sql, conn, t));
    }

    public <T> int updateB(String insert, T t) {
        String sql = resolveSQL(insert);
        Update<T> d = new Update<T>(converterStore, sql, conn, t);
        CommandRunner.execute(d);
        return d.getNumberOfRowsUpdated();
    }

    public <T> int deleteB(String insert, T t) {
        String sql = resolveSQL(insert);
        Delete<T> d = new Delete<T>(converterStore, sql, conn, t);
        CommandRunner.execute(d);
        return d.getNumberOfRowsDeleted();
    }

    public int deleteV(String insert, Object...params) {
        String sql = resolveSQL(insert);
        DeleteVariadic d = new DeleteVariadic(converterStore, sql, conn, params);
        CommandRunner.execute(d);
        return d.getNumberOfRowsDeleted();
    }

    public <T> T selectOneVGuessSetters(String sqlFile, Class<T> returnType, Object... params) {
        List<T> list = selectListVGuessSetters(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T> T selectOneVGuessConstructor(String sqlFile, Class<T> returnType, Object... params) {
        List<T> list = selectListVGuessConstructor(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T> T selectOneVSpecifySetters(String sqlFile, Class<T> returnType, Object... params) {
        List<T> list = selectListVSpecifySetters(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T> T selectOneVSpecifyConstructor(String sqlFile, Class<T> returnType, Object... params) {
        List<T> list = selectListVSpecifyConstructor(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }


    public <T, P> T selectOneBGuessSetters(String sqlFile, Class<T> returnType, P p) {
        List<T> list = selectListBGuessSetters(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T, P> T selectOneBGuessConstructor(String sqlFile, Class<T> returnType, P p) {
        List<T> list = selectListBGuessConstructor(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T, P> T selectOneBSpecifySetters(String sqlFile, Class<T> returnType, P p) {
        List<T> list = selectListBSpecifySetters(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T, P> T selectOneBSpecifyConstructor(String sqlFile, Class<T> returnType, P p) {
        List<T> list = selectListBSpecifyConstructor(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> selectListVGuessSetters(String sqlFile, Class<T> returnType, Object... params) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListVariadicGuessSetters<T>(converterStore, sql, conn, returnType, params);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> selectListVGuessConstructor(String sqlFile, Class<T> returnType, Object... params) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListVariadicGuessConstructor<T>(converterStore, sql, conn, returnType, params);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> selectListVSpecifySetters(String sqlFile, Class<T> returnType, Object... params) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListVariadicSpecifySetters<T>(converterStore, sql, conn, returnType, params);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> selectListVSpecifyConstructor(String sqlFile, Class<T> returnType, Object... params) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListVariadicSpecifyConstructor<T>(converterStore, sql, conn, returnType, params);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T, P> List<T> selectListBGuessSetters(String sqlFile, Class<T> returnType, P p) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListBeanGuessSetters<T, P>(converterStore, sql, conn, returnType, p);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T, P> List<T> selectListBGuessConstructor(String sqlFile, Class<T> returnType, P p) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListBeanGuessConstructor<T, P>(converterStore, sql, conn, returnType, p);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T, P> List<T> selectListBSpecifySetters(String sqlFile, Class<T> returnType, P p) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListBeanSpecifySetters<T, P>(converterStore, sql, conn, returnType, p);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T, P> List<T> selectListBSpecifyConstructor(String sqlFile, Class<T> returnType, P p) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListBeanSpecifyConstructor<T, P>(converterStore, sql, conn, returnType, p);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }


    @SuppressWarnings("unchecked")
    public <T> List<T> selectListVGuessScalar(String sqlFile, Class<T> returnType, Object... params) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListVariadicGuessScalar<T>(converterStore, sql, conn, returnType, params);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> selectListVSpecifyScalar(String sqlFile, Class<T> returnType, Object... params) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListVariadicSpecifyScalar<T>(converterStore, sql, conn, returnType, params);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T, P> List<T> selectListBGuessScalar(String sqlFile, Class<T> returnType, P p) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListBeanGuessScalar<T, P>(converterStore, sql, conn, returnType, p);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }

    @SuppressWarnings("unchecked")
    public <T, P> List<T> selectListBSpecifyScalar(String sqlFile, Class<T> returnType, P p) {
        String sql = resolveSQL(sqlFile);
        Command command = new SelectListBeanSpecifyScalar<T, P>(converterStore, sql, conn, returnType, p);
        CommandRunner.execute(command);
        return ((SelectListBase<T>)command).getResult();
    }


    public <T> T selectOneVGuessScalar(String sqlFile, Class<T> returnType, Object... params) {
        List<T> list = selectListVGuessScalar(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T> T selectOneVSpecifyScalar(String sqlFile, Class<T> returnType, Object... params) {
        List<T> list = selectListVSpecifyScalar(sqlFile, returnType, params);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T, P> T selectOneBGuessScalar(String sqlFile, Class<T> returnType, P p) {
        List<T> list = selectListBGuessScalar(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
    }

    public <T, P> T selectOneBSpecifyScalar(String sqlFile, Class<T> returnType, P p) {
        List<T> list = selectListBSpecifyScalar(sqlFile, returnType, p);
        if (list == null || list.isEmpty()) {
            return null;
        }
        if (list.size() > 1) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + sqlFile);
        }
        return list.get(0);
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

    /**
     * Resolves str to either a plain sql statement, or a
     * file in the classpath that contains sql; which is
     * slurped in an returned as sql.
     * @param str
     * @return
     */
    public String resolveSQL(String str) {
        // XXX: Keep a cache of file contents based on their file names
        // so that you do not read them off disk
        // whenever they are requested.
        if (str == null
                || str.length() < 2 /* leave room for '@' */) {
            throw new MPJWException("SQL string null or too short.");
        }
        if (str.startsWith("@")) {
            str = ResourceUtil.slurpFileFromClasspath(str.substring(1) /* remove leading '@' */);
        }
        return str;
    }

}
