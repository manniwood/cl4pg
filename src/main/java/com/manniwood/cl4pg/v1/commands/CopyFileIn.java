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

import java.io.FileReader;
import java.io.Reader;
import java.sql.Connection;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import com.manniwood.cl4pg.v1.DataSourceAdapter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgConfigException;
import com.manniwood.cl4pg.v1.typeconverters.TypeConverterStore;
import com.manniwood.cl4pg.v1.util.SqlCache;
import com.manniwood.cl4pg.v1.util.Str;

public class CopyFileIn implements Command {

    private final String copyFile;
    private final String sql;
    private final String filename;
    private Reader fileReader = null;

    private CopyFileIn(Builder builder) {
        this.copyFile = builder.copyFile;
        this.sql = builder.sql;
        this.filename = builder.filename;
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
        String theSql = sql == null ? sqlCache.slurpFileFromClasspath(filename) : sql;
        PGConnection pgConn = dataSourceAdapter.unwrapPgConnection(connection);
        CopyManager copyManager = (pgConn).getCopyAPI();
        fileReader = new FileReader(copyFile);
        copyManager.copyIn(theSql, fileReader);
    }

    @Override
    public void close() throws Exception {
        if (fileReader != null) {
            fileReader.close();
        }
    }

    public static Builder config() {
        return new Builder();
    }

    public static class Builder {
        private String copyFile;
        private String sql;
        private String filename;

        public Builder() {
            // null constructor
        }

        public Builder copyFile(String copyFile) {
            this.copyFile = copyFile;
            return this;
        }

        public Builder sql(String sql) {
            this.sql = sql;
            return this;
        }

        public Builder file(String filename) {
            this.filename = filename;
            return this;
        }

        public CopyFileIn done() {
            if (Str.isNullOrEmpty(sql) && Str.isNullOrEmpty(filename)) {
                throw new Cl4pgConfigException("SQL string or file must be specified.");
            }
            return new CopyFileIn(this);
        }
    }

}
