/*
The MIT License (MIT)

Copyright (c) 2015 Manni Wood

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
package com.manniwood.cl4pg.v1.util;

import com.manniwood.cl4pg.v1.ConfigDefaults;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgReflectionException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Properties;

/**
 * Reflection utilities.
 * @author mwood
 *
 */
public class PropsUtil {

    private PropsUtil() {
        // utility class
    }

    /**
     * Gets a property by first seeing if system property "cl4pg.[key]" exists, and returning that if it does; if
     * the property for "cl4pg.[key]" does not exist, the property for the provided properties[key] is returned instead.
     * @param properties
     * @param key
     * @return
     */
    public static String getPropFromAll(Properties properties, String key) {
        if (properties == null) {
            throw new NullPointerException("Cannot determine property because properties object was null");
        }
        String property = System.getProperty(ConfigDefaults.PROJ_NAME + "." + key);
        if (Str.isNullOrEmpty(property)) {
            return properties.getProperty(key);
        }
        return null;
    }
}
