/*
The MIT License (MIT)

Copyright (t) 2014 Manni Wood

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

import com.manniwood.cl4pg.v1.converters.ConstructorAndConverters;
import com.manniwood.cl4pg.v1.converters.ConverterStore;

/**
 * Reads the types of constructor arguments for a Java bean of type R, based on
 * the explicit column aliases in the result set, and returns a list of beans of
 * type R, one for each row from the result set.
 *
 * <p>
 * Let's assume Cl4pg is iterating through a result set from this piece of SQL:
 *
 * <pre>
 * <code>
 * select id          as "java.util.UUID",
 *        name        as "java.lang.String",
 *        password    as "java.lang.String",
 *        employee_id as "int"
 *   from users
 *  where id = 1
 * </code>
 * </pre>
 *
 * <p>
 * For each row in the result set:
 *
 * <p>
 * A bean of type R will be constructed using a constructor with the signature
 * new R(UUID, String, String, int). So, obviously, it is important that type R
 * <strong>has</strong> a method matching that signature.
 *
 * @author mwood
 *
 * @param <R>
 */
public class ExplicitConstructorListHandler<R> implements ResultSetHandler {

    private List<R> list;
    private ConstructorAndConverters constructorAndConverters;
    private ConverterStore converterStore;
    private Class<R> returnType;

    public ExplicitConstructorListHandler(Class<R> returnType) {
        list = new ArrayList<R>();
        this.returnType = returnType;
    }

    @Override
    public void init(ConverterStore converterStore,
                     ResultSet rs) throws SQLException {
        this.converterStore = converterStore;
        constructorAndConverters = converterStore.specifyConstructorArgs(rs, returnType);
    }

    @Override
    public void processRow(ResultSet rs) throws SQLException {
        list.add(converterStore.buildBeanUsingConstructor(rs, returnType, constructorAndConverters));
    }

    public List<R> getList() {
        return list;
    }
}
