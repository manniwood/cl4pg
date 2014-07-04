package com.manniwood.basicproj;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PgSession {

    private final static Logger log = LoggerFactory.getLogger(PgSession.class);

    private Connection conn = null;
    private String hostname = "localhost";
    private int dbPort = 5432;
    private String dbName = "postgres";
    private String dbUser = "postgres";
    private String dbPassword = "postgres";
    private int transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;
    private String appName = "MPJW";

    public PgSession() {
        String someSql = slurpFile("sql/create_test_table.sql");
        log.info("contents of file are: {}", someSql);
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

    public <T> T selectOne(String sqlFile, Class<T> type) {
        Object obj = instantiateBean(type);

        String setterName = "setName";
        Object value = "Foo";
        String valueTypeStr = "java.lang.String";
        callSetter(obj, setterName, valueTypeStr, value);

        setterName = "setId";
        value = UUID.fromString("910c80af-a4fa-49fc-b6b4-62eca118fbf7");
        valueTypeStr = "java.util.UUID";
        callSetter(obj, setterName, valueTypeStr, value);

        // what about primitive types? // we are presumably using resultSet.getObject for everything that we can...
        // figure out how to set employee id
        // Object obj = resultSet.getObject(1);
        // Class clazz = obj.getClass();

        return type.cast(obj);
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

    private String slurpFile(String path) {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(path)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            throw new MPJWException("Could not read file " + path, e);
        }  // XXX: is there a finally here? any resource I have to clean up?
        return sb.toString();
    }
}
