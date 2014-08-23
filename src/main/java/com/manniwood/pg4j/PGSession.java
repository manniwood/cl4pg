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
package com.manniwood.pg4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.CommandRunner;
import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.commands.Commit;
import com.manniwood.mpjw.commands.CopyFileIn;
import com.manniwood.mpjw.commands.CopyFileOut;
import com.manniwood.mpjw.commands.GetNotifications;
import com.manniwood.mpjw.commands.Listen;
import com.manniwood.mpjw.commands.Notify;
import com.manniwood.mpjw.commands.Rollback;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.pg4j.commands.Command;

public class PGSession {

    private final static Logger log                       = LoggerFactory
                                                                  .getLogger(PGSession.class);

    private Connection          conn                      = null;

    // XXX: make all of these dynamically settable
    private String              hostname                  = "localhost";
    private int                 dbPort                    = 5432;
    private String              dbName                    = "postgres";
    private String              dbUser                    = "postgres";
    private String              dbPassword                = "postgres";
    private int                 transactionIsolationLevel = Connection.TRANSACTION_READ_COMMITTED;
    private String              appName                   = "MPJW";

    private ConverterStore      converterStore            = new ConverterStore();

    public PGSession() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new MPJWException("Could not find PostgreSQL JDBC Driver", e);
        }
        String url = "jdbc:postgresql://" + hostname + ":" + dbPort + "/"
                + dbName;
        Properties props = new Properties();
        props.setProperty("user", dbUser);
        props.setProperty("password", dbPassword);
        props.setProperty("ApplicationName", appName);
        log.info("Application Name: {}", appName);
        try {
            conn = DriverManager.getConnection(url, props);
            conn.setTransactionIsolation(transactionIsolationLevel);
            conn.setAutoCommit(false);
        } catch (SQLException e) {
            throw new MPJWException("Could not connect to db", e);
        }
    }

    public void run(Command command) {
        CommandRunner.execute(command, conn, converterStore);
    }

    public void pgNotify(String channel,
                         String payload) {
        CommandRunner
                .execute(new Notify(converterStore, conn, channel, payload));
    }

    public void pgListen(String channel) {
        Listen listen = new Listen(conn, channel);
        CommandRunner.execute(listen);
    }

    public PGNotification[] getNotifications() {
        GetNotifications n = new GetNotifications(conn);
        CommandRunner.execute(n);
        return n.getNotifications();
    }

    public void commit() {
        CommandRunner.execute(new Commit(conn));
    }

    public void rollback() {
        CommandRunner.execute(new Rollback(conn));
    }

    // TODO: make a version of this that just takes a reader, too
    public void copyIn(String copyFileName,
                       String sql) {
        CommandRunner.execute(new CopyFileIn(conn, copyFileName, sql));
    }

    // TODO: make a version of this that just takes a reader, too
    public void copyOut(String copyFileName,
                        String sql) {
        CommandRunner.execute(new CopyFileOut(conn, copyFileName, sql));
    }

    // TODO: make copy between two connections

}
