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

import java.lang.reflect.Constructor;
import java.util.List;

import com.manniwood.cl4pg.v1.typeconverters.types.TypeConverter;

/**
 * Holds a bean constructor and all of the TypeConverters, in order, for the
 * constructor arguments of that bean. Used to cache info about beans we want to
 * construct from result set rows.
 *
 * @author mwood
 *
 */
public class ConstructorAndTypeConverters {

    private final Constructor<?> constructor;
    private final List<TypeConverter<?>> typeConverters;

    public ConstructorAndTypeConverters(Constructor<?> constructor,
            List<TypeConverter<?>> converters) {
        super();
        this.constructor = constructor;
        this.typeConverters = converters;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public List<TypeConverter<?>> getConverters() {
        return typeConverters;
    }

}
