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

import java.lang.reflect.Method;

import com.manniwood.cl4pg.v1.typeconverters.types.TypeConverter;

/**
 * Holds a setter method for a bean, a typeConverter that will convert a
 * result-set's column into the Java type required by the setter method forr the
 * bean, and the SQL column number (starting from 1, not 0) that this converter
 * and setter correspond to.
 *
 * @author mwood
 *
 */
public class SetterAndTypeConverterAndColNum {

    private final TypeConverter<?> typeConverter;
    private final Method setter;
    private final int colNum;
    private final int setCol;

    public SetterAndTypeConverterAndColNum(
            TypeConverter<?> converter,
            Method setter,
            int colNum,
            int setCol) {
        super();
        this.typeConverter = converter;
        this.setter = setter;
        this.colNum = colNum;
        this.setCol = setCol;
    }

    public TypeConverter<?> getConverter() {
        return typeConverter;
    }

    public Method getSetter() {
        return setter;
    }

    public int getColNum() {
        return colNum;
    }

    public int getSetCol() {
        return setCol;
    }
}
