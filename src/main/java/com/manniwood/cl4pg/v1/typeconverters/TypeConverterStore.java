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
package com.manniwood.cl4pg.v1.typeconverters;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.v1.ConfigDefaults;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfFileException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgReflectionException;
import com.manniwood.cl4pg.v1.sqlparsers.InOutArg;
import com.manniwood.cl4pg.v1.typeconverters.types.TypeConverter;
import com.manniwood.cl4pg.v1.util.ColumnLabelConverter;
import com.manniwood.cl4pg.v1.util.ResourceUtil;
import com.manniwood.cl4pg.v1.util.Str;

/**
 * A store of TypeConverters, as well as mappings to and from String names of
 * those converters, as well as utility classes for mapping ResultSet rows to
 * beans. This class needs to be thread safe!
 *
 * @author mwood
 *
 */
public class TypeConverterStore {

    private final static Logger log = LoggerFactory.getLogger(TypeConverterStore.class);

    private final Map<Class<?>, TypeConverter<?>> typeConverters;

    public TypeConverterStore(String typeConverterConfFiles) {
        // The builtin type typeConverters conf file is either the only
        // type typeConverters file in the list, or the first typeConverters
        // file
        // in the list.
        String allConfFiles;
        if (Str.isNullOrEmpty(typeConverterConfFiles)) {
            allConfFiles = ConfigDefaults.BUILTIN_TYPE_CONVERTERS_CONF_FILE;
        } else {
            allConfFiles = ConfigDefaults.BUILTIN_TYPE_CONVERTERS_CONF_FILE + "," + typeConverterConfFiles;
        }
        String[] fileNames = allConfFiles.split(",");

        typeConverters = new HashMap<>();

        for (String fileName : fileNames) {
            Properties props = loadPropsFromPath(fileName);

            for (String className : props.stringPropertyNames()) {
                Class<?> clazz = className2ClassOrThrow(className);
                Class<?> converterClass = className2ClassOrThrow(props.getProperty(className));
                TypeConverter<?> converter = (TypeConverter<?>) instantiateOrThrow(converterClass);
                typeConverters.put(clazz, converter);
            }
        }
    }

    /**
     * Loads the TypeConverterStore's properties from a properties file in the
     * classpath.
     *
     * @param path
     * @return
     */
    private Properties loadPropsFromPath(String path) {
        Properties props;
        props = new Properties();
        InputStream inStream = ResourceUtil.class.getClassLoader().getResourceAsStream(path);
        if (inStream == null) {
            throw new Cl4pgConfFileException("Could not find conf file \"" + path + "\"");
        }
        try {
            props.load(inStream);
        } catch (IOException e) {
            throw new Cl4pgConfFileException("Could not read conf file \"" + path + "\"", e);
        }
        return props;
    }

    /**
     * Maps primitive Java object wrappers to their primitive types.
     */
    private static Map<Class<?>, Class<?>> wrappersToPrimitives = new HashMap<>();

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

    /**
     * Maps Java primitive types to their wrapper classes.
     */
    private static Map<Class<?>, Class<?>> primitivesToWrappers = new HashMap<>();

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

    /**
     * Maps String names of Java primitives to their primitive types.
     */
    private static Map<String, Class<?>> primitiveNamesToClasses = new HashMap<>();

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

    /**
     * Calls setSQLArguments with the default startCol of 1.
     *
     * @param pstmt
     * @param p
     * @param getters
     * @throws SQLException
     */
    public <P> void setSQLArguments(PreparedStatement pstmt,
                                    P p,
                                    List<String> getters) throws SQLException {
        setSQLArguments(pstmt, p, getters, 1);
    }

