package com.manniwood.mpjw;

public class SQLTransformer {

    public static ParsedSQLWithSimpleArgs transformSimply(String sql) {
        SimpleSQLTransformer transformer = new SimpleSQLTransformer(sql);
        transformer.transform();
        return transformer.getParsedSql();
    }

    public static ParsedSQLWithComplexArgs transformWithInOut(String sql) {
        ComplexSQLTransformer transformer = new ComplexSQLTransformer(sql);
        transformer.transform();
        return transformer.getParsedSql();
    }
}
