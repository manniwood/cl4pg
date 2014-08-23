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
import com.manniwood.pg4j.argsetters.BeanArgSetter;
import com.manniwood.pg4j.resultsethandlers.ResultSetHandler;

public class SelectB<A> implements Command {

    private final String           sql;
    private final BeanArgSetter<A> beanArgSetter;
    private final ResultSetHandler resultSetHandler;
    private final A                param;
    private PreparedStatement      pstmt;

    public SelectB(Builder<A> builder) {
        //@formatter:off
        this.sql                = builder.sql;
        this.beanArgSetter      = builder.beanArgSetter;
        this.resultSetHandler   = builder.resultSetHandler;
        this.param              = builder.arg;
        //@formatter:on
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore) throws Exception {
        pstmt = beanArgSetter.setSQLArguments(sql,
                                              connection,
                                              converterStore,
                                              param);
        ResultSet rs = pstmt.executeQuery();

        resultSetHandler.init(converterStore, rs);
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

    public static <A> Builder<A> config() {
        return new Builder<A>();
    }

    public static class Builder<A> {
        private String           sql;
        private BeanArgSetter<A> beanArgSetter;
        private ResultSetHandler resultSetHandler;
        private A                arg;

        public Builder() {
            // null constructor
        }

        public Builder<A> sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder<A> file(String filename) {
            this.sql = ResourceUtil.slurpFileFromClasspath(filename);
            return this;
        }

        public Builder<A> argSetter(BeanArgSetter<A> beanArgSetter) {
            this.beanArgSetter = beanArgSetter;
            return this;
        }

        public Builder<A> resultSetHandler(ResultSetHandler resultSetHandler) {
            this.resultSetHandler = resultSetHandler;
            return this;
        }

        public Builder<A> arg(A arg) {
            this.arg = arg;
            return this;
        }

        public SelectB<A> done() {
            return new SelectB<A>(this);
        }
    }

}
