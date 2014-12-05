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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.v1.DataSourceAdapter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfigException;
import com.manniwood.cl4pg.v1.sqlparsers.BasicParserListener;
import com.manniwood.cl4pg.v1.sqlparsers.SqlParser;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;
import com.manniwood.cl4pg.v1.util.SqlCache;
import com.manniwood.cl4pg.v1.util.Str;

/**
 * Run a SQL insert command, filling in the SQL statement's arguments using a
 * list of variadic arguments.
 *
 * @author mwood
 *
 */
public class InsertV implements Command {

    private final static Logger log = LoggerFactory.getLogger(InsertV.class);

    private String sql;
    private final String filename;
    private final Object[] args;
    private PreparedStatement pstmt;

    private InsertV(Builder builder) {
        this.sql = builder.sql;
        this.filename = builder.filename;
        this.args = builder.args;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        TypeConverterStore converterStore,
                        SqlCache sqlCache,
                        DataSourceAdapter dataSourceAdapter) throws Exception {
        if (Str.isNullOrEmpty(sql)) {
            sql = sqlCache.slurpFileFromClasspath(filename);
        }

        BasicParserListener basicParserListener = new BasicParserListener();
        SqlParser sqlParser = new SqlParser(basicParserListener);
        String transformedSql = sqlParser.transform(sql);

        PreparedStatement pstmt = connection.prepareStatement(transformedSql);
        List<String> classNames = basicParserListener.getArgs();

        if (classNames != null && !classNames.isEmpty()) {
            for (int i = 0; i < classNames.size(); i++) {
                converterStore.setSQLArgument(pstmt, i + 1, args[i], classNames.get(i));
            }
        }

        log.debug("Final SQL:\n{}", dataSourceAdapter.unwrapPgPreparedStatement(pstmt));
        pstmt.execute();
    }

    @Override
    public void close() throws Exception {
        if (pstmt != null) {
            pstmt.close();
        }
    }

    public static Builder config() {
        return new Builder();
    }

    public static class Builder {
        private String sql;
        private String filename;
        private Object[] args;

        public Builder() {
            // null constructor
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder file(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder args(Object... args) {
            this.args = args;
            return this;
        }

        public InsertV done() {
            if (Str.isNullOrEmpty(sql) && Str.isNullOrEmpty(filename)) {
                throw new Cl4pgConfigException("SQL string or file must be specified.");
            }
            return new InsertV(this);
        }
    }

}
