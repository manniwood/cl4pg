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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.commands.Commit;
import com.manniwood.mpjw.commands.DDL;
import com.manniwood.mpjw.commands.Insert;
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
        // START HERE: will have to figure out how to
        // make the Insert work; will first have to write class
        // and test cases to take any sql insert statement
        // and turn it into its #{} --> ? form, and a corresponding
        // bean and introspect its get methods
        CommandRunner.execute(new Insert(converterStore, sql, conn, t));
    }

    public <T> T selectOne(String sqlFile, Class<T> type) {
        Object obj = instantiateBean(type);

        // resultSet.getObject("name")
        String setterName = "setName";
        Object value = "Foo";
        String valueTypeStr = "java.lang.String";
        callSetter(obj, setterName, valueTypeStr, value);

        setterName = "setId";
        value = UUID.fromString("910c80af-a4fa-49fc-b6b4-62eca118fbf7");
        valueTypeStr = "java.util.UUID";
        callSetter(obj, setterName, valueTypeStr, value);

        // resultSet.getInt("employee_id")
        setterName = "setEmployeeId";
        int intVal = 42;
        callSetter(obj, setterName, intVal);

        return type.cast(obj);
    }

    public void ddl(String ddl) {
        String sql = resolveSQL(ddl);
        CommandRunner.execute(new DDL(sql, conn));
    }

    public void commit() {
        CommandRunner.execute(new Commit(conn));
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

    private <T> Object instantiateBean(Class<T> type) {
        Object obj = null;
        try {
            obj = type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new MPJWException("Could not instantiate " + type, e);
        }
        return obj;
    }

    private <T> void callSetter(Object obj, String setterName, String valueTypeStr, T value) {
        @SuppressWarnings("rawtypes")
        Class valueType = classFromString(valueTypeStr);
        Method setter = null;
        try {
            setter = obj.getClass().getMethod(setterName, valueType);
            setter.invoke(obj, valueType.cast(value));
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException("Could not call method " + setterName, e);
        }
    }

    private void callSetter(Object obj, String setterName, int value) {
        Method setter = null;
        try {
            setter = obj.getClass().getMethod(setterName, int.class);
            setter.invoke(obj, value);
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException("Could not call method " + setterName, e);
        }
    }

    @SuppressWarnings("rawtypes")
    private Class classFromString(String className) {
        Class clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new MPJWException("Could find value class " + className, e);
        }
        return clazz;
    }

}
