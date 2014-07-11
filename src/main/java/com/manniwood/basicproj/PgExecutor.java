package com.manniwood.basicproj;

import java.sql.SQLException;

public class PgExecutor {
    public static void execute(PgExecutable pg) {
        try {
            pg.execute();
        } catch (SQLException e) {
            try {
                pg.getConnection().rollback();
            } catch (SQLException e1) {
                // XXX: do we put e inside e1, so the user has all of the exceptions?
                throw new MPJWException("Could not roll back connection after catching exception trying to execute " + pg.getSQL(), e1);
            }
            throw new MPJWException("ROLLED BACK. Exception while trying to run this sql statement: " + pg.getSQL(), e);
        } finally {
            if (pg.getPreparedStatement() != null) {
                try {
                    pg.getPreparedStatement().close();
                } catch (SQLException e) {
                    throw new MPJWException("Could not close PreparedStatement for " + pg.getSQL(), e);
                }
            }
        }
    }
}
