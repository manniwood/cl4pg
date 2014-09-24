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
package com.manniwood.pg4j.v1.resultsethandlers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.manniwood.pg4j.v1.converters.ConstructorAndConverters;
import com.manniwood.pg4j.v1.converters.ConverterStore;

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
