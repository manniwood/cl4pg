/*
The MIT License (MIT)

Copyright (c) 2014 Manni Wood

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
package com.manniwood.mpjw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.manniwood.mpjw.ResourceUtil;
import com.manniwood.mpjw.SQLTransformer;
import com.manniwood.mpjw.TransformedSQL;

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
