package com.manniwood.mpjw.commands;

import java.sql.Connection;

import com.manniwood.pg4j.v1.commands.Command;
import com.manniwood.pg4j.v1.converters.ConverterStore;

public class ConnectionCommand implements Command {

    protected String sql;
    protected Connection conn;

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute(Connection connection,
                        ConverterStore converterStore) throws Exception {
        // do nothing, but be overridden
    }

    @Override
    public void cleanUp() throws Exception {
        // nothing to clean up
    }

}
