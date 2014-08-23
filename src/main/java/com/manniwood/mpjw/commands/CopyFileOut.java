/*
The MIT License (MIT)

Copyright (c) 2014 Manni Wood

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
package com.manniwood.mpjw.commands;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

public class CopyFileOut extends ConnectionCommand implements OldCommand {

    private final String copyFileName;
    private Writer fileWriter = null;

    public CopyFileOut(Connection conn, String copyFileName, String sql) {
        this.conn = conn;
        this.copyFileName = copyFileName;
        this.sql = sql;
    }

    @Override
    public void execute() throws SQLException, IOException {
        CopyManager copyManager = ((PGConnection)conn).getCopyAPI();
        fileWriter = new FileWriter(copyFileName);
        copyManager.copyOut(sql, fileWriter);
    }

    @Override
    public void cleanUp() throws Exception {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}
