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
package com.manniwood.cl4pg.v1.resultsethandlers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.manniwood.cl4pg.v1.typeconverters.ConstructorAndTypeConverters;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;

/**
 * Guesses the types of constructor arguments for a Java bean of type R, based
 * on the column metadata the result set, and returns a list of beans of type R,
 * one for each row from the result set.
 *
 * So, a two-columned result set with columns whose
 * ResultSetMetaData.getColumnClassName(1) method returns "java.lang.String",
 * and whose ResultSetMetaData.getColumnClassName(2) returns "java.util.UUID",
 * will result in a bean of type R being instantiated using a constructor that
 * takes those two arguments, in that order. A bean will be constructed in that
 * way for each row in the ResultSet.
 *
 * @author mwood
 *
 * @param <R>
 */
public class GuessConstructorListHandler<R> implements ResultSetHandler<R> {

    private List<R> list;
    private ConstructorAndTypeConverters constructorAndConverters;
    private TypeConverterStore converterStore;
    private Class<R> returnType;

    public GuessConstructorListHandler(Class<R> returnType) {
        list = new ArrayList<R>();
        this.returnType = returnType;
    }

    @Override
    public void init(TypeConverterStore converterStore,
                     ResultSet rs) throws SQLException {
        this.converterStore = converterStore;
        constructorAndConverters = converterStore.guessConstructor(rs, returnType);
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
        list.add(converterStore.buildBeanUsingConstructor(rs, returnType, constructorAndConverters));
    }

    public List<R> getList() {
        return list;
    }
}
