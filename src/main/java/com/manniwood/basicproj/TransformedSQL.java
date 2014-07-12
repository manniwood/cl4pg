package com.manniwood.basicproj;

import java.util.List;

public class TransformedSQL {
    private final String sql;
    private final List<String> getters;
    public TransformedSQL(String sql, List<String> getters) {
        super();
        this.sql = sql;
        this.getters = getters;
    }
    public String getSql() {
        return sql;
    }
    public List<String> getGetters() {
        return getters;
    }

}
