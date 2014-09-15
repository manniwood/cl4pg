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

import com.manniwood.mpjw.InOutArg;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.mpjw.converters.SetterAndConverterAndColNum;
import com.manniwood.mpjw.util.ResourceUtil;
import com.manniwood.pg4j.v1.argsetters.InOutArgSetter;
import com.manniwood.pg4j.v1.argsetters.InOutBeanArgSetter;
import com.manniwood.pg4j.v1.util.Str;

public class CallStoredProcInOut<A> implements Command {

    private final String sql;
    private final A param;
    private CallableStatement cstmt;

    private CallStoredProcInOut(Builder<A> builder) {
        this.sql = builder.sql;
        this.param = builder.arg;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore) throws Exception {
        // Stored procs with in/out params will only work with
        // ComplexBeanArgSetter, so hard-code it here.
        InOutBeanArgSetter<A> beanArgSetter = new InOutBeanArgSetter<>();
        cstmt = beanArgSetter.setSQLArguments(sql,
                                              connection,
                                              converterStore,
                                              param);
        cstmt.execute();

        // There is no result set handler here; we just set the
        // out parameters on the argument bean
        List<InOutArg> args = ((InOutArgSetter) beanArgSetter).getArgs();
        List<SetterAndConverterAndColNum> settersAndConverters = converterStore.specifySetters(cstmt, param.getClass(), args);
        converterStore.populateBeanUsingSetters(cstmt, param, settersAndConverters);
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
            // beanArgSetter has a default, so that's OK.
            // arg should be allowed to be null for those times
            // when there really is no bean argument.
            return new CallStoredProcInOut<A>(this);
        }
    }

}
