package com.manniwood.mpjw.commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.manniwood.mpjw.BeanBuildStyle;
import com.manniwood.mpjw.MPJWException;
import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.TransformedSQL;
import com.manniwood.mpjw.converters.ConverterStore;

public abstract class SelectListBase<T> implements Command {

    private final static Logger log = LoggerFactory.getLogger(SelectListBase.class);

    protected final ConverterStore converterStore;
    protected final String sql;
    protected final Connection conn;
    protected final Class<T> returnType;
    protected final BeanBuildStyle beanBuildStyle;

    protected PreparedStatement pstmt;

    /**
     * Return object.
     */
    protected List<T> list;

    public SelectListBase(
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
        list = new ArrayList<T>();
        log.debug("beanBuildStyle: {}", beanBuildStyle);
        while (rs.next()) {
            log.debug("*****************************");
            // XXX: this REALLY BADLY needs to be made more efficient
            // Only want to figure out the beanBuildStyle once;
            // Want to cache the guessed or specified setters once
            // and then use throughout the loop.
            switch (beanBuildStyle) {
            case GUESS_SETTERS:
                list.add(converterStore.guessSettersAndInvoke(rs, returnType));
                break;
            case GUESS_CONSTRUCTOR:
                list.add(converterStore.guessConstructor(rs, returnType));
                break;
            case SPECIFY_SETTERS:
                list.add(converterStore.specifySetters(rs, returnType));
                break;
            case SPECIFY_CONSTRUCTOR:
                list.add(converterStore.specifyConstructorArgs(rs, returnType));
                break;
            default:
                throw new MPJWException("unknown beanBuildStyle");
            }
        }
        // Empty results should just return null
        if (list.isEmpty()) {
            list = null;
        }
    }

    protected abstract void convertItems(TransformedSQL tsql) throws SQLException;

    public List<T> getResult() {
        return list;
    }
}
