package com.manniwood.mpjw.converters;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.util.ColumnLabelConverter;

public class ConverterStore {

    private final static Logger log = LoggerFactory.getLogger(ConverterStore.class);

    private Map<Class<?>, Converter<?>> converters;

    public ConverterStore() {
        converters = new HashMap<>();
        converters.put(int.class, new IntConverter());
        converters.put(Integer.class, new IntConverter());
        converters.put(String.class, new StringConverter());
        converters.put(UUID.class, new UUIDConverter());
    }

    public static Map<Class<?>, Class<?>> wrappersToPrimitives = new HashMap<>();

    static {
        wrappersToPrimitives.put(Byte.class, byte.class);
        wrappersToPrimitives.put(Boolean.class, boolean.class);
        wrappersToPrimitives.put(Short.class, short.class);
        wrappersToPrimitives.put(Integer.class, int.class);
        wrappersToPrimitives.put(Long.class, long.class);
        wrappersToPrimitives.put(Float.class, float.class);
        wrappersToPrimitives.put(Double.class, double.class);
        wrappersToPrimitives.put(Character.class, char.class);
    }

    public static Map<Class<?>, Class<?>> primitivesToWrappers = new HashMap<>();

    static {
        primitivesToWrappers.put(byte.class, Byte.class);
        primitivesToWrappers.put(boolean.class, Boolean.class);
        primitivesToWrappers.put(short.class, Short.class);
        primitivesToWrappers.put(int.class, Integer.class);
        primitivesToWrappers.put(long.class, Long.class);
        primitivesToWrappers.put(float.class, Float.class);
        primitivesToWrappers.put(double.class, Double.class);
        primitivesToWrappers.put(char.class, Character.class);
    }

    public static Map<String, Class<?>> primitiveNamesToClasses = new HashMap<>();

    static {
        primitiveNamesToClasses.put("byte", byte.class);
        primitiveNamesToClasses.put("boolean", boolean.class);
        primitiveNamesToClasses.put("short", short.class);
        primitiveNamesToClasses.put("int", int.class);
        primitiveNamesToClasses.put("long", long.class);
        primitiveNamesToClasses.put("float", float.class);
        primitiveNamesToClasses.put("double", double.class);
        primitiveNamesToClasses.put("char", char.class);
    }

    @SuppressWarnings("unchecked")
    public <T> void setItems(PreparedStatement pstmt, T t, List<String> getters) throws SQLException {
        int i = 0;
        Class<?> tclass = t.getClass();
        try {
            for (String getter : getters) {
                i++;
                Method m = tclass.getMethod(getter);
                Object o = m.invoke(t);
                Class<?> returnClass = m.getReturnType();
                Converter<T> converter = (Converter<T>)converters.get(returnClass);
                converter.setItem(pstmt, i, (T)o);
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException(e);
        }
    }

    // XXX: This is doing more than guessing the setters; it's invoking them, too; we need to de-couple that
    public <T> void guessSetters(ResultSet rs, Class<T> returnType) throws SQLException {
        // XXX: lots of opportunity for speedups here;
        // don't want to look up the methods for any result
        // set larger than 1; cache methods found.
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnClassName(i);
                String label = md.getColumnLabel(i);
                Class<?> parameterType = Class.forName(className);
                String setterName = ColumnLabelConverter.convert(label);
                Method m = findMethod(returnType, parameterType, setterName);
                @SuppressWarnings("unchecked")
                Converter<?> converter = converters.get(parameterType);
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
            throw new MPJWException(e);
        }
    }

    // XXX: This is doing more than guessing the setters; it's invoking them, too; we need to de-couple that
    public <T> T guessSettersAndInvoke(ResultSet rs, Class<T> returnType) throws SQLException {
        T t = null;
        // XXX: lots of opportunity for speedups here;
        // don't want to look up the methods for any result
        // set larger than 1; cache methods found.
        try {
            t = returnType.newInstance();
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnClassName(i);
                String label = md.getColumnLabel(i);
                Class<?> parameterType = Class.forName(className);
                String setterName = ColumnLabelConverter.convert(label);
                Method m = findMethod(returnType, parameterType, setterName);
                Converter<?> converter = converters.get(parameterType);
                m.invoke(t, converter.getItem(rs, i));
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException(e);
        }
        return t;
    }

    public <T> T specifySetters(ResultSet rs, Class<T> returnType) throws SQLException {
        T t = null;
        // XXX: lots of opportunity for speedups here;
        // don't want to look up the methods for any result
        // set larger than 1; cache methods found.
        try {
            t = returnType.newInstance();
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnClassName(i);
                String setterName = md.getColumnLabel(i);
                Class<?> parameterType = Class.forName(className);
                Method m = findMethod(returnType, parameterType, setterName);
                Converter<?> converter = converters.get(parameterType);
                m.invoke(t, converter.getItem(rs, i));
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException(e);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> T guessConstructor(ResultSet rs, Class<T> returnType) throws SQLException {
        T t = null;
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            Class<?>[] parameterTypes = new Class[numCols];
            Object[] params = new Object[numCols];
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnClassName(i);
                // String label = md.getColumnLabel(i);
                Class<?> parameterType = Class.forName(className);
                parameterTypes[i - 1] = parameterType;
                Converter<?> converter = converters.get(parameterType);
                params[i - 1] = converter.getItem(rs, i);
                log.debug("param {} == {}", i - 1, params[i - 1]);
            }
            Constructor<?> constructor = returnType.getDeclaredConstructor(parameterTypes);
            t = (T)constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException(e);
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public <T> T specifyConstructorArgs(ResultSet rs, Class<T> returnType) throws SQLException {
        T t = null;
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            Class<?>[] parameterTypes = new Class[numCols];
            Object[] params = new Object[numCols];
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                // String className = md.getColumnClassName(i);
                String label = md.getColumnLabel(i);
                Class<?> parameterType = className2Class(label);
                parameterTypes[i - 1] = parameterType;
                Converter<?> converter = converters.get(parameterType);
                params[i - 1] = converter.getItem(rs, i);
                log.debug("param {} == {}", i - 1, params[i - 1]);
            }
            Constructor<?> constructor = returnType.getDeclaredConstructor(parameterTypes);
            t = (T)constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException(e);
        }
        return t;
    }

    public static Class<?> className2Class(String className) throws ClassNotFoundException {
        Class<?> c = primitiveNamesToClasses.get(className);
        if (c != null) {
            return c;
        }
        return Class.forName(className);
    }

    private <T> Method findMethod(Class<T> returnType, Class<?> parameterType, String setterName) throws NoSuchMethodException {
        Method m;
        try {
            // Usually, this will succeed, but sometimes we are looking
            // for method setFoo(Integer) and really the method
            // is setFoo(int), so it's possible to throw NoSuchMethodException
            // here.
            m = returnType.getMethod(setterName, parameterType);
        } catch (NoSuchMethodException e) {
            // Just in case we got here looking for setFoo(Integer),
            // when really we need setFoo(int), try find setFoo(int)
            Class<?> primitiveType = wrappersToPrimitives.get(parameterType);
            if (primitiveType == null) {
                // Nope. There is no primitive type. The method
                // really isn't there.
                throw e;
            }
            // This can also throw NoSuchMethodException
            // if the primitive method really isn't there.
            m = returnType.getMethod(setterName, primitiveType);
        }
        return m;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setBare(PreparedStatement pstmt, int i, Object param, String className) throws SQLException {
        Class<?> parameterType = null;
        try {
            parameterType = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new MPJWException(e);
        }
        Converter converter = converters.get(parameterType);
        converter.setItem(pstmt, i, parameterType.cast(param));
    }

}
