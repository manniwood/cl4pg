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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.cl4pg.Cl4pgException;
import com.manniwood.cl4pg.converters.ConverterStore;
import com.manniwood.cl4pg.util.SqlCache;
import com.manniwood.cl4pg.util.Str;

public class Listen implements Command {
    private final static Logger log = LoggerFactory.getLogger(Listen.class);

    private String sql;
    private PreparedStatement pstmt;
    private String channel;

    public Listen(String channel) {
        if (Str.isNullOrEmpty(channel)) {
            throw new IllegalArgumentException("Channel must be specified.");
        }
        this.channel = channel;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore,
                        SqlCache sqlCache) throws Exception {

        // Ask postgresql to correctly quote this identifier, to make it
        // safe to use in the next sql statement.
        PreparedStatement pstmt1 = connection.prepareStatement("select quote_ident(?)");
        pstmt1.setString(1, channel);
        ResultSet rs = pstmt1.executeQuery();
        if (rs.next()) {
            channel = rs.getString(1);
        } else {
            throw new Cl4pgException("Was not able to quote identifier \"" + channel + "\"");
        }

        sql = "listen " + channel;
        log.debug("Outgoing sql: {}", sql);

        pstmt = connection.prepareStatement(sql);
        pstmt.execute();
    }

    @Override
    public void cleanUp() throws Exception {
        if (pstmt != null) {
            pstmt.close();
        }
    }

}
