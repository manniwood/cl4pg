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
import com.manniwood.mpjw.converters.ConstructorAndConverters;
import com.manniwood.mpjw.converters.ConverterStore;
import com.manniwood.mpjw.converters.SetterAndConverter;

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
        List<SetterAndConverter> settersAndConverters = null;
        // XXX: DEFINITELY SUB-CLASS THIS
        ConstructorAndConverters cac = null;
        switch (beanBuildStyle) {
        case GUESS_SETTERS:
            settersAndConverters = converterStore.guessSetters(rs, returnType);
            break;
        case GUESS_CONSTRUCTOR:
            cac = converterStore.guessConstructor(rs, returnType);
            break;
        case SPECIFY_SETTERS:
            // TODO list.add(converterStore.specifySetters(rs, returnType));
            break;
        case SPECIFY_CONSTRUCTOR:
            // TODO list.add(converterStore.specifyConstructorArgs(rs, returnType));
            break;
        default:
            throw new MPJWException("unknown beanBuildStyle");
        }

        while (rs.next()) {
            log.debug("*****************************");
            switch (beanBuildStyle) {
            case GUESS_SETTERS:
                list.add(converterStore.buildBeanUsingSetters(rs, returnType, settersAndConverters));
                break;
            case GUESS_CONSTRUCTOR:
                list.add(converterStore.buildBeanUsingConstructor(rs, returnType, cac));
                break;
            case SPECIFY_SETTERS:
                // TODO list.add(converterStore.specifySetters(rs, returnType));
                break;
            case SPECIFY_CONSTRUCTOR:
                // TODO list.add(converterStore.specifyConstructorArgs(rs, returnType));
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
