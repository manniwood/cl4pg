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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.PGConnection;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfFileException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedConnectionException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgReflectionException;
import com.manniwood.cl4pg.v1.util.ResourceUtil;
import com.manniwood.cl4pg.v1.util.Str;

public class PgSimpleDataSourceAdapter implements DataSourceAdapter {

    public static final String DEFAULT_CONF_FILE = ConfigDefaults.PROJ_NAME + "/" + PgSimpleDataSourceAdapter.class.getSimpleName() + ".properties";

    private final static Logger log = LoggerFactory.getLogger(PgSimpleDataSourceAdapter.class);

    private ExceptionConverter exceptionConverter;

    private PGSimpleDataSource ds;
    private int transactionIsolationLevel;

    @Override
    public Connection getConnection() {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setTransactionIsolation(transactionIsolationLevel);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new Cl4pgFailedConnectionException("Could not get connection.", e);
        }
        return conn;
    }

    @Override
    public ExceptionConverter getExceptionConverter() {
        return exceptionConverter;
    }

    @Override
    public PGConnection unwrapPgConnection(Connection conn) throws SQLException {
        return (PGConnection) conn;
    }

    public static PgSimpleDataSourceAdapter.Builder configure() {
        return new PgSimpleDataSourceAdapter.Builder();
    }

    public static PgSimpleDataSourceAdapter buildFromDefaultConfFile() {
        return buildFromConfFile(DEFAULT_CONF_FILE);
    }

    public static PgSimpleDataSourceAdapter buildFromConfFile(String path) {
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

        Builder builder = new PgSimpleDataSourceAdapter.Builder();

        String hostname = props.getProperty(ConfigDefaults.HOSTNAME_KEY);
        if (!Str.isNullOrEmpty(hostname)) {
            builder.hostname(hostname);
        }

        String portStr = props.getProperty(ConfigDefaults.PORT_KEY);
        if (!Str.isNullOrEmpty(portStr)) {
            builder.port(Integer.parseInt(portStr));
        }

        String database = props.getProperty(ConfigDefaults.DATABASE_KEY);
        if (!Str.isNullOrEmpty(database)) {
            builder.database(database);
        }

        String username = props.getProperty(ConfigDefaults.USERNAME_KEY);
        if (!Str.isNullOrEmpty(username)) {
            builder.username(username);
        }

        String password = props.getProperty(ConfigDefaults.PASSWORD_KEY);
        if (!Str.isNullOrEmpty(password)) {
            builder.password(password);
        }

        String appName = props.getProperty(ConfigDefaults.APP_NAME_KEY);
        if (!Str.isNullOrEmpty(appName)) {
            builder.appName(appName);
        }

        String exceptionConverterStr = props.getProperty(ConfigDefaults.EXCEPTION_CONVERTER_KEY);
        if (!Str.isNullOrEmpty(exceptionConverterStr)) {
            builder.exceptionConverter(exceptionConverterStr);
        }

        String transactionIsolationLevelStr = props.getProperty(ConfigDefaults.TRANSACTION_ISOLATION_LEVEL_KEY);
        if (!Str.isNullOrEmpty(transactionIsolationLevelStr)) {
            builder.transactionIsolationLevel(Integer.parseInt(transactionIsolationLevelStr));
        }

        return builder.done();
    }

    public static class Builder {
        private String hostname = ConfigDefaults.DEFAULT_HOSTNAME;
        private int port = ConfigDefaults.DEFAULT_PORT;
        private String database = ConfigDefaults.DEFAULT_DATABASE;
        private String username = ConfigDefaults.DEFAULT_USERNAME;
        private String password = ConfigDefaults.DEFAULT_PASSWORD;
        private String appName = ConfigDefaults.DEFAULT_APP_NAME;
        private String exceptionConverterStr = ConfigDefaults.DEFAULT_EXCEPTION_CONVERTER_CLASS;
        private ExceptionConverter exceptionConverter = null;
        private int transactionIsolationLevel = ConfigDefaults.DEFAULT_TRANSACTION_ISOLATION_LEVEL;

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

        public PgSimpleDataSourceAdapter done() {
            if (this.exceptionConverter == null) {
                if (Str.isNullOrEmpty(this.exceptionConverterStr)) {
                    this.exceptionConverterStr = ConfigDefaults.DEFAULT_EXCEPTION_CONVERTER_CLASS;
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
            return new PgSimpleDataSourceAdapter(this);
        }
    }

    private PgSimpleDataSourceAdapter() {
    }

    private PgSimpleDataSourceAdapter(Builder builder) {
        ds = new PGSimpleDataSource();
        String url = "jdbc:postgresql://" + builder.hostname + ":" + builder.port + "/" + builder.database;
        try {
            ds.setUrl(url);
        } catch (SQLException e) {
            throw new Cl4pgFailedConnectionException("Connection to URL " + url + " failed.", e);
        }
        ds.setUser(builder.username);
        ds.setPassword(builder.password);
        ds.setApplicationName(builder.appName);
        log.info("Application Name: {}", builder.appName);
        transactionIsolationLevel = builder.transactionIsolationLevel;
        exceptionConverter = builder.exceptionConverter;
    }

}
