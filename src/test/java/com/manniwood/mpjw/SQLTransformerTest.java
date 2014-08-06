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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.manniwood.mpjw.util.ResourceUtil;

public class SQLTransformerTest {

    private final static Logger log = LoggerFactory.getLogger(SQLTransformerTest.class);

    public static List<String> correctGetters;

    public static List<String> correctScalarGetters;


    public static final String EXPECTED_SCALAR = "java.util.UUID";

    @BeforeClass
    public void init() {
        correctGetters = new ArrayList<String>();
        correctGetters.add("getId");
        correctGetters.add("getName");
        correctGetters.add("getPassword");
        correctGetters.add("getEmployeeId");

        correctScalarGetters = new ArrayList<String>();
        correctScalarGetters.add(EXPECTED_SCALAR);
    }

    @Test
    public void testBean() {
        String sql = ResourceUtil.slurpFileFromClasspath("sql/insert_user.sql");
        ParsedSQLWithSimpleArgs tsql = SQLTransformer.transformSimply(sql);
        List<String> getters = tsql.getArgs();
        Assert.assertEquals(getters.size(), correctGetters.size(), "Must have all getters.");
        for (int i = 0; i < correctGetters.size(); i++) {
            Assert.assertEquals(getters.get(i), correctGetters.get(i));
            log.info("getter: {}, expected getter: {}", getters.get(i), correctGetters.get(i));
        }
        // XXX: how to I test that question marks replaced names?
        log.info("sql:\n{}", tsql.getSql());
    }

    @Test
    public void testScalar() {
        String sql = ResourceUtil.slurpFileFromClasspath("sql/select_user_guess_setters.sql");
        ParsedSQLWithSimpleArgs tsql = SQLTransformer.transformSimply(sql);
        List<String> getters = tsql.getArgs();
        Assert.assertEquals(getters.size(), correctScalarGetters.size(), "Must have all getters.");
        for (int i = 0; i < correctScalarGetters.size(); i++) {
            Assert.assertEquals(getters.get(i), correctScalarGetters.get(i));
            log.info("getter: {}, expected getter: {}", getters.get(i), correctScalarGetters.get(i));
        }
        // XXX: how to I test that question marks replaced names?
        log.info("sql:\n{}", tsql.getSql());
    }

    @Test
    public void testComplex1() {
        List<ComplexArg> correctComplexArgs = new ArrayList<>();
        correctComplexArgs.add(new ComplexArg("getFirst", "setFirst"));
        correctComplexArgs.add(new ComplexArg("getSecond", "setSecond"));

        String sql = ResourceUtil.slurpFileFromClasspath("sql/swap.sql");
        ParsedSQLWithComplexArgs tsql = SQLTransformer.transformWithInOut(sql);
        List<ComplexArg> args = tsql.getArgs();
        Assert.assertEquals(args.size(), correctComplexArgs.size(), "Must have all args.");
        for (int i = 0; i < correctComplexArgs.size(); i++) {
            Assert.assertEquals(args.get(i).getGetter(), correctComplexArgs.get(i).getGetter());
            log.info("getter: {}, expected getter: {}", args.get(i).getGetter(), correctComplexArgs.get(i).getGetter());
            Assert.assertEquals(args.get(i).getSetter(), correctComplexArgs.get(i).getSetter());
            log.info("setter: {}, expected setter: {}", args.get(i).getSetter(), correctComplexArgs.get(i).getSetter());
        }
    }

    @Test
    public void testComplex2() {
        List<ComplexArg> correctComplexArgs = new ArrayList<>();
        correctComplexArgs.add(new ComplexArg(null, "setUpperString"));
        correctComplexArgs.add(new ComplexArg("getLowerString", null));

        String sql = ResourceUtil.slurpFileFromClasspath("sql/upper.sql");
        ParsedSQLWithComplexArgs tsql = SQLTransformer.transformWithInOut(sql);
        List<ComplexArg> args = tsql.getArgs();
        Assert.assertEquals(args.size(), correctComplexArgs.size(), "Must have all args.");
        for (int i = 0; i < correctComplexArgs.size(); i++) {
            log.info("correct: {} found: {}", correctComplexArgs.get(i), args.get(i));
            Assert.assertEquals(args.get(i).getGetter(), correctComplexArgs.get(i).getGetter());
            log.info("getter: {}, expected getter: {}", args.get(i).getGetter(), correctComplexArgs.get(i).getGetter());
            Assert.assertEquals(args.get(i).getSetter(), correctComplexArgs.get(i).getSetter());
            log.info("setter: {}, expected setter: {}", args.get(i).getSetter(), correctComplexArgs.get(i).getSetter());
        }
    }

}
