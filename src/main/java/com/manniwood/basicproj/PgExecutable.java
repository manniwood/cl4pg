package com.manniwood.basicproj;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface PgExecutable {
    String getSQL();
    void execute() throws SQLException;
    Connection getConnection();
    PreparedStatement getPreparedStatement();
}
