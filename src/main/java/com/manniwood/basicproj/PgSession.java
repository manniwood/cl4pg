package com.manniwood.basicproj;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class PgSession {

    public PgSession() {
    }

    public <T> T selectOne(String sqlFile, Class<T> type) {
        Object obj = instantiateBean(type);

        String setterName = "setName";
        String value = "Foo";
        String valueTypeStr = "java.lang.String";
        callSetter(obj, setterName, valueTypeStr, value);

        setterName = "setId";
        UUID val = UUID.fromString("910c80af-a4fa-49fc-b6b4-62eca118fbf7");
        valueTypeStr = "java.util.UUID";
        callSetter(obj, setterName, valueTypeStr, val);

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
