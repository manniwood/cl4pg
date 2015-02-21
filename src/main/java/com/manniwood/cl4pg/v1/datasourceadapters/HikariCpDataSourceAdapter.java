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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfFileException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedConnectionException;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.proxy.CallableStatementProxy;
import com.zaxxer.hikari.proxy.ConnectionProxy;
import com.zaxxer.hikari.proxy.PreparedStatementProxy;

/**
 * HikariCP implementation of DataSourceAdapter, with HikariCP-specific
 * configuration, where appropriate. By default, configures itself from
 * cl4pg/HikariCpDataSourceAdapter.properties found in the classpath. Use this
 * DataSourceAdapter if you need high-performance connection pooling.
 *
 * @author mwood
 *
 */
public class HikariCpDataSourceAdapter implements DataSourceAdapter {

    public static final String DEFAULT_CONF_FILE = ConfigDefaults.PROJ_NAME + "/" + HikariCpDataSourceAdapter.class.getSimpleName() + ".properties";

    // Attributes unique to this data source
    public static final String DATA_SOURCE_NAME_KEY = "dataSourceName";
    public static final String DEFAULT_DATA_SOURCE_NAME = null;
    public static final String INITIAL_CONNECTIONS_KEY = "initialConnections";
    public static final int DEFAULT_INITIAL_CONNECTIONS = 5;
    public static final String MAX_CONNECTIONS_KEY = "maxConnections";
    public static final int DEFAULT_MAX_CONNECTIONS = 20;
    public static final String CONNECTION_CUSTOMIZER_CLASS_NAME_KEY = "connectionCustomizerClassName";
    public static final String DEFAULT_CONNECTION_CUSTOMIZER_CLASS_NAME = null;

    private static final Logger log = LoggerFactory.getLogger(HikariCpDataSourceAdapter.class);

    private final SqlCache sqlCache = new SqlCache();
    private final ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder;
    private final RowResultSetHandlerBuilder rowResultSetHandlerBuilder;

    private final ExceptionConverter exceptionConverter;
    private final TypeConverterStore converterStore;

    private final HikariDataSource ds;

    @Override
    public PgSession getSession() {
        return new PgSession(this);
    }

