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
import java.sql.ResultSet;
import java.util.List;

import com.manniwood.pg4j.v1.converters.ConverterStore;
import com.manniwood.pg4j.v1.resultsethandlers.ResultSetHandler;
import com.manniwood.pg4j.v1.sqlparsers.BasicParserListener;
import com.manniwood.pg4j.v1.sqlparsers.SqlParser;
import com.manniwood.pg4j.v1.util.Cllctn;
import com.manniwood.pg4j.v1.util.ResourceUtil;
import com.manniwood.pg4j.v1.util.Str;

public class SelectV implements Command {

    private final String sql;
    private final ResultSetHandler resultSetHandler;
    private final Object[] args;
    private PreparedStatement pstmt;

    private SelectV(Builder builder) {
        this.sql = builder.sql;
        this.resultSetHandler = builder.resultSetHandler;
        this.args = builder.args;
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
        List<String> classNames = basicParserListener.getArgs();

        if (!Cllctn.isNullOrEmpty(classNames)) {
            for (int i = 0; i < classNames.size(); i++) {
                converterStore.setSQLArgument(pstmt, i + 1, args[i], classNames.get(i));
            }
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

    public static Builder config() {
        return new Builder();
    }

    public static class Builder {
        private String sql;
        private ResultSetHandler resultSetHandler;
        private Object[] args;

        public Builder() {
            // null constructor
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder file(String filename) {
            this.sql = ResourceUtil.slurpFileFromClasspath(filename);
            return this;
        }

        public Builder resultSetHandler(ResultSetHandler resultSetHandler) {
            this.resultSetHandler = resultSetHandler;
            return this;
        }

        public Builder args(Object... args) {
            this.args = args;
            return this;
        }

        public SelectV done() {
            if (Str.isNullOrEmpty(sql)) {
                throw new Pg4jConfigException("SQL string or file must be specified.");
            }
            if (resultSetHandler == null) {
                throw new Pg4jConfigException("A result set handler must be specified.");
            }
            // beanArgSetter has a default, so that's OK.
            // args should be allowed to be null for those times
            // when there really are no arguments.
            return new SelectV(this);
        }
    }

}
