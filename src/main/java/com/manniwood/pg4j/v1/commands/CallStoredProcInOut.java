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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.util.List;

import com.manniwood.pg4j.v1.converters.ConverterStore;
import com.manniwood.pg4j.v1.converters.SetterAndConverterAndColNum;
import com.manniwood.pg4j.v1.sqlparsers.InOutArg;
import com.manniwood.pg4j.v1.sqlparsers.SlashParserListener;
import com.manniwood.pg4j.v1.sqlparsers.SqlParser;
import com.manniwood.pg4j.v1.util.ResourceUtil;
import com.manniwood.pg4j.v1.util.Str;

public class CallStoredProcInOut<A> implements Command {

    private final String sql;
    private final A arg;
    private CallableStatement cstmt;

    private CallStoredProcInOut(Builder<A> builder) {
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
        SlashParserListener slashParserListener = new SlashParserListener();
        SqlParser sqlParser = new SqlParser(slashParserListener);
        String transformedSql = sqlParser.transform(sql);

        CallableStatement cstmt = connection.prepareCall(transformedSql);
        List<InOutArg> gettersAndSetters = slashParserListener.getArgs();

        if (gettersAndSetters != null && !gettersAndSetters.isEmpty()) {
            converterStore.setSQLArguments(cstmt, arg, gettersAndSetters);
        }

        cstmt.execute();

        // There is no result set handler here; we just set the
        // out parameters on the argument bean
        List<SetterAndConverterAndColNum> settersAndConverters = converterStore.specifySetters(cstmt, arg.getClass(), gettersAndSetters);
        converterStore.populateBeanUsingSetters(cstmt, arg, settersAndConverters);
    }

    @Override
    public void cleanUp() throws Exception {
        if (cstmt != null) {
            cstmt.close();
        }
    }

    public static <A> Builder<A> config() {
        return new Builder<A>();
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

        public CallStoredProcInOut<A> done() {
            if (Str.isNullOrEmpty(sql)) {
                throw new Pg4jConfigException("SQL string or file must be specified.");
            }
            return new CallStoredProcInOut<A>(this);
        }
    }

}
