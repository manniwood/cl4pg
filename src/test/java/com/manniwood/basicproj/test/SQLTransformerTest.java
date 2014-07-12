package com.manniwood.basicproj.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.manniwood.basicproj.ResourceUtil;
import com.manniwood.basicproj.SQLTransformer;
import com.manniwood.basicproj.TransformedSQL;

public class SQLTransformerTest {

    private final static Logger log = LoggerFactory.getLogger(SQLTransformerTest.class);

    @Test
    public void test() {
        String sql = ResourceUtil.slurpFileFromClasspath("sql/insert_user.sql");
        TransformedSQL tsql = SQLTransformer.transform(sql);
        for (String getter : tsql.getGetters()) {
            log.info("getter: {}", getter);
        }
        log.info("sql:\n{}", tsql.getSql());
    }
}
