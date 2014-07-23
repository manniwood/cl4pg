package com.manniwood.mpjw.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.BeanBuildStyle;
import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.MoreThanOneResultException;
import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.TransformedSQL;
import com.manniwood.mpjw.converters.ConverterStore;

public abstract class SelectOneBase<T> implements Command {

    private final static Logger log = LoggerFactory.getLogger(SelectOneBase.class);

    protected final ConverterStore converterStore;
    protected final String sql;
    protected final Connection conn;
    protected final Class<T> returnType;
    protected final BeanBuildStyle beanBuildStyle;

    protected PreparedStatement pstmt;

    /**
     * Return object.
     */
    protected T t;

    public SelectOneBase(
            ConverterStore converterStore,
            String sql,
            Connection conn,
            Class<T> returnType,
            BeanBuildStyle beanBuildStyle) {
        super();
        this.converterStore = converterStore;
        this.sql = sql;
        this.conn = conn;
        this.beanBuildStyle = beanBuildStyle;
        this.returnType = returnType;
    }

    @Override
    public String getSQL() {
        return sql;
    }

    @Override
    public Connection getConnection() {
        return conn;
    }

    @Override
    public PreparedStatement getPreparedStatement() {
        return pstmt;
    }

    @Override
    public void execute() throws SQLException {
        TransformedSQL tsql = SQLTransformer.transform(sql);
        pstmt = conn.prepareStatement(tsql.getSql());
        convertItems(tsql);
        ResultSet rs = pstmt.executeQuery();
        if ( ! rs.next()) {
            t = null;
        } else {
            log.debug("beanBuildStyle: {}", beanBuildStyle);
            // XXX: Switch statements are usually opportunities for sub-classing.
            switch (beanBuildStyle) {
            case GUESS_SETTERS:
                t = converterStore.guessSettersAndInvoke(rs, returnType);
                break;
            case GUESS_CONSTRUCTOR:
                t = converterStore.guessConstructor(rs, returnType);
                break;
            case SPECIFY_SETTERS:
                t = converterStore.specifySetters(rs, returnType);
                break;
            case SPECIFY_CONSTRUCTOR:
                t = converterStore.specifyConstructorArgs(rs, returnType);
                break;
            default:
                throw new MPJWException("unknown beanBuildStyle");
            }
        }
        if (rs.next()) {
            throw new MoreThanOneResultException("More than one result found when trying to get only one result running the following query:\n" + tsql.getSql());
        }
    }

    protected abstract void convertItems(TransformedSQL tsql) throws SQLException;

    public T getResult() {
        return t;
    }
}
