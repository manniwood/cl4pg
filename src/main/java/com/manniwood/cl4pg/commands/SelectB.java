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
package com.manniwood.cl4pg.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import com.manniwood.cl4pg.converters.ConverterStore;
import com.manniwood.cl4pg.resultsethandlers.ResultSetHandler;
import com.manniwood.cl4pg.sqlparsers.BasicParserListener;
import com.manniwood.cl4pg.sqlparsers.SqlParser;
import com.manniwood.cl4pg.util.SqlCache;
import com.manniwood.cl4pg.util.Str;

public class SelectB<A> implements Command {

    private final String sql;
    private final String filename;
    private final ResultSetHandler resultSetHandler;
    private final A arg;
    private PreparedStatement pstmt;

    private SelectB(Builder<A> builder) {
        this.sql = builder.sql;
        this.filename = builder.filename;
        this.resultSetHandler = builder.resultSetHandler;
        this.arg = builder.arg;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore,
                        SqlCache sqlCache) throws Exception {
        String theSql = sql == null ? sqlCache.slurpFileFromClasspath(filename) : sql;

        BasicParserListener basicParserListener = new BasicParserListener();
        SqlParser sqlParser = new SqlParser(basicParserListener);
        String transformedSql = sqlParser.transform(theSql);

        PreparedStatement pstmt = connection.prepareStatement(transformedSql);
        List<String> getters = basicParserListener.getArgs();
        if (getters != null && !getters.isEmpty()) {
            converterStore.setSQLArguments(pstmt, arg, getters);
        }

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
        private String sql;
        private String filename;
        private ResultSetHandler resultSetHandler;
        private A arg;

        public Builder() {
            // null constructor
        }

        public Builder<A> sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder<A> file(String filename) {
            this.filename = filename;
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
            if (Str.isNullOrEmpty(sql) && Str.isNullOrEmpty(filename)) {
                throw new Cl4pgConfigException("SQL string or file must be specified.");
            }
            if (resultSetHandler == null) {
                throw new Cl4pgConfigException("A result set handler must be specified.");
            }
            return new SelectB<A>(this);
        }
    }

}
