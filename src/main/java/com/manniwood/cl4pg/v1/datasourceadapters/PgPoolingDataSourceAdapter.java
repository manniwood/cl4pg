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

import com.manniwood.cl4pg.v1.ConfigDefaults;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfFileException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgFailedConnectionException;
import com.manniwood.cl4pg.v1.resultsethandlers.RowResultSetHandlerBuilder;
import com.manniwood.cl4pg.v1.resultsethandlers.ScalarResultSetHandlerBuilder;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;
import com.manniwood.cl4pg.v1.util.PropsUtil;
import com.manniwood.cl4pg.v1.util.ReflectionUtil;
import com.manniwood.cl4pg.v1.util.ResourceUtil;
import com.manniwood.cl4pg.v1.util.SqlCache;
import com.manniwood.cl4pg.v1.util.Str;
import com.manniwood.cl4pg.v1.util.TransactionIsolationLevelConverter;
import org.postgresql.PGConnection;
import org.postgresql.PGStatement;
import org.postgresql.ds.PGPoolingDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

/**
 * PGPoolingDataSource implementation of DataSourceAdapter, with
 * PGPoolingDataSource-specific configuration, where appropriate. By default,
 * configures itself from cl4pg/PgPoolingDataSourceAdapter.properties found in
 * the classpath. Use this DataSourceAdapter if you need rudimentary connection
 * pooling.
 *
 * @author mwood
 *
 */
public class PgPoolingDataSourceAdapter implements DataSourceAdapter {

    public static final String DEFAULT_CONF_FILE = ConfigDefaults.PROJ_NAME + "/" + PgPoolingDataSourceAdapter.class.getSimpleName() + ".properties";

    // Attributes unique to this data source
    public static final String DATA_SOURCE_NAME_KEY = "dataSourceName";
    public static final String DEFAULT_DATA_SOURCE_NAME = null;
    public static final String INITIAL_CONNECTIONS_KEY = "initialConnections";
    public static final int DEFAULT_INITIAL_CONNECTIONS = 5;
    public static final String MAX_CONNECTIONS_KEY = "maxConnections";
    public static final int DEFAULT_MAX_CONNECTIONS = 20;

    private static final Logger log = LoggerFactory.getLogger(PgPoolingDataSourceAdapter.class);

    private final SqlCache sqlCache = new SqlCache();
    private final ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder;
    private final RowResultSetHandlerBuilder rowResultSetHandlerBuilder;

    private final ExceptionConverter exceptionConverter;
    private final TypeConverterStore converterStore;

    private final PGPoolingDataSource ds;
    private final int transactionIsolationLevel;
    private final boolean autoCommit;
    private final boolean readOnly;
    private final int holdability;

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
            conn.setHoldability(holdability);
            // conn.setNetworkTimeout(null, 0); // Not implemented by PostgreSQL
            conn.setReadOnly(readOnly);
            // conn.setSchema("String schema");  // Not implemented by PostgreSQL
            // conn.setTypeMap(null);  // Map<String, Class<?>> TODO

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

    public static PgPoolingDataSourceAdapter.Builder configure() {
        return new PgPoolingDataSourceAdapter.Builder();
    }

    public static PgPoolingDataSourceAdapter buildFromDefaultConfFile() {
        return buildFromConfFile(DEFAULT_CONF_FILE);
    }

    public static PgPoolingDataSourceAdapter buildFromConfFile(String path) {
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

        Builder builder = new PgPoolingDataSourceAdapter.Builder();

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

        public PgPoolingDataSourceAdapter done() {
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
            return new PgPoolingDataSourceAdapter(this);
        }
    }

    private PgPoolingDataSourceAdapter() {
        exceptionConverter = null;
        converterStore = null;
        ds = null;
        transactionIsolationLevel = -1;
        scalarResultSetHandlerBuilder = null;
        rowResultSetHandlerBuilder = null;
        autoCommit = false;
        readOnly = false;
        holdability = -1;
    }

    // NOTE that https://jdbc.postgresql.org/documentation/94/connect.html#connection-parameters shows that
    // you can pass more parameters to Postgres' JDBC driver using DriverManager to get connections
    // instead of using DataSource to get connections. However, using DriverManager to get connections
    // precludes the use of connection pooling!
    // TODO: explain this in docs. For instance, kerberosServerName can only be set using DriverManager.getConnection,
    // not using DataSource.getConnection(). Lame.

    private PgPoolingDataSourceAdapter(Builder builder) {
        ds = new PGPoolingDataSource();
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
        ds.setTcpKeepAlive(builder.tcpKeepAlive);
        ds.setUnknownLength(builder.unknownLength);

        log.info("Application Name: {}", builder.appName);
        transactionIsolationLevel = builder.transactionIsolationLevel;
        exceptionConverter = builder.exceptionConverter;
        converterStore = new TypeConverterStore(builder.typeConverterConfFiles);
        scalarResultSetHandlerBuilder = builder.scalarResultSetHandlerBuilder;
        rowResultSetHandlerBuilder = builder.rowResultSetHandlerBuilder;
        autoCommit = builder.autoCommit;
        readOnly = builder.readOnly;
        holdability = builder.holdability;

        ds.setInitialConnections(builder.initialConnections);
        ds.setMaxConnections(builder.maxConnections);
        ds.setDataSourceName(builder.dataSourceName);

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
