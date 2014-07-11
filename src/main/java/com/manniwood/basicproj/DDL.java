package com.manniwood.basicproj;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DDL implements PgExecutable {

    private final String sql;
    private final Connection conn;
    private PreparedStatement pstmt;

    public DDL(String sql, Connection conn) {
        super();
        this.sql = sql;
        this.conn = conn;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute() throws SQLException {
        pstmt = conn.prepareStatement(sql);
        pstmt.execute();
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