    @Override
    public Connection getConnection() {
        Connection conn = null;
        try {
            conn = ds.getConnection();
            // HikariCP should have done this for us: conn.setTransactionIsolation(transactionIsolationLevel);
            // HikariCP should have done this for us: conn.setAutoCommit(autoCommit);
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
        ConnectionProxy proxy = (ConnectionProxy) conn;
        return proxy.<PGConnection> unwrap(PGConnection.class);
    }

    @Override
    public PGStatement unwrapPgPreparedStatement(PreparedStatement pstmt) throws SQLException {
        PreparedStatementProxy proxy = (PreparedStatementProxy) pstmt;
        return proxy.<PGStatement> unwrap(PGStatement.class);
    }

    @Override
    public PGStatement unwrapPgCallableStatement(CallableStatement cstmt) throws SQLException {
        CallableStatementProxy proxy = (CallableStatementProxy) cstmt;
        return proxy.<PGStatement> unwrap(PGStatement.class);
    }

    public static HikariCpDataSourceAdapter.Builder configure() {
        return new HikariCpDataSourceAdapter.Builder();
    }

    public static HikariCpDataSourceAdapter buildFromDefaultConfFile() {
        return buildFromConfFile(DEFAULT_CONF_FILE);
    }

    public static HikariCpDataSourceAdapter buildFromConfFile(String path) {
        Properties props = new Properties();
        InputStream inStream = ResourceUtil.class.getClassLoader().getResourceAsStream(path);
        if (inStream == null) {
            throw new Cl4pgConfFileException("Could not find conf file \"" + path + "\" in classpath");
        }
        try {
            props.load(inStream);
        } catch (IOException e) {
            throw new Cl4pgConfFileException("Could not read conf file \"" + path + "\"", e);
        }

        Builder builder = new HikariCpDataSourceAdapter.Builder();

        String hostname = PropsUtil.getPropFromAll(props, ConfigDefaults.HOSTNAME_KEY);
        if (!Str.isNullOrEmpty(hostname)) {
            builder.hostname(hostname);
        }

        String portStr = PropsUtil.getPropFromAll(props, ConfigDefaults.PORT_KEY);
        if (!Str.isNullOrEmpty(portStr)) {
            builder.port(Integer.parseInt(portStr));
        }

        String database = PropsUtil.getPropFromAll(props, ConfigDefaults.DATABASE_KEY);
        if (!Str.isNullOrEmpty(database)) {
            builder.database(database);
        }

        String username = PropsUtil.getPropFromAll(props, ConfigDefaults.USERNAME_KEY);
        if (!Str.isNullOrEmpty(username)) {
            builder.username(username);
        }

        String password = PropsUtil.getPropFromAll(props, ConfigDefaults.PASSWORD_KEY);
        if (!Str.isNullOrEmpty(password)) {
            builder.password(password);
        }

        String appName = PropsUtil.getPropFromAll(props, ConfigDefaults.APP_NAME_KEY);
        if (!Str.isNullOrEmpty(appName)) {
            builder.appName(appName);
        }

        String exceptionConverterStr = PropsUtil.getPropFromAll(props, ConfigDefaults.EXCEPTION_CONVERTER_KEY);
        if (!Str.isNullOrEmpty(exceptionConverterStr)) {
            builder.exceptionConverter(exceptionConverterStr);
        }

        String transactionIsolationLevelStr = PropsUtil.getPropFromAll(props, ConfigDefaults.TRANSACTION_ISOLATION_LEVEL_KEY);
        if (!Str.isNullOrEmpty(transactionIsolationLevelStr)) {
            builder.transactionIsolationLevelName(transactionIsolationLevelStr);
        }

        String scalarResultSetHandlerBuilder = PropsUtil.getPropFromAll(props, ConfigDefaults.SCALAR_RESULT_SET_HANDLER_BUILDER_KEY);
        if (!Str.isNullOrEmpty(scalarResultSetHandlerBuilder)) {
            builder.scalarResultSetHandlerBuilder(scalarResultSetHandlerBuilder);
        }

        String typeConverterConfFilesStr = PropsUtil.getPropFromAll(props, ConfigDefaults.TYPE_CONVERTER_CONF_FILES_KEY);
        if (!Str.isNullOrEmpty(typeConverterConfFilesStr)) {
            builder.typeConverterConfFiles(typeConverterConfFilesStr);
        }

        String rowResultSetHandlerBuilder = PropsUtil.getPropFromAll(props, ConfigDefaults.ROW_RESULT_SET_HANDLER_BUILDER_KEY);
        if (!Str.isNullOrEmpty(rowResultSetHandlerBuilder)) {
            builder.rowResultSetHandlerBuilder(rowResultSetHandlerBuilder);
        }

        String autoCommitStr = PropsUtil.getPropFromAll(props, ConfigDefaults.AUTO_COMMIT_KEY);
        if (!Str.isNullOrEmpty(autoCommitStr)) {
            builder.autoCommit(autoCommitStr);
        }

        String binaryTransferStr = PropsUtil.getPropFromAll(props, ConfigDefaults.BINARY_TRANSFER_KEY);
        if (!Str.isNullOrEmpty(binaryTransferStr)) {
            builder.binaryTransfer(binaryTransferStr);
        }

        String binaryTransferEnable = PropsUtil.getPropFromAll(props, ConfigDefaults.BINARY_TRANSFER_ENABLE_KEY);
        if (!Str.isNullOrEmpty(binaryTransferEnable)) {
            builder.binaryTransferEnable(binaryTransferEnable);
        }

        String binaryTransferDisable = PropsUtil.getPropFromAll(props, ConfigDefaults.BINARY_TRANSFER_DISABLE_KEY);
        if (!Str.isNullOrEmpty(binaryTransferDisable)) {
            builder.binaryTransferDisable(binaryTransferDisable);
        }

        String compatible = PropsUtil.getPropFromAll(props, ConfigDefaults.COMPATIBLE_KEY);
        if (!Str.isNullOrEmpty(compatible)) {
            builder.compatible(compatible);
        }

        String disableColumnSanitizer = PropsUtil.getPropFromAll(props, ConfigDefaults.DISABLE_COLUMN_SANITIZER_KEY);
        if (!Str.isNullOrEmpty(disableColumnSanitizer)) {
            builder.disableColumnSanitizer(disableColumnSanitizer);
        }

        String loginTimeout = PropsUtil.getPropFromAll(props, ConfigDefaults.LOGIN_TIMEOUT_KEY);
        if (!Str.isNullOrEmpty(loginTimeout)) {
            builder.loginTimeout(loginTimeout);
        }

        String logLevel = PropsUtil.getPropFromAll(props, ConfigDefaults.LOG_LEVEL_KEY);
        if (!Str.isNullOrEmpty(logLevel)) {
            builder.logLevel(logLevel);
        }

        // skipping logWriter because it is of type PrintWriter; figure out later

        String prepareThreshold = PropsUtil.getPropFromAll(props, ConfigDefaults.PREPARE_THRESHOLD_KEY);
        if (!Str.isNullOrEmpty(prepareThreshold)) {
            builder.prepareThreshold(prepareThreshold);
        }

        String protocolVersion = PropsUtil.getPropFromAll(props, ConfigDefaults.PROTOCOL_VERSION_KEY);
        if (!Str.isNullOrEmpty(protocolVersion)) {
            builder.protocolVersion(protocolVersion);
        }

        String receiveBufferSize = PropsUtil.getPropFromAll(props, ConfigDefaults.RECEIVE_BUFFER_SIZE_KEY);
        if (!Str.isNullOrEmpty(receiveBufferSize)) {
            builder.receiveBufferSize(receiveBufferSize);
        }

        String sendBufferSize = PropsUtil.getPropFromAll(props, ConfigDefaults.SEND_BUFFER_SIZE_KEY);
        if (!Str.isNullOrEmpty(sendBufferSize)) {
            builder.sendBufferSize(sendBufferSize);
        }

        String stringtype = PropsUtil.getPropFromAll(props, ConfigDefaults.STRING_TYPE_KEY);
        if (!Str.isNullOrEmpty(stringtype)) {
            builder.stringType(stringtype);
        }

        String ssl = PropsUtil.getPropFromAll(props, ConfigDefaults.SSL_KEY);
        if (!Str.isNullOrEmpty(ssl)) {
            builder.ssl(ssl);
        }

        String sslfactory = PropsUtil.getPropFromAll(props, ConfigDefaults.SSL_FACTORY_KEY);
        if (!Str.isNullOrEmpty(sslfactory)) {
            builder.sslFactory(sslfactory);
        }

        String socketTimeout = PropsUtil.getPropFromAll(props, ConfigDefaults.SOCKET_TIMEOUT_KEY);
        if (!Str.isNullOrEmpty(socketTimeout)) {
            builder.socketTimeout(socketTimeout);
        }

        String tcpKeepAlive = PropsUtil.getPropFromAll(props, ConfigDefaults.TCP_KEEP_ALIVE_KEY);
        if (!Str.isNullOrEmpty(tcpKeepAlive)) {
            builder.tcpKeepAlive(tcpKeepAlive);
        }

        String unknownLength = PropsUtil.getPropFromAll(props, ConfigDefaults.UNKNOWN_LENGTH_KEY);
        if (!Str.isNullOrEmpty(unknownLength)) {
            builder.unknownLength(unknownLength);
        }

        String readOnly = PropsUtil.getPropFromAll(props, ConfigDefaults.READ_ONLY_KEY);
        if (!Str.isNullOrEmpty(readOnly)) {
            builder.readOnly(readOnly);
        }

        String holdability = PropsUtil.getPropFromAll(props, ConfigDefaults.HOLDABILITY_KEY);
        if (!Str.isNullOrEmpty(holdability)) {
            builder.holdability(holdability);
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
        private boolean tcpKeepAlive = ConfigDefaults.DEFAULT_TCP_KEEP_ALIVE;
        private int unknownLength = ConfigDefaults.DEFAULT_UNKNOWN_LENGTH;
        private boolean readOnly = ConfigDefaults.DEFAULT_READ_ONLY;
        private int holdability = ConfigDefaults.DEFAULT_HOLDABILITY;
        private int initialConnections = DEFAULT_INITIAL_CONNECTIONS;
        private int maxConnections = DEFAULT_MAX_CONNECTIONS;
        private String dataSourceName = DEFAULT_DATA_SOURCE_NAME;
        private String connectionCustomizerClassName = DEFAULT_CONNECTION_CUSTOMIZER_CLASS_NAME;

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

        public Builder port(String port) {
            this.port = Integer.parseInt(port);
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

        public Builder loginTimeout(String loginTimeOut) {
            this.loginTimeout = Integer.parseInt(loginTimeOut);
            return this;
        }

        public Builder logLevel(int logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder logLevel(String logLevel) {
            this.logLevel = Integer.parseInt(logLevel);
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

        public Builder prepareThreshold(String prepareThreshold) {
            this.prepareThreshold = Integer.parseInt(prepareThreshold);
            return this;
        }

        public Builder protocolVersion(int protocolVersion) {
            this.protocolVersion = protocolVersion;
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            this.protocolVersion = Integer.parseInt(protocolVersion);
            return this;
        }

        public Builder receiveBufferSize(int receiveBufferSize) {
            this.receiveBufferSize = receiveBufferSize;
            return this;
        }

        public Builder receiveBufferSize(String receiveBufferSize) {
            this.receiveBufferSize = Integer.parseInt(receiveBufferSize);
            return this;
        }

        public Builder sendBufferSize(int sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
            return this;
        }

        public Builder sendBufferSize(String sendBufferSize) {
            this.sendBufferSize = Integer.parseInt(sendBufferSize);
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

        public Builder socketTimeout(String socketTimeout) {
            this.socketTimeout = Integer.parseInt(socketTimeout);
            return this;
        }

        public Builder tcpKeepAlive(boolean tcpKeepAlive) {
            this.tcpKeepAlive = tcpKeepAlive;
            return this;
        }

        public Builder tcpKeepAlive(String tcpKeepAlive) {
            this.tcpKeepAlive = Boolean.parseBoolean(tcpKeepAlive);
            return this;
        }

        public Builder unknownLength(int unknownLength) {
            this.unknownLength = unknownLength;
            return this;
        }

        public Builder unknownLength(String unknownLength) {
            this.unknownLength = Integer.parseInt(unknownLength);
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            this.readOnly = readOnly;
            return this;
        }

        public Builder readOnly(String readOnly) {
            this.readOnly = Boolean.parseBoolean(readOnly);
            return this;
        }

        public Builder holdability(int holdability) {
            this.holdability = holdability;
            return this;
        }

        public Builder holdability(String holdability) {
            this.holdability = Integer.parseInt(holdability);
            return this;
        }

        public Builder initialConnections(int initialConnections) {
            this.initialConnections = initialConnections;
            return this;
        }

        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder dataSourceName(String dataSourceName) {
            this.dataSourceName = dataSourceName;
            return this;
        }

        public HikariCpDataSourceAdapter done() {
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
            return new HikariCpDataSourceAdapter(this);
        }
    }

    private HikariCpDataSourceAdapter() {
        exceptionConverter = null;
        converterStore = null;
        ds = null;
        scalarResultSetHandlerBuilder = null;
        rowResultSetHandlerBuilder = null;
    }

    private HikariCpDataSourceAdapter(Builder builder) {
        HikariConfig config = new HikariConfig();
        String url = "jdbc:postgresql://" + builder.hostname + ":" + builder.port + "/" + builder.database;
        // config.setDataSourceClassName(PGSimpleDataSource.class.getName());
        config.setDriverClassName(org.postgresql.Driver.class.getName());
        config.setJdbcUrl(url);
        config.setUsername(builder.username);
        config.setPassword(builder.password);
        config.setMinimumIdle(builder.initialConnections);
        config.setMaximumPoolSize(builder.maxConnections);
        config.setAutoCommit(builder.autoCommit);
        config.setTransactionIsolation(TransactionIsolationLevelConverter.convert(builder.transactionIsolationLevel));

        if (!Str.isNullOrEmpty(builder.connectionCustomizerClassName)) {
            config.setConnectionCustomizerClassName(builder.connectionCustomizerClassName);
        }

        // TODO:
        /*
        config.setConnectionInitSql();
        config.setConnectionTestQuery();
        config.setConnectionTimeout();
        config.setIdleTimeout();
        config.setInitializationFailFast();
        config.setIsolateInternalQueries();
        config.setJdbc4ConnectionTest();
        config.setLeakDetectionThreshold();
        config.setMaxLifetime();
        config.setMetricsTrackerClassName();
        config.setPoolName();
        config.setReadOnly();
        config.setRegisterMbeans(); */

        // PostgreSQL-specific properties
        Properties props = new Properties();
        props.setProperty(ConfigDefaults.APP_NAME_KEY, builder.appName);
        config.setDataSourceProperties(props);

        log.info("Application Name: {}", builder.appName);
        exceptionConverter = builder.exceptionConverter;
        converterStore = new TypeConverterStore(builder.typeConverterConfFiles);
        scalarResultSetHandlerBuilder = builder.scalarResultSetHandlerBuilder;
        rowResultSetHandlerBuilder = builder.rowResultSetHandlerBuilder;

        ds = new HikariDataSource(config);
    }

    @Override
    public void close() {
        ds.close();
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
