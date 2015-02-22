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
import org.postgresql.Driver;
import org.postgresql.PGConnection;
import org.postgresql.PGStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

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

    private final Properties connProps;
    private final Driver driver;
    private final String url;

    @Override
    public PgSession getSession() {
        return new PgSession(this);
    }

    @Override
    public Connection getConnection() {
        Connection conn = null;
        try {
            conn = driver.connect(url, connProps);
            // XXX: why do we have to set this here? Doesn't connect, above, use the connProps?
            conn.setAutoCommit(Boolean.parseBoolean(connProps.getProperty(ConfigDefaults.AUTO_COMMIT_KEY)));
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
            throw new Cl4pgConfFileException("Could not find conf file \"" + path + "\" in classpath");
        }
        try {
            props.load(inStream);
        } catch (IOException e) {
            throw new Cl4pgConfFileException("Could not read conf file \"" + path + "\"", e);
        }

        Builder builder = new PgSimpleDataSourceAdapter.Builder(props);

        String exceptionConverterStr = PropsUtil.getPropFromAll(props, ConfigDefaults.EXCEPTION_CONVERTER_KEY);
        if (!Str.isNullOrEmpty(exceptionConverterStr)) {
            builder.exceptionConverter(exceptionConverterStr);
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

        return builder.done();
    }

    public static class Builder {
        private String exceptionConverterStr = ConfigDefaults.DEFAULT_EXCEPTION_CONVERTER_CLASS;
        private ExceptionConverter exceptionConverter = null;
        private String typeConverterConfFiles = null;
        private String scalarResultSetHandlerBuilderStr = ConfigDefaults.DEFAULT_SCALAR_RESULT_SET_HANDLER_BUILDER;
        private ScalarResultSetHandlerBuilder scalarResultSetHandlerBuilder = null;
        private String rowResultSetHandlerBuilderStr = ConfigDefaults.DEFAULT_ROW_RESULT_SET_HANDLER_BUILDER;
        private RowResultSetHandlerBuilder rowResultSetHandlerBuilder = null;

        private final Properties props = new Properties();

        public Builder() {
            ConfigDefaults.setDefaults(props);
        }

        public Builder(Properties properties) {
            ConfigDefaults.setDefaults(props);
            props.putAll(properties);
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

        public Builder typeConverterConfFiles(String typeConverterConfFiles) {
            this.typeConverterConfFiles = typeConverterConfFiles;
            return this;
        }

        public Builder hostname(String hostname) {
            props.setProperty(ConfigDefaults.HOSTNAME_KEY, hostname);
            return this;
        }

        public Builder port(int port) {
            props.setProperty(ConfigDefaults.PORT_KEY, String.valueOf(port));
            return this;
        }

        public Builder port(String port) {
            props.setProperty(ConfigDefaults.PORT_KEY, port);
            return this;
        }

        public Builder database(String database) {
            props.setProperty(ConfigDefaults.DATABASE_KEY, database);
            return this;
        }

        public Builder username(String username) {
            props.setProperty(ConfigDefaults.USERNAME_KEY, username);
            return this;
        }

        public Builder password(String password) {
            props.setProperty(ConfigDefaults.PASSWORD_KEY, password);
            return this;
        }

        public Builder appName(String appName) {
            props.setProperty(ConfigDefaults.APP_NAME_KEY, appName);
            return this;
        }


        public Builder transactionIsolationLevel(int transactionIsolationLevel) {
            props.setProperty(ConfigDefaults.TRANSACTION_ISOLATION_LEVEL_KEY, String.valueOf(transactionIsolationLevel));
            return this;
        }

        public Builder transactionIsolationLevel(String transactionIsolationLevel) {
            props.setProperty(ConfigDefaults.TRANSACTION_ISOLATION_LEVEL_KEY, transactionIsolationLevel);
            return this;
        }

        public Builder transactionIsolationLevelName(String name) {
            props.setProperty(ConfigDefaults.TRANSACTION_ISOLATION_LEVEL_KEY, String.valueOf(TransactionIsolationLevelConverter.convert(name)));
            return this;
        }


        public Builder autoCommit(boolean autoCommit) {
            props.setProperty(ConfigDefaults.AUTO_COMMIT_KEY, String.valueOf(autoCommit));
            return this;
        }

        public Builder autoCommit(String autoCommit) {
            props.setProperty(ConfigDefaults.AUTO_COMMIT_KEY, autoCommit);
            return this;
        }

        public Builder binaryTransfer(boolean binaryTransfer) {
            props.setProperty(ConfigDefaults.BINARY_TRANSFER_KEY, String.valueOf(binaryTransfer));
            return this;
        }

        public Builder binaryTransfer(String binaryTransfer) {
            props.setProperty(ConfigDefaults.BINARY_TRANSFER_KEY, binaryTransfer);
            return this;
        }

        public Builder binaryTransferEnable(String binaryTransferEnable) {
            props.setProperty(ConfigDefaults.BINARY_TRANSFER_ENABLE_KEY, binaryTransferEnable);
            return this;
        }

        public Builder binaryTransferDisable(String binaryTransferDisable) {
            props.setProperty(ConfigDefaults.BINARY_TRANSFER_DISABLE_KEY, binaryTransferDisable);
            return this;
        }

        public Builder compatible(String compatible) {
            props.setProperty(ConfigDefaults.COMPATIBLE_KEY, compatible);
            return this;
        }

        public Builder disableColumnSanitizer(boolean disableColumnSanitizer) {
            props.setProperty(ConfigDefaults.DISABLE_COLUMN_SANITIZER_KEY, String.valueOf(disableColumnSanitizer));
            return this;
        }

        public Builder disableColumnSanitizer(String disableColumnSanitizer) {
            props.setProperty(ConfigDefaults.DISABLE_COLUMN_SANITIZER_KEY, disableColumnSanitizer);
            return this;
        }

        public Builder loginTimeout(int loginTimeout) {
            props.setProperty(ConfigDefaults.LOGIN_TIMEOUT_KEY, String.valueOf(loginTimeout));
            return this;
        }

        public Builder loginTimeout(String loginTimeout) {
            props.setProperty(ConfigDefaults.LOGIN_TIMEOUT_KEY, loginTimeout);
            return this;
        }

        public Builder connectTimeout(int connectTimeout) {
            props.setProperty(ConfigDefaults.CONNECT_TIMEOUT_KEY, String.valueOf(connectTimeout));
            return this;
        }

        public Builder connectTimeout(String connectTimeout) {
            props.setProperty(ConfigDefaults.CONNECT_TIMEOUT_KEY, connectTimeout);
            return this;
        }

        public Builder logLevel(int logLevel) {
            props.setProperty(ConfigDefaults.LOG_LEVEL_KEY, String.valueOf(logLevel));
            return this;
        }

        public Builder logLevel(String logLevel) {
            props.setProperty(ConfigDefaults.LOG_LEVEL_KEY, logLevel);
            return this;
        }

        public Builder prepareThreshold(int prepareThreshold) {
            props.setProperty(ConfigDefaults.PREPARE_THRESHOLD_KEY, String.valueOf(prepareThreshold));
            return this;
        }

        public Builder prepareThreshold(String prepareThreshold) {
            props.setProperty(ConfigDefaults.PREPARE_THRESHOLD_KEY, prepareThreshold);
            return this;
        }

        public Builder protocolVersion(int protocolVersion) {
            props.setProperty(ConfigDefaults.PROTOCOL_VERSION_KEY, String.valueOf(protocolVersion));
            return this;
        }

        public Builder protocolVersion(String protocolVersion) {
            props.setProperty(ConfigDefaults.PROTOCOL_VERSION_KEY, protocolVersion);
            return this;
        }

        public Builder receiveBufferSize(int receiveBufferSize) {
            props.setProperty(ConfigDefaults.RECEIVE_BUFFER_SIZE_KEY, String.valueOf(receiveBufferSize));
            return this;
        }

        public Builder receiveBufferSize(String receiveBufferSize) {
            props.setProperty(ConfigDefaults.RECEIVE_BUFFER_SIZE_KEY, receiveBufferSize);
            return this;
        }

        public Builder sendBufferSize(int sendBufferSize) {
            props.setProperty(ConfigDefaults.SEND_BUFFER_SIZE_KEY, String.valueOf(sendBufferSize));
            return this;
        }

        public Builder sendBufferSize(String sendBufferSize) {
            props.setProperty(ConfigDefaults.SEND_BUFFER_SIZE_KEY, sendBufferSize);
            return this;
        }

        public Builder stringType(String stringType) {
            props.setProperty(ConfigDefaults.STRING_TYPE_KEY, stringType);
            return this;
        }

        public Builder ssl(boolean ssl) {
            props.setProperty(ConfigDefaults.SSL_KEY, String.valueOf(ssl));
            return this;
        }

        public Builder ssl(String ssl) {
            props.setProperty(ConfigDefaults.SSL_KEY, ssl);
            return this;
        }

        public Builder sslFactory(String sslFactory) {
            props.setProperty(ConfigDefaults.SSL_FACTORY_KEY, sslFactory);
            return this;
        }

        public Builder sslFactoryarg(String sslFactoryarg) {
            props.setProperty(ConfigDefaults.SSL_FACTORY_ARG_KEY, sslFactoryarg);
            return this;
        }

        public Builder socketTimeout(int socketTimeout) {
            props.setProperty(ConfigDefaults.SOCKET_TIMEOUT_KEY, String.valueOf(socketTimeout));
            return this;
        }

        public Builder socketTimeout(String socketTimeout) {
            props.setProperty(ConfigDefaults.SOCKET_TIMEOUT_KEY, socketTimeout);
            return this;
        }

        public Builder tcpKeepAlive(boolean tcpKeepAlive) {
            props.setProperty(ConfigDefaults.TCP_KEEP_ALIVE_KEY, String.valueOf(tcpKeepAlive));
            return this;
        }

        public Builder tcpKeepAlive(String tcpKeepAlive) {
            props.setProperty(ConfigDefaults.TCP_KEEP_ALIVE_KEY, tcpKeepAlive);
            return this;
        }

        public Builder unknownLength(int unknownLength) {
            props.setProperty(ConfigDefaults.UNKNOWN_LENGTH_KEY, String.valueOf(unknownLength));
            return this;
        }

        public Builder unknownLength(String unknownLength) {
            props.setProperty(ConfigDefaults.UNKNOWN_LENGTH_KEY, unknownLength);
            return this;
        }

        public Builder readOnly(boolean readOnly) {
            props.setProperty(ConfigDefaults.READ_ONLY_KEY, String.valueOf(readOnly));
            return this;
        }

        public Builder readOnly(String readOnly) {
            props.setProperty(ConfigDefaults.READ_ONLY_KEY, readOnly);
            return this;
        }

        public Builder holdability(int holdability) {
            props.setProperty(ConfigDefaults.HOLDABILITY_KEY, String.valueOf(holdability));
            return this;
        }

        public Builder holdability(String holdability) {
            props.setProperty(ConfigDefaults.HOLDABILITY_KEY, holdability);
            return this;
        }

        public Builder charSet(String charSet) {
            props.setProperty(ConfigDefaults.CHAR_SET_KEY, charSet);
            return this;
        }

        public Builder allowEncodingChanges(boolean allowEncodingChanges) {
            props.setProperty(ConfigDefaults.ALLOW_ENCODING_CHANGES_KEY, String.valueOf(allowEncodingChanges));
            return this;
        }

        public Builder allowEncodingChanges(String allowEncodingChanges) {
            props.setProperty(ConfigDefaults.ALLOW_ENCODING_CHANGES_KEY, allowEncodingChanges);
            return this;
        }

        public Builder logUnclosedConnections(boolean logUnclosedConnections) {
            props.setProperty(ConfigDefaults.LOG_UNCLOSED_CONNECTIONS_KEY, String.valueOf(logUnclosedConnections));
            return this;
        }

        public Builder logUnclosedConnections(String logUnclosedConnections) {
            props.setProperty(ConfigDefaults.LOG_UNCLOSED_CONNECTIONS_KEY, logUnclosedConnections);
            return this;
        }

        public Builder kerberosServerName(String kerberosServerName) {
            props.setProperty(ConfigDefaults.KERBEROS_SERVER_NAME_KEY, kerberosServerName);
            return this;
        }

        public Builder jaasApplicationName(String jaasApplicationServerName) {
            props.setProperty(ConfigDefaults.JAAS_APPLICATION_NAME_KEY, jaasApplicationServerName);
            return this;
        }

        public Builder gsslib(String gsslib) {
            props.setProperty(ConfigDefaults.GSS_LIB_KEY, gsslib);
            return this;
        }

        public Builder sspiServiceClass(String sspiServiceClass) {
            props.setProperty(ConfigDefaults.SSPI_SERVICE_CLASS_KEY, sspiServiceClass);
            return this;
        }

        public Builder useSpnego(boolean useSpnego) {
            props.setProperty(ConfigDefaults.USE_SPNEGO_KEY, String.valueOf(useSpnego));
            return this;
        }

        public Builder useSpnego(String useSpnego) {
            props.setProperty(ConfigDefaults.USE_SPNEGO_KEY, useSpnego);
            return this;
        }

        public Builder assumeMinServerVersion(String assumeMinServerVersion) {
            props.setProperty(ConfigDefaults.ASSUME_MIN_SERVER_VERSION_KEY, assumeMinServerVersion);
            return this;
        }

        public Builder currentSchema(String currentSchema) {
            props.setProperty(ConfigDefaults.CURRENT_SCHEMA_KEY, currentSchema);
            return this;
        }

        public Builder targetServerType(String targetServerType) {
            props.setProperty(ConfigDefaults.TARGET_SERVER_TYPE_KEY, targetServerType);
            return this;
        }

        public Builder hostRecheckSeconds(int hostRecheckSeconds) {
            props.setProperty(ConfigDefaults.HOST_RECHECK_SECONDS_KEY, String.valueOf(hostRecheckSeconds));
            return this;
        }

        public Builder hostRecheckSeconds(String hostRecheckSeconds) {
            props.setProperty(ConfigDefaults.HOST_RECHECK_SECONDS_KEY, hostRecheckSeconds);
            return this;
        }

        public Builder loadBalanceHosts(boolean loadBalanceHosts) {
            props.setProperty(ConfigDefaults.LOAD_BALANCE_HOSTS_KEY, String.valueOf(loadBalanceHosts));
            return this;
        }

        public Builder loadBalanceHosts(String loadBalanceHosts) {
            props.setProperty(ConfigDefaults.LOAD_BALANCE_HOSTS_KEY, loadBalanceHosts);
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

    /**
     * Private, to enforce use of the builder or one of the other convenience factory methods.
     */
    private PgSimpleDataSourceAdapter() {
        url = null;
        connProps = null;
        exceptionConverter = null;
        converterStore = null;
        driver = null;
        scalarResultSetHandlerBuilder = null;
        rowResultSetHandlerBuilder = null;
    }

    private PgSimpleDataSourceAdapter(Builder builder) {
        driver = new Driver();
        connProps = builder.props;
        url = ConfigDefaults.URL_PREAMBLE
                + connProps.getProperty(ConfigDefaults.HOSTNAME_KEY)
                + ":"
                + connProps.getProperty(ConfigDefaults.PORT_KEY)
                + "/"
                + connProps.getProperty(ConfigDefaults.DATABASE_KEY);

        exceptionConverter = builder.exceptionConverter;
        converterStore = new TypeConverterStore(builder.typeConverterConfFiles);
        scalarResultSetHandlerBuilder = builder.scalarResultSetHandlerBuilder;
        rowResultSetHandlerBuilder = builder.rowResultSetHandlerBuilder;
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
