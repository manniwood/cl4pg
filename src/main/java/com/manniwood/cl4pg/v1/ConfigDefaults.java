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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Configuration defaults for Cl4Pg. Note that many of them are taken from org.postgresql.ds.common.BaseDataSource. The
 * textual names try to match the exact property names that are used in the PostgreSQL driver; so, even though
 * most of the names follow the example of camel case with the first character in lowercase, oddly-named
 * property names such as AutoCommit or ApplicationName have been preserved.
 *
 * @author mwood
 *
 */
public final class ConfigDefaults {

    public static final String PROJ_NAME = "cl4pg";
    public static final String BUILTIN_TYPE_CONVERTERS_CONF_FILE = PROJ_NAME + "/BuiltinTypeConverters.properties";

    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_DATABASE = "postgres";
    public static final String DEFAULT_USERNAME = "postgres";
    public static final String DEFAULT_PASSWORD = "postgres";
    public static final String DEFAULT_APP_NAME = "cl4pg";
    public static final String DEFAULT_EXCEPTION_CONVERTER_CLASS = "com.manniwood.cl4pg.v1.exceptionconverters.DefaultExceptionConverter";
    public static final int DEFAULT_TRANSACTION_ISOLATION_LEVEL = Connection.TRANSACTION_READ_COMMITTED;
    public static final String HOSTNAME_KEY = "hostname";
    public static final String PORT_KEY = "port";
    public static final String DATABASE_KEY = "database";
    public static final String USERNAME_KEY = "user";
    public static final String PASSWORD_KEY = "password";
    // ugly, but matches name in Pg Driver
    public static final String APP_NAME_KEY = "ApplicationName";
    public static final String EXCEPTION_CONVERTER_KEY = "ExceptionConverter";
    public static final String TRANSACTION_ISOLATION_LEVEL_KEY = "TransactionIsolationLevel";
    public static final String TYPE_CONVERTER_CONF_FILES_KEY = "TypeConverterConfFiles";
    public static final String DEFAULT_SCALAR_RESULT_SET_HANDLER_BUILDER = "com.manniwood.cl4pg.v1.resultsethandlers.GuessScalarResultSetHandlerBuilder";
    public static final String DEFAULT_ROW_RESULT_SET_HANDLER_BUILDER = "com.manniwood.cl4pg.v1.resultsethandlers.GuessConstructorResultSetHandlerBuilder";
    public static final String SCALAR_RESULT_SET_HANDLER_BUILDER_KEY = "ScalarResultSetHandlerBuilder";
    public static final String ROW_RESULT_SET_HANDLER_BUILDER_KEY = "RowResultSetHandlerBuilder";
    public static final String AUTO_COMMIT_KEY = "AutoCommit";
    public static final boolean DEFAULT_AUTO_COMMIT = false;
    public static final String BINARY_TRANSFER_KEY = "binaryTransfer";
    public static final boolean DEFAULT_BINARY_TRANSFER = false;
    public static final String BINARY_TRANSFER_ENABLE_KEY = "binaryTransferEnable";
    public static final String DEFAULT_BINARY_TRANSFER_ENABLE = null;
    public static final String BINARY_TRANSFER_DISABLE_KEY = "binaryTransferDisable";
    public static final String DEFAULT_BINARY_TRANSFER_DISABLE = null;
    public static final String COMPATIBLE_KEY = "compatible";
    public static final String DEFAULT_COMPATIBLE = null;
    public static final String DISABLE_COLUMN_SANITIZER_KEY = "disableColumnSanitizer";
    public static final boolean DEFAULT_DISABLE_COLUMN_SANITIZER = false;
    public static final String LOGIN_TIMEOUT_KEY = "loginTimeout";
    public static final int DEFAULT_LOGIN_TIMEOUT = 0;
    public static final String LOG_LEVEL_KEY = "logLevel";
    public static final int DEFAULT_LOG_LEVEL = 0;
    public static final String LOG_WRITER_KEY = "logWriter";
    public static final PrintWriter DEFAULT_LOG_WRITER = null;
    public static final String PREPARE_THRESHOLD_KEY = "prepareThreshold";
    public static final int DEFAULT_PREPARE_THRESHOLD = 5;
    public static final String PROTOCOL_VERSION_KEY = "protocolVersion";
    public static final int DEFAULT_PROTOCOL_VERSION = 0;
    public static final String RECEIVE_BUFFER_SIZE_KEY = "receiveBufferSize";
    public static final int DEFAULT_RECEIVE_BUFFER_SIZE = -1;
    public static final String SEND_BUFFER_SIZE_KEY = "sendBufferSize";
    public static final int DEFAULT_SEND_BUFFER_SIZE = -1;
    public static final String STRING_TYPE_KEY = "stringtype";
    public static final String DEFAULT_STRING_TYPE = null;
    public static final String SSL_KEY = "ssl";
    public static final boolean DEFAULT_SSL = false;
    public static final String SSL_FACTORY_KEY = "sslfactory";
    public static final String DEFAULT_SSL_FACTORY = null;
    public static final String SOCKET_TIMEOUT_KEY = "socketTimeout";
    public static final int DEFAULT_SOCKET_TIMEOOUT = 0;
    public static final String TCP_KEEP_ALIVE_KEY = "tcpKeepAlive";
    public static final boolean DEFAULT_TCP_KEEP_ALIVE = false;
    public static final String UNKNOWN_LENGTH_KEY = "unknownLength";
    public static final int DEFAULT_UNKNOWN_LENGTH = Integer.MAX_VALUE;
    public static final String READ_ONLY_KEY = "readOnly";
    public static final boolean DEFAULT_READ_ONLY = false;
    public static final String HOLDABILITY_KEY = "holdability";
    public static final int DEFAULT_HOLDABILITY = ResultSet.CLOSE_CURSORS_AT_COMMIT;

    private ConfigDefaults() {
        // Utility class
    }

}
