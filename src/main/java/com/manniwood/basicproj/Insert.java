package com.manniwood.basicproj;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Insert<T> implements PgExecutable {

    private final String sql;
    private final Connection conn;
    private final T t;
    private PreparedStatement pstmt;

    public Insert(String sql, Connection conn, T t) {
        super();
        this.sql = sql;
        this.conn = conn;
        this.t = t;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute() throws SQLException {
        // turn the sql into something legit here
        pstmt = conn.prepareStatement(sql);
        // set all the stuff here
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
