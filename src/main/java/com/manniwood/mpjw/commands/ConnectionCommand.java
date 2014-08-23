package com.manniwood.mpjw.commands;

import java.sql.Connection;

public class ConnectionCommand implements OldCommand {

    protected String sql;
    protected Connection conn;

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute() throws Exception {
        // do nothing, but be overridden
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public void cleanUp() throws Exception {
        // nothing to clean up
    }

}