    /**
     * In a PreparedStatement, such as "select foo from foos where foo = ?",
     * sets all question mark arguments using the appropriate PreparedStatement
     * methods, such as setInt(), setString(), setObject(), etc. The source data
     * for these setter arguments comes from the bean p, whose getters are
     * called in the order listed in the getters argument. PreparedStatement
     * question mark arguments are set starting at offset 1, but startCol allows
     * you to start at any other offset, if required.
     *
     * @param pstmt
     * @param p
     * @param getters
     * @param startCol
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public <P> void setSQLArguments(PreparedStatement pstmt,
                                    P p,
                                    List<String> getters,
                                    int startCol) throws SQLException {
        int i = startCol;
        Class<?> tclass = p.getClass();
        try {
            for (String getter : getters) {
                Method m = tclass.getMethod(getter);
                Object o = m.invoke(p);
                Class<?> returnClass = m.getReturnType();
                TypeConverter<P> converter = (TypeConverter<P>) typeConverters.get(returnClass);
                converter.setItem(pstmt, i, (P) o);
                i++;
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new Cl4pgReflectionException(e);
        }
    }

    /**
     * In a CallableStatement, sets all question mark arguments using the
     * appropriate CallableStatement methods, such as setInt(), setString(),
     * registerOutParameter(), etc. The source data for the CallableStatement
     * set arguments comes from the bean p, whose getters are called in the
     * order listed in the args argument. Additionally, the bean p's setters
     * will later get called based on any argument in args listed as an OUT
     * parameter.
     *
     * @param cstmt
     * @param p
     * @param args
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public <P> void setSQLArguments(CallableStatement cstmt,
                                    P p,
                                    List<InOutArg> args) throws SQLException {
        int i = 0;
        Class<?> tclass = p.getClass();
        try {
            for (InOutArg arg : args) {
                i++;
                String getter = arg.getGetter();
                if (getter != null && !getter.isEmpty()) {
                    Method m = tclass.getMethod(getter);
                    Object o = m.invoke(p);
                    Class<?> returnClass = m.getReturnType();
                    TypeConverter<P> converter = (TypeConverter<P>) typeConverters.get(returnClass);
                    converter.setItem(cstmt, i, (P) o);
                }
                String setter = arg.getSetter();
                if (setter != null && !setter.isEmpty()) {
                    Method setMethod = null;
                    for (Method m : tclass.getMethods()) {
                        if (m.getName().equals(setter)) {
                            setMethod = m;
                        }
                    }
                    Class<?>[] paramTypes = setMethod.getParameterTypes();
                    Class<?> setterClass = paramTypes[0];
                    TypeConverter<P> converter = (TypeConverter<P>) typeConverters.get(setterClass);
                    log.debug("setter converter {} for offset {}", converter, i);
                    converter.registerOutParameter(cstmt, i);
                }
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new Cl4pgReflectionException(e);
        }
    }

    /**
     * Sets the parameter (param) on prepared statement (pstmt) at offset i,
     * using className to determine which TypeConverter should be used to
     * actually set the parameter (e.g., so that PreparedStatement's setInt() or
     * setString() or setObject() method will get used).
     *
     * @param pstmt
     * @param i
     * @param param
     * @param className
     * @throws SQLException
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setSQLArgument(PreparedStatement pstmt,
                               int i,
                               Object param,
                               String className) throws SQLException {
        Class<?> parameterType = null;
        try {
            parameterType = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new Cl4pgReflectionException(e);
        }
        TypeConverter typeConverter = typeConverters.get(parameterType);
        typeConverter.setItem(pstmt, i, parameterType.cast(param));
    }

    /**
     * Uses metadata from a ResultSet (rs) to guess what the setters on a bean
     * of type Class (returnType) will be. So for a ResultSet whose columns are
     * named user_name and employee_id, this method will guess the bean's
     * corresponding setter methods to be named setUserName and setEmployeeId.
     * Furthermore, the class name from each column
     * (ResultSetMetaData.getColumnClassName()) will be used to determine the
     * TypeConverter that should be used to translate the SQL type for each
     * column to the type required by each corresponding bean setter method.
     *
     * @param rs
     * @param returnType
     * @return
     * @throws SQLException
     */
    public <T> List<SetterAndTypeConverter> guessSetters(ResultSet rs,
                                                     Class<T> returnType) throws SQLException {
        List<SetterAndTypeConverter> settersAndConverters = new ArrayList<>();
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnClassName(i);
                String label = md.getColumnLabel(i);
                Class<?> parameterType = Class.forName(className);
                String setterName = ColumnLabelConverter.convert(label);
                Method setter = findSetterMethod(returnType, parameterType, setterName);
                TypeConverter<?> converter = typeConverters.get(parameterType);
                settersAndConverters.add(new SetterAndTypeConverter(converter, setter));
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
            throw new Cl4pgReflectionException(e);
        }
        return settersAndConverters;
    }

    /**
     * Uses column labels from a ResultSet (rs) as names of the setters on a
     * bean of type Class (returnType). So for a ResultSet whose column labels
     * are "setUserName" and "setEmployeeId", this method will use the bean's
     * corresponding setter methods to be named setUserName and setEmployeeId.
     * Furthermore, the class name from each column
     * (ResultSetMetaData.getColumnClassName()) will be used to determine the
     * TypeConverter that should be used to translate the SQL type for each
     * column to the type required by each corresponding bean setter method.
     *
     * @param rs
     * @param returnType
     * @return
     * @throws SQLException
     */
    public <T> List<SetterAndTypeConverter> specifySetters(ResultSet rs,
                                                       Class<T> returnType) throws SQLException {
        List<SetterAndTypeConverter> settersAndConverters = new ArrayList<>();
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnClassName(i);
                String setterName = md.getColumnLabel(i);
                Class<?> parameterType = Class.forName(className);
                Method setter = findSetterMethod(returnType, parameterType, setterName);
                TypeConverter<?> converter = typeConverters.get(parameterType);
                settersAndConverters.add(new SetterAndTypeConverter(converter, setter));
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
            throw new Cl4pgReflectionException(e);
        }
        return settersAndConverters;
    }

    /**
     * Uses argument args to determine the names of setters on a bean of type
     * Class (returnType).
     *
     * <p>
     * Also, because this method deals with CallableStatements, it also
     * determines the column number of each setter argument (corresponding to an
     * OUT argument in the CallableStatement) as well as each setter argument's
     * absolute column number. For instance, for a stored procedure with one IN
     * param and two OUT params, we will want to find the corresponding setter
     * methods for the two OUT params, and the correct columns to get the
     * corresponding data from.
     *
     * <p>
     * Furthermore, the class name from each column
     * (ResultSetMetaData.getColumnClassName()) will be used to determine the
     * TypeConverter that should be used to translate the SQL type for each
     * column to the type required by each corresponding bean setter method.
     *
     * @param rs
     * @param returnType
     * @return
     * @throws SQLException
     */
    public <T> List<SetterAndTypeConverterAndColNum> specifySetters(CallableStatement cstmt,
                                                                Class<T> returnType,
                                                                List<InOutArg> args) throws SQLException {
        List<SetterAndTypeConverterAndColNum> settersAndConverters = new ArrayList<>();
        try {
            int absCol = 0;
            int setCol = 1;
            ResultSetMetaData md = cstmt.getMetaData();
            for (InOutArg arg : args) {
                absCol++;
                String setterName = arg.getSetter();
                if (Str.isNullOrEmpty(setterName)) {
                    // setterName is null or empty, so do not increment i.
                    // There are *only* as many return columns as there
                    // are OUT params, *not* as many return columns
                    // as there are all types (IN/OUT/INOUT) of params.
                    continue;
                }
                String className = md.getColumnClassName(setCol);
                Class<?> parameterType = Class.forName(className);
                Method setter = findSetterMethod(returnType, parameterType, setterName);
                TypeConverter<?> converter = typeConverters.get(parameterType);
                settersAndConverters.add(new SetterAndTypeConverterAndColNum(converter, setter, absCol, setCol));
                // only increment for non-null setters
                setCol++;
            }
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
            throw new Cl4pgReflectionException(e);
        }
        return settersAndConverters;
    }

    /**
     * Uses the class name (ResultSetMetaData.getColumnClassName()) from the
     * only column of a single-column result set to determine the TypeConverter
     * that should be used to translate the SQL type into a Java type.
     *
     * @param rs
     * @param returnType
     * @return
     * @throws SQLException
     */
    public <T> TypeConverter<?> guessConverter(ResultSet rs) throws SQLException {
        TypeConverter<?> converter = null;
        Class<?> parameterType = null;
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            if (numCols > 1) {
                throw new Cl4pgReflectionException("Only one column is allowed to be in the result set.");
            }
            String className = md.getColumnClassName(1);
            parameterType = Class.forName(className);
            converter = typeConverters.get(parameterType);
        } catch (ClassNotFoundException | SecurityException | IllegalArgumentException e) {
            throw new Cl4pgReflectionException(e);
        }
        if (converter == null) {
            throw new IllegalArgumentException("TypeConverter not found for column return type " + parameterType);
        }
        return converter;
    }

    /**
     * Uses returnType to determine the TypeConverter that should be used to
     * translate the SQL type of a single-column result set into a Java type.
     *
     * @param rs
     * @param returnType
     * @return
     * @throws SQLException
     */
    public <T> TypeConverter<?> specifyConverter(ResultSet rs,
                                                 Class<T> returnType) throws SQLException {
        TypeConverter<?> converter = null;
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            if (numCols > 1) {
                throw new Cl4pgReflectionException("Only one column is allowed to be in the result set.");
            }
            String className = md.getColumnLabel(1);
            Class<?> parameterType = className2Class(className);
            converter = typeConverters.get(parameterType);
        } catch (ClassNotFoundException | SecurityException | IllegalArgumentException e) {
            throw new Cl4pgReflectionException(e);
        }
        return converter;
    }

    /**
     * Uses the next row in ResultSet rs to build a bean of type returnType
     * using the setter methods and their corresponding TypeConverters listed in
     * settersAndConverters. First, the bean's null constructor is called, and
     * then the setter method corresponding to each column in the result set
     * gets called, to populate the bean.
     *
     * @param rs
     * @param returnType
     * @param settersAndConverters
     * @return
     * @throws SQLException
     */
    public <T> T buildBeanUsingSetters(ResultSet rs,
                                       Class<T> returnType,
                                       List<SetterAndTypeConverter> settersAndConverters) throws SQLException {
        T t = null;
        try {
            t = returnType.newInstance();
            int col = 1; // JDBC cols start at 1, not zero
            for (SetterAndTypeConverter sac : settersAndConverters) {
                sac.getSetter().invoke(t, sac.getConverter().getItem(rs, col));
                col++;
            }
        } catch (InstantiationException | IllegalAccessException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new Cl4pgReflectionException(e);
        }
        return t;
    }

    /**
     * Uses CallableStatement cstmt and a list of setters and converters
     * (settersAndConverters) to populate bean t.
     *
     * @param rs
     * @param returnType
     * @param settersAndConverters
     * @return
     * @throws SQLException
     */
    public <T> T populateBeanUsingSetters(CallableStatement cstmt,
                                          T t,
                                          List<SetterAndTypeConverterAndColNum> settersAndConverters) throws SQLException {
        try {
            for (SetterAndTypeConverterAndColNum sac : settersAndConverters) {
                log.debug("Calling setter {} for column {}", sac.getConverter(), sac.getColNum());
                sac.getSetter().invoke(t, sac.getConverter().getItem(cstmt, sac.getColNum()));
            }
        } catch (IllegalAccessException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new Cl4pgReflectionException(e);
        }
        return t;
    }

    /**
     * Uses metadata from a ResultSet (rs) to guess the constructor arguments on
     * a bean of type Class (returnType). So for a bean of type User, and a
     * ResultSet whose columns are user_name (java.lang.String) and employee_id
     * (java.lang.Integer), this method will guess the bean's corresponding
     * constructor to be User(String, Integer).
     *
     * <p>
     * Furthermore, the class name from each column
     * (ResultSetMetaData.getColumnClassName()) will be used to determine the
     * TypeConverter that should be used to translate the SQL type for each
     * column to the type required by each corresponding argument of the bean's
     * constructor.
     *
     * @param rs
     * @param returnType
     * @return
     * @throws SQLException
     */
    public <T> ConstructorAndTypeConverters guessConstructor(ResultSet rs,
                                                         Class<T> returnType) throws SQLException {
        Constructor<?> constructor = null;
        List<TypeConverter<?>> convs = new ArrayList<>();
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            Class<?>[] parameterTypes = new Class[numCols];
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnClassName(i);
                Class<?> parameterType = Class.forName(className);
                parameterTypes[i - 1] = parameterType;
                TypeConverter<?> converter = typeConverters.get(parameterType);
                convs.add(converter);
            }
            constructor = returnType.getDeclaredConstructor(parameterTypes);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
            throw new Cl4pgReflectionException(e);
        }
        return new ConstructorAndTypeConverters(constructor, convs);
    }

    /**
     * Uses column labels from a ResultSet (rs) as argument types on a
     * constructor for a bean of type Class (returnType). So for a bean of type
     * User, and a ResultSet whose column labels are "java.lang.String" and
     * "java.lang.Integer", this method will use the bean's corresponding
     * constructor User(String, Integer).
     *
     * <p>
     * Furthermore, the class name from each column
     * (ResultSetMetaData.getColumnClassName()) will be used to determine the
     * TypeConverter that should be used to translate the SQL type for each
     * column to the type required by each corresponding argument of the bean's
     * constructor.
     *
     * @param rs
     * @param returnType
     * @return
     * @throws SQLException
     */
    public <T> ConstructorAndTypeConverters specifyConstructorArgs(ResultSet rs,
                                                               Class<T> returnType) throws SQLException {
        Constructor<?> constructor = null;
        List<TypeConverter<?>> convs = new ArrayList<>();
        try {
            ResultSetMetaData md = rs.getMetaData();
            int numCols = md.getColumnCount();
            Class<?>[] parameterTypes = new Class[numCols];
            for (int i = 1 /* JDBC cols start at 1 */; i <= numCols; i++) {
                String className = md.getColumnLabel(i);
                Class<?> parameterType = className2Class(className);
                parameterTypes[i - 1] = parameterType;
                TypeConverter<?> converter = typeConverters.get(parameterType);
                convs.add(converter);
            }
            constructor = returnType.getDeclaredConstructor(parameterTypes);
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException e) {
            throw new Cl4pgReflectionException(e);
        }
        return new ConstructorAndTypeConverters(constructor, convs);
    }

    /**
     * Uses the next row in ResultSet rs to build a bean of type returnType
     * using the constructor and the constructor arguments' corresponding
     * TypeConverters listed in cac.
     *
     * @param rs
     * @param returnType
     * @param cac
     * @return
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public <T> T buildBeanUsingConstructor(ResultSet rs,
                                           Class<T> returnType,
                                           ConstructorAndTypeConverters cac) throws SQLException {
        T t = null;
        try {
            Object[] params = new Object[cac.getConverters().size()];
            int col = 1; // JDBC cols start at 1
            for (TypeConverter<?> converter : cac.getConverters()) {
                params[col - 1] = converter.getItem(rs, col);
                log.debug("param {} == {}", col - 1, params[col - 1]);
                col++;
            }
            Constructor<?> constructor = cac.getConstructor();
            t = (T) constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | SecurityException | IllegalArgumentException | InvocationTargetException e) {
            throw new Cl4pgReflectionException(e);
        }
        return t;
    }

    /**
     * Converts the string name of a class into a class object.
     *
     * @param className
     * @return
     * @throws ClassNotFoundException
     */
    public static Class<?> className2Class(String className) throws ClassNotFoundException {
        Class<?> c = primitiveNamesToClasses.get(className);
        if (c != null) {
            return c;
        }
        return Class.forName(className);
    }

    /**
     * Converts the string name of a class into a class object, or, on failure,
     * throws a Cl4pgReflectionException.
     *
     * @param className
     * @return
     */
    public static Class<?> className2ClassOrThrow(String className) {
        Class<?> c = null;
        try {
            c = className2Class(className);
        } catch (ClassNotFoundException e) {
            throw new Cl4pgReflectionException(e);
        }
        return c;
    }

    /**
     * Instantiates a new instance of converterClass, or throws
     * Cl4pgReflectionException.
     *
     * @param converterClass
     * @return
     */
    private Object instantiateOrThrow(Class<?> converterClass) {
        try {
            return converterClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new Cl4pgReflectionException(e);
        }
    }

    /**
     * Finds a setter method for bean with the name setterName and the argument
     * parameterType. In the case of wrapper types like java.lang.Integer for
     * the parameterType, will also try to find the method for the corresponding
     * primitive type (such as int in this example) if a setter for the wrapper
     * type was not found.
     *
     * @param bean
     * @param parameterType
     * @param setterName
     * @return
     * @throws NoSuchMethodException
     */
    private <T> Method findSetterMethod(Class<T> bean,
                                        Class<?> parameterType,
                                        String setterName) throws NoSuchMethodException {
        Method m;
        try {
            // Usually, this will succeed, but sometimes we are looking
            // for method setFoo(Integer) and really the method
            // is setFoo(int), so it's possible to throw NoSuchMethodException
            // here.
            m = bean.getMethod(setterName, parameterType);
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
            m = bean.getMethod(setterName, primitiveType);
        }
        return m;
    }

    public Map<Class<?>, TypeConverter<?>> getConverters() {
        return typeConverters;
    }

}
