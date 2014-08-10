package com.manniwood.mpjw.commands;

import java.sql.CallableStatement;

public class CallableStatementCommand extends ConnectionCommand implements Command {

    protected CallableStatement cstmt;

    @Override
    public void cleanUp() throws Exception {
        if (cstmt != null) {
            cstmt.close();
        }
    }

}
