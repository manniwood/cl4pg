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
package com.manniwood.pg4j.v1.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import com.manniwood.pg4j.v1.argsetters.BasicParserListener;
import com.manniwood.pg4j.v1.argsetters.SqlParser;
import com.manniwood.pg4j.v1.converters.ConverterStore;
import com.manniwood.pg4j.v1.util.ResourceUtil;
import com.manniwood.pg4j.v1.util.Str;

public class InsertB<A> implements Command {

    private final String sql;
    private final A arg;
    private PreparedStatement pstmt;

    private InsertB(Builder<A> builder) {
        this.sql = builder.sql;
        this.arg = builder.arg;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore) throws Exception {
        BasicParserListener basicParserListener = new BasicParserListener();
        SqlParser sqlParser = new SqlParser(basicParserListener);
        String transformedSql = sqlParser.transform(sql);

        PreparedStatement pstmt = connection.prepareStatement(transformedSql);
        List<String> getters = basicParserListener.getArgs();
        if (getters != null && !getters.isEmpty()) {
            converterStore.setSQLArguments(pstmt, arg, getters);
        }

        pstmt.execute();
    }

    @Override
    public void cleanUp() throws Exception {
        if (pstmt != null) {
            pstmt.close();
        }
    }

    public static <P> Builder<P> config() {
        return new Builder<P>();
    }

    public static class Builder<A> {
        private String sql;
        private A arg;

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

        public Builder<A> arg(A arg) {
            this.arg = arg;
            return this;
        }

        public InsertB<A> done() {
            if (Str.isNullOrEmpty(sql)) {
                throw new Pg4jConfigException("SQL string or file must be specified.");
            }
            // beanArgSetter has a default, so that's OK.
            // arg should be allowed to be null for those times
            // when there really is no bean argument.
            return new InsertB<A>(this);
        }
    }

}
