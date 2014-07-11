package com.manniwood.basicproj;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Commit implements PgExecutable {

    private final String sql;
    private final Connection conn;
    private PreparedStatement pstmt;

    public Commit(Connection conn) {
        super();
        this.sql = "commit;";
        this.conn = conn;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute() throws SQLException {
        conn.commit();
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public PreparedStatement getPreparedStatement() {
        return pstmt;
    }

}
