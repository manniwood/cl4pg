package com.manniwood.mpjw.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.converters.Converter;
import com.manniwood.mpjw.converters.IntConverter;
import com.manniwood.mpjw.converters.StringConverter;
import com.manniwood.mpjw.converters.UUIDConverter;

public class SetterUtil {

    private static Map<Class, Converter> converters;

    static {
        converters = new HashMap<>();
        converters.put(int.class, new IntConverter());
        converters.put(String.class, new StringConverter());
        converters.put(UUID.class, new UUIDConverter());
    }

    public static <T> void setItems(PreparedStatement pstmt, T t, List<String> getters) throws SQLException {
        int i = 0;
        Class<? extends Object> tclass = t.getClass();
        try {
            for (String getter : getters) {
                i++;
                Method m = tclass.getMethod(getter);
                Object o = m.invoke(t);
                Class<?> returnClass = m.getReturnType();
                Converter<T> converter = converters.get(returnClass);
                converter.setItem(pstmt, i, (T)o);
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException(e);
        }

    }
}
