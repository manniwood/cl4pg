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
package com.manniwood.cl4pg.v1.datasourceadapters;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import com.manniwood.cl4pg.v1.ConfigDefaults;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.resultsethandlers.RowResultSetHandlerBuilder;
import com.manniwood.cl4pg.v1.resultsethandlers.ScalarResultSetHandlerBuilder;
import com.manniwood.cl4pg.v1.util.*;
import org.postgresql.PGConnection;
import org.postgresql.PGStatement;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfFileException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedConnectionException;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;

/**
 * PGSimpleDataSource implementation of DataSourceAdapter, with
 * PGSimpleDataSource-specific configuration, where appropriate. By default,
 * configures itself from cl4pg/PgSimpleDataSourceAdapter.properties found in
 * the classpath. Use this DataSource if you need each call to getConnection()
 * to return a brand new connection to PostgreSQL.
 *
 * @author mwood
 *
 */
public class PgSimpleDataSourceAdapter implements DataSourceAdapter {

    public static final String DEFAULT_CONF_FILE = ConfigDefaults.PROJ_NAME + "/" + PgSimpleDataSourceAdapter.class.getSimpleName() + ".properties";

    private final static Logger log = LoggerFactory.getLogger(PgSimpleDataSourceAdapter.class);

    private final SqlCache sqlCache = new SqlCache();
    private final ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder;
    private final RowResultSetHandlerBuilder rowResultSetHandlerBuilder;

    private final ExceptionConverter exceptionConverter;
    private final TypeConverterStore converterStore;

    private final PGSimpleDataSource ds;
    private final int transactionIsolationLevel;
    private final boolean autoCommit;

    @Override
    public PgSession getSession() {
        return new PgSession(this);
    }

