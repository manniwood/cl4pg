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
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.commands.Commit;
import com.manniwood.mpjw.commands.DDL;
import com.manniwood.mpjw.commands.Delete;
import com.manniwood.mpjw.commands.DeleteBare;
import com.manniwood.mpjw.commands.Insert;
import com.manniwood.mpjw.commands.Rollback;
import com.manniwood.mpjw.commands.SelectOne;
import com.manniwood.mpjw.commands.SelectOneBare;
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
        try {
            conn = DriverManager.getConnection(url, props);
            conn.setTransactionIsolation(transactionIsolationLevel);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new MPJWException("Could not connect to db", e);
        }
    }

    public <T> void insert(String insert, T t) {
        String sql = resolveSQL(insert);
        CommandRunner.execute(new Insert<T>(converterStore, sql, conn, t));
    }

    public <T> int delete(String insert, T t) {
        String sql = resolveSQL(insert);
        Delete<T> d = new Delete<T>(converterStore, sql, conn, t);
        CommandRunner.execute(d);
        return d.getNumberOfRowsDeleted();
    }

    public int deleteBare(String insert, Object...params) {
        String sql = resolveSQL(insert);
        DeleteBare d = new DeleteBare(converterStore, sql, conn, params);
        CommandRunner.execute(d);
        return d.getNumberOfRowsDeleted();
    }

    public <T, P> T selectOne(String sqlFile, Class<T> returnType, P parameter) {
        String sql = resolveSQL(sqlFile);
        SelectOne<T, P> so = new SelectOne<T, P>(converterStore, sql, conn, returnType, parameter);
        CommandRunner.execute(so);
        return so.getResult();
    }

    public <T> T selectOneBare(String sqlFile, Class<T> returnType, Object... params) {
        String sql = resolveSQL(sqlFile);
        SelectOneBare<T> so = new SelectOneBare<T>(converterStore, sql, conn, returnType, params);
        CommandRunner.execute(so);
        return so.getResult();
    }

    public void ddl(String ddl) {
        String sql = resolveSQL(ddl);
        CommandRunner.execute(new DDL(sql, conn));
    }

    public void commit() {
        CommandRunner.execute(new Commit(conn));
    }

    public void rollback() {
        CommandRunner.execute(new Rollback(conn));
    }

    /**
     * Resolves sql to either a plain sql statement, or a
     * file in the classpath that contains sql; which is
     * slurped in an returned as sql.
     * @param str
     * @return
     */
    public String resolveSQL(String str) {
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
