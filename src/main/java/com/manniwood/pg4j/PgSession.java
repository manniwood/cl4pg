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
import com.manniwood.mpjw.commands.Rollback;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.pg4j.commands.Command;
import com.manniwood.pg4j.commands.GetNotifications;
import com.manniwood.pg4j.commands.Listen;
import com.manniwood.pg4j.commands.Notify;

public class PgSession {

    private final static Logger log                       = LoggerFactory.getLogger(PgSession.class);

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

    public PgSession() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new MPJWException("Could not find PostgreSQL JDBC Driver", e);
        }
        String url = "jdbc:postgresql://" + hostname + ":" + dbPort + "/" + dbName;
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
        execute(command);
    }

    public void pgNotify(String channel,
                         String payload) {
        execute(new Notify(channel, payload));
    }

    public void pgListen(String channel) {
        execute(new Listen(channel));
    }

    public PGNotification[] getNotifications() {
        GetNotifications getNotifications = new GetNotifications();
        execute(getNotifications);
        return getNotifications.getNotifications();
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

    private void execute(Command command) {
        try {
            command.execute(conn, converterStore);
        } catch (Exception e) {
            // Above, catch Exception instead of SQLException so that you can
            // also
            // roll back on other problems.
            try {
                conn.rollback();
            } catch (Exception e1) {
                // put e inside e1, so the user has all of the exceptions
                e1.initCause(e);
                throw new MPJWException("Could not roll back connection after catching exception trying to execute:\n" + command.getSQL(), e1);
            }
            throw new MPJWException("ROLLED BACK. Exception while trying to run this sql statement:\n" + command.getSQL(), e);
        } finally {
            try {
                command.cleanUp();
            } catch (Exception e) {
                throw new MPJWException("Could not clean up after running the following SQL command; resources may have been left open! SQL command is:\n"
                        + command.getSQL(), e);
            }
        }
    }
}
