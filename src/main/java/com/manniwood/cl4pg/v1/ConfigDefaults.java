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

import java.sql.Connection;

public class ConfigDefaults {

    public static final String PROJ_NAME = "cl4pg";

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
    public static final String USERNAME_KEY = "user"; // ugly, but matches name
    // in Pg Driver
    public static final String PASSWORD_KEY = "password";
    public static final String APP_NAME_KEY = "ApplicationName"; // ugly, but
    // matches name
    // in Pg Driver
    public static final String EXCEPTION_CONVERTER_KEY = "ExceptionConverter";
    public static final String TRANSACTION_ISOLATION_LEVEL_KEY = "TransactionIsolationLevel";

    private ConfigDefaults() {
        // Utility class
    }

}