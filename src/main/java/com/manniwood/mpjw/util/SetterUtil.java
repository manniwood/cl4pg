package com.manniwood.mpjw.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.manniwood.mpjw.MPJWException;

public class SetterUtil {
    public static <T> void setItems(PreparedStatement pstmt, T t, List<String> getters) throws SQLException {
        int i = 0;
        Class<? extends Object> tclass = t.getClass();
        try {
            for (String getter : getters) {
                i++;
                Method m = tclass.getMethod(getter);
                Class<?> returnClass = m.getReturnType();
                if (returnClass == int.class) {
                    int myInt = ((Integer)m.invoke(t)).intValue();
                    pstmt.setInt(i, myInt);
                } else if (returnClass == String.class) {
                    String myStr = (String)m.invoke(t);
                    pstmt.setString(i, myStr);
                } else if (returnClass == UUID.class) {
                    UUID uuid = (UUID)m.invoke(t);
                    pstmt.setObject(i, uuid);
                }
            }
        } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new MPJWException(e);
        }

    }
}