    @Override
    public Connection getConnection() {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            conn.setTransactionIsolation(transactionIsolationLevel);
            conn.setAutoCommit(autoCommit);

            // conn.setCatalog("String catalog");  // Not supported by PostgreSQL
            // conn.setClientInfo(null);  // Covered by more direct connection setters that we are already using
            conn.setHoldability(0);  // TODO
            // conn.setNetworkTimeout(null, 0); // Not implemented by PostgreSQL
            conn.setReadOnly(false);  // TODO
            // conn.setSchema("String schema");  // Not implemented by PostgreSQL
            conn.setTypeMap(null);  // Map<String, Class<?>> TODO

            // PGConnection pgConn = (PGConnection) conn;
            // pgConn.setPrepareThreshold(0);  // Already gets set in data source
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

    @Override
    public PGStatement unwrapPgPreparedStatement(PreparedStatement pstmt) throws SQLException {
        return (PGStatement) pstmt;
    }

    @Override
    public PGStatement unwrapPgCallableStatement(CallableStatement cstmt) throws SQLException {
        return (PGStatement) cstmt;
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

        String scalarResultSetHandlerBuilder = props.getProperty(ConfigDefaults.SCALAR_RESULT_SET_HANDLER_BUILDER_KEY);
        if (!Str.isNullOrEmpty(scalarResultSetHandlerBuilder)) {
            builder.scalarResultSetHandlerBuilder(scalarResultSetHandlerBuilder);
        }

        String rowResultSetHandlerBuilder = props.getProperty(ConfigDefaults.ROW_RESULT_SET_HANDLER_BUILDER_KEY);
        if (!Str.isNullOrEmpty(rowResultSetHandlerBuilder)) {
            builder.rowResultSetHandlerBuilder(rowResultSetHandlerBuilder);
        }

        String transactionIsolationLevelStr = props.getProperty(ConfigDefaults.TRANSACTION_ISOLATION_LEVEL_KEY);
        if (!Str.isNullOrEmpty(transactionIsolationLevelStr)) {
            builder.transactionIsolationLevelName(transactionIsolationLevelStr);
        }

        String autoCommitStr = props.getProperty(ConfigDefaults.AUTO_COMMIT_KEY);
        if (!Str.isNullOrEmpty(autoCommitStr)) {
            builder.autoCommit(autoCommitStr);
        }

        String typeConverterConfFilesStr = props.getProperty(ConfigDefaults.TYPE_CONVERTER_CONF_FILES_KEY);
        if (!Str.isNullOrEmpty(typeConverterConfFilesStr)) {
            builder.typeConverterConfFiles(typeConverterConfFilesStr);
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
        private String typeConverterConfFiles = null;
        private String scalarResultSetHandlerBuilderStr = ConfigDefaults.DEFAULT_SCALAR_RESULT_SET_HANDLER_BUILDER;
        private ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder = null;
        private String rowResultSetHandlerBuilderStr = ConfigDefaults.DEFAULT_ROW_RESULT_SET_HANDLER_BUILDER;
        private RowResultSetHandlerBuilder rowResultSetHandlerBuilder = null;
        private boolean autoCommit = ConfigDefaults.DEFAULT_AUTO_COMMIT;
        private boolean binaryTransfer = ConfigDefaults.DEFAULT_BINARY_TRANSFER;
        private String binaryTransferEnable = ConfigDefaults.DEFAULT_BINARY_TRANSFER_ENABLE;
        private String binaryTransferDisable = ConfigDefaults.DEFAULT_BINARY_TRANSFER_DISABLE;
        private String compatible = ConfigDefaults.DEFAULT_COMPATIBLE;
        private boolean disableColumnSanitiser = ConfigDefaults.DEFAULT_DISABLE_COLUMN_SANITIZER;
        private int loginTimeout = ConfigDefaults.DEFAULT_LOGIN_TIMEOUT;
        private int logLevel = ConfigDefaults.DEFAULT_LOG_LEVEL;
        private PrintWriter logWriter = ConfigDefaults.DEFAULT_LOG_WRITER;
        private int prepareThreshold = ConfigDefaults.DEFAULT_PREPARE_THRESHOLD;
        private int protocolVersion = ConfigDefaults.DEFAULT_PROTOCOL_VERSION;
        private  int receiveBufferSize = ConfigDefaults.DEFAULT_RECEIVE_BUFFER_SIZE;
        private int sendBufferSize = ConfigDefaults.DEFAULT_SEND_BUFFER_SIZE;
        private String stringType = ConfigDefaults.DEFAULT_STRING_TYPE;
        private boolean ssl = ConfigDefaults.DEFAULT_SSL;
        private String sslFactory = ConfigDefaults.DEFAULT_SSL_FACTORY;
        private int socketTimeout = ConfigDefaults.DEFAULT_SOCKET_TIMEOOUT;

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

        public Builder scalarResultSetHandlerBuilder(String scalarResultSetHandlerBuilderStr) {
            this.scalarResultSetHandlerBuilderStr = scalarResultSetHandlerBuilderStr;
            return this;
        }

        public Builder scalarResultSetHandlerBuilder(ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder) {
            this.scalarResultSetHandlerBuilder = scalarResultSetHandlerBuilder;
            return this;
        }

        public Builder rowResultSetHandlerBuilder(String rowResultSetHandlerBuilderStr) {
            this.rowResultSetHandlerBuilderStr = rowResultSetHandlerBuilderStr;
            return this;
        }

        public Builder rowResultSetHandlerBuilder(RowResultSetHandlerBuilder rowResultSetHandlerBuilder) {
            this.rowResultSetHandlerBuilder = rowResultSetHandlerBuilder;
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

        public Builder transactionIsolationLevelName(String name) {
            this.transactionIsolationLevel = TransactionIsolationLevelConverter.convert(name);
            return this;
        }

        public Builder typeConverterConfFiles(String typeConverterConfFiles) {
            this.typeConverterConfFiles = typeConverterConfFiles;
            return this;
        }

        public Builder autoCommit(boolean autoCommit) {
            this.autoCommit = autoCommit;
            return this;
        }

        public Builder autoCommit(String autoCommitStr) {
            this.autoCommit = Boolean.parseBoolean(autoCommitStr);
            return this;
        }

        public Builder binaryTransfer(boolean binaryTransfer) {
            this.binaryTransfer = binaryTransfer;
            return this;
        }

        public Builder binaryTransfer(String binaryTransfer) {
            this.binaryTransfer = Boolean.parseBoolean(binaryTransfer);
            return this;
        }

        public Builder binaryTransferEnable(String binaryTransferEnable) {
            this.binaryTransferEnable = binaryTransferEnable;
            return this;
        }

        public Builder binaryTransferDisable(String binaryTransferDisable) {
            this.binaryTransferDisable = binaryTransferDisable;
            return this;
        }

        public Builder compatible(String compatible) {
            this.compatible = compatible;
            return this;
        }

        public Builder disableColumnSanitizer(boolean disableColumnSanitizer) {
            this.disableColumnSanitiser = disableColumnSanitizer;
            return this;
        }

        public Builder disableColumnSanitizer(String disableColumnSanitizer) {
            this.disableColumnSanitiser = Boolean.parseBoolean(disableColumnSanitizer);
            return this;
        }

        public Builder loginTimeout(int loginTimeout) {
            this.loginTimeout = loginTimeout;
            return this;
        }

        public Builder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder logWriter(PrintWriter logWriter) {
            this.logWriter = logWriter;
            return this;
        }
        // THOUGHT: make version of this that takes a string and instantiates the correct printwriter?
        // Would have to have a null constructor, though.

        public Builder prepareThreshold(int prepareThreshold) {
            this.prepareThreshold = prepareThreshold;
            return this;
        }

        public Builder protocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder receiveBufferSize(int receiveBufferSize) {
            this.receiveBufferSize = receiveBufferSize;
            return this;
        }

        public Builder sendBufferSize(int sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
            return this;
        }

        public Builder stringType(String stringType) {
            this.stringType = stringType;
            return this;
        }

        public Builder ssl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public Builder ssl(String ssl) {
            this.ssl = Boolean.parseBoolean(ssl);
            return this;
        }

        public Builder sslFactory(String sslFactory) {
            this.sslFactory = sslFactory;
            return this;
        }

        public Builder socketTimeout(int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public PgSimpleDataSourceAdapter done() {
            if (this.exceptionConverter == null) {
                if (Str.isNullOrEmpty(this.exceptionConverterStr)) {
                    this.exceptionConverterStr = ConfigDefaults.DEFAULT_EXCEPTION_CONVERTER_CLASS;
                }
                this.exceptionConverter = (ExceptionConverter) ReflectionUtil.instantiateUsingNullConstructor(this.exceptionConverterStr);
            }
            if (this.scalarResultSetHandlerBuilder == null) {
                if (Str.isNullOrEmpty(this.scalarResultSetHandlerBuilderStr)) {
                    this.scalarResultSetHandlerBuilderStr = ConfigDefaults.DEFAULT_SCALAR_RESULT_SET_HANDLER_BUILDER;
                }
                this.scalarResultSetHandlerBuilder = (ScalarResultSetHandlerBuilder) ReflectionUtil.instantiateUsingNullConstructor(this.scalarResultSetHandlerBuilderStr);
            }
            if (this.rowResultSetHandlerBuilder == null) {
                if (Str.isNullOrEmpty(this.rowResultSetHandlerBuilderStr)) {
                    this.rowResultSetHandlerBuilderStr = ConfigDefaults.DEFAULT_ROW_RESULT_SET_HANDLER_BUILDER;
                }
                this.rowResultSetHandlerBuilder = (RowResultSetHandlerBuilder) ReflectionUtil.instantiateUsingNullConstructor(this.rowResultSetHandlerBuilderStr);
            }
            return new PgSimpleDataSourceAdapter(this);
        }
    }

    private PgSimpleDataSourceAdapter() {
        exceptionConverter = null;
        converterStore = null;
        ds = null;
        transactionIsolationLevel = -1;
        scalarResultSetHandlerBuilder = null;
        rowResultSetHandlerBuilder = null;
        autoCommit = false;
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

        // Most defaults are listed in org.postgresql.ds.common.BaseDataSource
        ds.setBinaryTransfer(builder.binaryTransfer);
        ds.setBinaryTransferEnable(builder.binaryTransferEnable);
        ds.setBinaryTransferDisable(builder.binaryTransferDisable);
        ds.setCompatible(builder.compatible);
        ds.setDisableColumnSanitiser(builder.disableColumnSanitiser);
        try {
            ds.setLoginTimeout(builder.loginTimeout);
        } catch (SQLException e) {
            throw new Cl4pgFailedConnectionException("Connection to URL " + url + " failed while trying to set login timeout to <<TODO: APPEND LOGIN TIMEOUT>>", e);
        }
        ds.setLogLevel(builder.logLevel);
        try {
            ds.setLogWriter(builder.logWriter);
        } catch (SQLException e) {
            throw new Cl4pgFailedConnectionException("Connection to URL " + url + " failed while trying to set log writer <<TODO: APPEND LOGIN TIMEOUT>>", e);
        }
        ds.setPrepareThreshold(builder.prepareThreshold);
        ds.setProtocolVersion(builder.protocolVersion);
        ds.setReceiveBufferSize(builder.receiveBufferSize);
        ds.setSendBufferSize(builder.sendBufferSize);
        ds.setStringType(builder.stringType);
        ds.setSsl(builder.ssl);
        ds.setSslfactory(builder.sslFactory);
        ds.setSocketTimeout(builder.socketTimeout);
        ds.setTcpKeepAlive(builder.tcpKeepAlive);  // TODO false
        ds.setUnknownLength(builder.unknownLength);  // TODO Integer.MAX_VALUE

        log.info("Application Name: {}", builder.appName);
        transactionIsolationLevel = builder.transactionIsolationLevel;
        exceptionConverter = builder.exceptionConverter;
        converterStore = new TypeConverterStore(builder.typeConverterConfFiles);
        scalarResultSetHandlerBuilder = builder.scalarResultSetHandlerBuilder;
        rowResultSetHandlerBuilder = builder.rowResultSetHandlerBuilder;
        autoCommit = builder.autoCommit;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public TypeConverterStore getTypeConverterStore() {
        return converterStore;
    }

    @Override
    public SqlCache getSqlCache() {
        return sqlCache;
    }

    @Override
    public ScalarResultSetHandlerBuilder getScalarResultSetHandlerBuilder() {
        return scalarResultSetHandlerBuilder;
    }

    @Override
    public RowResultSetHandlerBuilder getRowResultSetHandlerBuilder() {
        return rowResultSetHandlerBuilder;
    }
}
