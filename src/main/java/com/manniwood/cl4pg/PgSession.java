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
package com.manniwood.cl4pg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.PGNotification;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.commands.Command;
import com.manniwood.cl4pg.commands.Commit;
import com.manniwood.cl4pg.commands.GetNotifications;
import com.manniwood.cl4pg.commands.Listen;
import com.manniwood.cl4pg.commands.Notify;
import com.manniwood.cl4pg.commands.Rollback;
import com.manniwood.cl4pg.converters.ConverterStore;
import com.manniwood.cl4pg.exceptionconverters.DefaultExceptionConverter;
import com.manniwood.cl4pg.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.util.SqlCache;

public class PgSession {

    private final static Logger log = LoggerFactory.getLogger(PgSession.class);

    private final SqlCache sqlCache = new SqlCache();

    private Connection conn = null;

    private ExceptionConverter exceptionConverter = new DefaultExceptionConverter();
    private ConverterStore converterStore = new ConverterStore();

    public static PgSession.Builder configure() {
        return new PgSession.Builder();
    }

    public static class Builder {
        private String hostname = "localhost";
        private int port = 5432;
        private String database = "postgres";
        private String username = "postgres";
        private String password = "postgres";
        private String appName = "Pg4j";
        private ExceptionConverter exceptionConverter = new DefaultExceptionConverter();
        private int transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;

        public Builder() {
            // null constructor
        }

        public Builder hostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder port(String portStr) {
            int port = Integer.parseInt(portStr);
            this.port = port;
            return this;
        }

        public Builder database(String database) {
            this.database = database;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder exceptionConverter(ExceptionConverter exceptionConverter) {
            this.exceptionConverter = exceptionConverter;
            return this;
        }

        public Builder transactionIsolationLevel(int transactionIsolationLevel) {
            this.transactionIsolationLevel = transactionIsolationLevel;
            return this;
        }

        public Builder transactionIsolationLevel(String transactionIsolationLevelStr) {
            int transactionIsolationLevel = Integer.parseInt(transactionIsolationLevelStr);
            if (!(transactionIsolationLevel == Connection.TRANSACTION_READ_COMMITTED
                    || transactionIsolationLevel == Connection.TRANSACTION_READ_UNCOMMITTED
                    || transactionIsolationLevel == Connection.TRANSACTION_REPEATABLE_READ
                    || transactionIsolationLevel == Connection.TRANSACTION_SERIALIZABLE)) {
                throw new IllegalArgumentException("Transaction Isolation Level \"" + transactionIsolationLevelStr + "\" is not valid.");
            }
            this.transactionIsolationLevel = transactionIsolationLevel;
            return this;
        }

        public PgSession done() {
            return new PgSession(this);
        }
    }

    private PgSession() {
    }

    private PgSession(Builder builder) {
        this.exceptionConverter = builder.exceptionConverter;
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new Cl4pgException("Could not find PostgreSQL JDBC Driver", e);
        }
        String url = "jdbc:postgresql://" + builder.hostname + ":" + builder.port + "/" + builder.database;
        Properties props = new Properties();
        props.setProperty("user", builder.username);
        props.setProperty("password", builder.password);
        props.setProperty("ApplicationName", builder.appName);
        log.info("Application Name: {}", builder.appName);
        try {
            conn = DriverManager.getConnection(url, props);
            conn.setTransactionIsolation(builder.transactionIsolationLevel);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new Cl4pgException("Could not connect to db", e);
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
            command.execute(conn, converterStore, sqlCache);
        } catch (Exception e) {
            rollback(e, command.getSQL());
            throw createPg4jException(e, command.getSQL());
        } finally {
            try {
                command.cleanUp();
            } catch (Exception e) {
                throw new Cl4pgException("Could not clean up after running the following SQL command; resources may have been left open! SQL command is:\n"
                        + command.getSQL(), e);
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
