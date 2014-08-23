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
package com.manniwood.pg4j.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.mpjw.util.ResourceUtil;
import com.manniwood.pg4j.argsetters.VariadicArgSetter;
import com.manniwood.pg4j.resultsethandlers.ResultSetHandlerB;

public class SelectVB<R> implements Command {

    private final String               sql;
    private final VariadicArgSetter    variadicArgSetter;
    private final ResultSetHandlerB<R> resultSetHandler;
    private final Object[]             params;
    private final Class<R>             returnType;
    private PreparedStatement          pstmt;

    public SelectVB(Builder<R> builder) {
        //@formatter:off
        this.sql                = builder.sql;
        this.variadicArgSetter  = builder.variadicArgSetter;
        this.resultSetHandler   = builder.resultSetHandler;
        this.returnType         = builder.returnType;
        this.params             = builder.params;
        //@formatter:on
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore) throws Exception {
        pstmt = variadicArgSetter.setSQLArguments(sql,
                                                  connection,
                                                  converterStore,
                                                  params);
        ResultSet rs = pstmt.executeQuery();

        resultSetHandler.init(converterStore, rs, returnType);
        while (rs.next()) {
            resultSetHandler.processRow(rs);
        }
    }

    @Override
    public void cleanUp() throws Exception {
        if (pstmt != null) {
            pstmt.close();
        }
    }

    public static <R> Builder<R> config() {
        return new Builder<R>();
    }

    public static class Builder<R> {
        private String               sql;
        private VariadicArgSetter    variadicArgSetter;
        private ResultSetHandlerB<R> resultSetHandler;
        private Object[]             params;
        private Class<R>             returnType;

        public Builder() {
            // null constructor
        }

        public Builder<R> sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder<R> file(String filename) {
            this.sql = ResourceUtil.slurpFileFromClasspath(filename);
            return this;
        }

        public Builder<R> variadicArgSetter(VariadicArgSetter variadicArgSetter) {
            this.variadicArgSetter = variadicArgSetter;
            return this;
        }

        public Builder<R> resultSetHandler(ResultSetHandlerB<R> resultSetHandler) {
            this.resultSetHandler = resultSetHandler;
            return this;
        }

        public Builder<R> params(Object... params) {
            this.params = params;
            return this;
        }

        public Builder<R> returnType(Class<R> returnType) {
            this.returnType = returnType;
            return this;
        }

        public SelectVB<R> done() {
            return new SelectVB<R>(this);
        }
    }

}
