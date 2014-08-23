package com.manniwood.mpjw.commands;

import java.sql.PreparedStatement;

public class PreparedStatementCommand extends ConnectionCommand implements OldCommand {

    protected PreparedStatement pstmt;

    @Override
    public void cleanUp() throws Exception {
        if (pstmt != null) {
            pstmt.close();
        }
    }

}
