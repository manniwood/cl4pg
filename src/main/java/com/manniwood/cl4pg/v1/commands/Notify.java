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
package com.manniwood.cl4pg.v1.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.v1.DataSourceAdapter;
import com.manniwood.cl4pg.v1.converters.ConverterStore;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfigException;
import com.manniwood.cl4pg.v1.util.SqlCache;
import com.manniwood.cl4pg.v1.util.Str;

public class Notify implements Command {
    private final static Logger log = LoggerFactory.getLogger(Notify.class);

    private final String sql = "select pg_notify(?, ?)";
    private final String channel;
    private final String payload;
    private PreparedStatement pstmt;

    private Notify(Builder builder) {
        if (Str.isNullOrEmpty(builder.channel)) {
            throw new Cl4pgConfigException("Channel must be specified.");
        }
        if (Str.isNullOrEmpty(builder.payload)) {
            throw new Cl4pgConfigException("Payload must be specified.");
        }
        this.channel = builder.channel;
        this.payload = builder.payload;
    }

    public static Builder config() {
        return new Builder();
    }

    public static class Builder {
        private String channel;
        private String payload;

        public Builder() {
            // null constructor
        }

        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        public Notify done() {
            return new Notify(this);
        }
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore,
                        SqlCache sqlCache,
                        DataSourceAdapter dataSourceAdapter) throws Exception {
        pstmt = connection.prepareStatement(sql);
        converterStore.setSQLArgument(pstmt, 1, channel, String.class.getName());
        converterStore.setSQLArgument(pstmt, 2, payload, String.class.getName());
        log.debug("Final SQL:\n{}", dataSourceAdapter.unwrapPgPreparedStatement(pstmt));
        pstmt.execute();
    }

    @Override
    public void close() throws Exception {
        if (pstmt != null) {
            pstmt.close();
        }
    }

}
