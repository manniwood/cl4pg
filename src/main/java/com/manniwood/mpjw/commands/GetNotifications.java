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

import java.sql.Connection;
import java.sql.SQLException;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

public class GetNotifications extends PreparedStatementCommand implements OldCommand {

    /**
     * This dummy query gets run just to get the messages back from
     * the server.
     */
    public static final String DUMMY_QUERY = "select 1";
    private PGNotification[] notifications;

    public GetNotifications(Connection conn) {
        this.sql = DUMMY_QUERY;
        this.conn = conn;
    }

    @Override
    public void execute() throws SQLException {
        pstmt = conn.prepareStatement(sql);
        pstmt.execute();
        notifications = ((PGConnection)conn).getNotifications();
    }

    public PGNotification[] getNotifications() {
        return notifications;
    }
}
