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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
import com.manniwood.cl4pg.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.util.ResourceUtil;
import com.manniwood.cl4pg.util.SqlCache;
import com.manniwood.cl4pg.util.Str;

public class PgSession {

    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_DATABASE = "postgres";
    public static final String DEFAULT_USERNAME = "postgres";
    public static final String DEFAULT_PASSWORD = "postgres";
    public static final String DEFAULT_APP_NAME = "cl4pg";
    public static final String DEFAULT_EXCEPTION_CONVERTER_CLASS = "com.manniwood.cl4pg.exceptionconverters.DefaultExceptionConverter";
    public static final int DEFAULT_TRANSACTION_ISOLATION_LEVEL = Connection.TRANSACTION_READ_COMMITTED;
    public static final String DEFAULT_CONF_FILE = "cl4pg.properties";
    public static final String HOSTNAME_KEY = "hostname";
    public static final String PORT_KEY = "port";
    public static final String DATABASE_KEY = "database";
    public static final String USERNAME_KEY = "user"; // ugly, but matches name
                                                      // in Pg Driver
    public static final String PASSWORD_KEY = "password";
    public static final String APP_NAME_KEY = "ApplicationName"; // ugly, but
                                                                 // matches name
                                                                 // in Pg Driver
    public static final String EXCEPTION_CONVERTER_KEY = "ExceptionConverter";
    public static final String TRANSACTION_ISOLATION_LEVEL_KEY = "TransactionIsolationLevel";

    private final static Logger log = LoggerFactory.getLogger(PgSession.class);

    private final SqlCache sqlCache = new SqlCache();

    private Connection conn = null;

    private ExceptionConverter exceptionConverter = null;
    private ConverterStore converterStore = new ConverterStore();

    public static PgSession.Builder configure() {
        return new PgSession.Builder();
    }

    public static PgSession buildFromDefaultConfFile() {
        return buildFromConfFile(DEFAULT_CONF_FILE);
    }

    public static PgSession buildFromConfFile(String path) {
        Properties props = new Properties();
        InputStream inStream = ResourceUtil.class.getClassLoader().getResourceAsStream(path);
        if (inStream == null) {
            throw new Cl4pgConfFileException("Could not find conf file \"" + path + "\"");
        }
        try {
            props.load(inStream);
        } catch (IOException e) {
            throw new Cl4pgConfFileException("Could not read conf file \"" + path + "\"", e);
        }

        Builder builder = new PgSession.Builder();

        String hostname = props.getProperty(HOSTNAME_KEY);
        if (!Str.isNullOrEmpty(hostname)) {
            builder.hostname(hostname);
        }

        String portStr = props.getProperty(PORT_KEY);
        if (!Str.isNullOrEmpty(portStr)) {
            builder.port(Integer.parseInt(portStr));
        }

        String database = props.getProperty(DATABASE_KEY);
        if (!Str.isNullOrEmpty(database)) {
            builder.database(database);
        }

        String username = props.getProperty(USERNAME_KEY);
        if (!Str.isNullOrEmpty(username)) {
            builder.username(username);
        }

        String password = props.getProperty(PASSWORD_KEY);
        if (!Str.isNullOrEmpty(password)) {
            builder.password(password);
        }

        String appName = props.getProperty(APP_NAME_KEY);
        if (!Str.isNullOrEmpty(appName)) {
            builder.appName(appName);
        }

        String exceptionConverterStr = props.getProperty(EXCEPTION_CONVERTER_KEY);
        if (!Str.isNullOrEmpty(exceptionConverterStr)) {
            builder.exceptionConverter(exceptionConverterStr);
        }

        String transactionIsolationLevelStr = props.getProperty(TRANSACTION_ISOLATION_LEVEL_KEY);
        if (!Str.isNullOrEmpty(transactionIsolationLevelStr)) {
            builder.transactionIsolationLevel(Integer.parseInt(transactionIsolationLevelStr));
        }

        return builder.done();
    }

    public static class Builder {
        private String hostname = DEFAULT_HOSTNAME;
        private int port = DEFAULT_PORT;
        private String database = DEFAULT_DATABASE;
        private String username = DEFAULT_USERNAME;
        private String password = DEFAULT_PASSWORD;
        private String appName = DEFAULT_APP_NAME;
        private String exceptionConverterStr = DEFAULT_EXCEPTION_CONVERTER_CLASS;
        private ExceptionConverter exceptionConverter = null;
        private int transactionIsolationLevel = DEFAULT_TRANSACTION_ISOLATION_LEVEL;

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

        public Builder exceptionConverter(String exceptionConverterStr) {
            this.exceptionConverterStr = exceptionConverterStr;
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
            if (this.exceptionConverter == null) {
                if (Str.isNullOrEmpty(this.exceptionConverterStr)) {
                    this.exceptionConverterStr = DEFAULT_EXCEPTION_CONVERTER_CLASS;
                }
                try {
                    Class<?> clazz = Class.forName(this.exceptionConverterStr);
                    Constructor<?> constructor = clazz.getDeclaredConstructor();
                    this.exceptionConverter = (ExceptionConverter) constructor.newInstance();
                } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                        | IllegalArgumentException | InvocationTargetException e) {
                    throw new Cl4pgReflectionException("Could not instantiate exception converter " + this.exceptionConverterStr, e);
                }
            }
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
            throw new Cl4pgReflectionException("Could not find PostgreSQL JDBC Driver", e);
        }
        String url = "jdbc:postgresql://" + builder.hostname + ":" + builder.port + "/" + builder.database;
        Properties props = new Properties();
        props.setProperty(USERNAME_KEY, builder.username);
        props.setProperty(PASSWORD_KEY, builder.password);
        props.setProperty(APP_NAME_KEY, builder.appName);
        log.info("Application Name: {}", builder.appName);
        try {
            conn = DriverManager.getConnection(url, props);
            conn.setTransactionIsolation(builder.transactionIsolationLevel);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new Cl4pgFailedConnectionException("Could not connect to db using the following url \"" + url + "\" and the following properties: " + props,
                                                     e);
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
