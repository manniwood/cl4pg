/*
The MIT License (MIT)

Copyright (t) 2014 Manni Wood

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
package com.manniwood.mpjw.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.manniwood.mpjw.BeanBuildStyle;
import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.MoreThanOneResultException;
import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.TransformedSQL;
import com.manniwood.mpjw.converters.ConverterStore;

public class SelectOne<T, P> implements Command {

    private final ConverterStore converterStore;
    private final String sql;
    private final Connection conn;
    private final BeanBuildStyle beanBuildStyle;

    /**
     * Parameter
     */
    private final P parameter;
    private PreparedStatement pstmt;

    /**
     * Return type.
     */
    private T t;

    private Class<T> returnType;

    public SelectOne(
            ConverterStore converterStore,
            String sql,
            Connection conn,
            Class<T> returnType,
            BeanBuildStyle beanBuildStyle,
            P parameter) {
        super();
        this.converterStore = converterStore;
        this.sql = sql;
        this.conn = conn;
        this.parameter = parameter;
        this.beanBuildStyle = beanBuildStyle;
        this.returnType = returnType;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public void execute() throws SQLException {
        TransformedSQL tsql = SQLTransformer.transform(sql);
        pstmt = conn.prepareStatement(tsql.getSql());
        converterStore.setItems(pstmt, parameter, tsql.getGetters());
        ResultSet rs = pstmt.executeQuery();
        if ( ! rs.next()) {
            t = null;
        } else {
            switch (beanBuildStyle) {
            case GUESS_SETTERS:
                t = converterStore.guessSetters(rs, returnType);
                break;
            case GUESS_CONSTRUCTOR:
                t = converterStore.guessConstructor(rs, returnType);
                break;
            case USE_NAMED_SETTERS:
                // TODO
                t = converterStore.guessConstructor(rs, returnType);
                break;
            case USE_NAMED_CLASSES_FOR_CONSTRUCTOR:
                // TODO
                t = converterStore.guessConstructor(rs, returnType);
                break;
            default:
                throw new MPJWException("unknown beanBuildStyle");
            }
        }
        if (rs.next()) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + tsql.getSql());
        }
    }

    public T getResult() {
        return t;
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
