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
package com.manniwood.cl4pg.v1.test.types;

import com.manniwood.cl4pg.v1.datasourceadapters.DataSourceAdapter;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.datasourceadapters.PgSimpleDataSourceAdapter;
import com.manniwood.cl4pg.v1.commands.DDL;
import com.manniwood.cl4pg.v1.commands.Insert;
import com.manniwood.cl4pg.v1.commands.Select;
import com.manniwood.cl4pg.v1.resultsethandlers.GuessScalarListHandler;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Please note that these tests must be run serially, and not all at once.
 * Although they depend as little as possible on state in the database, it is
 * very convenient to have them all use the same db session; so they are all run
 * one after the other so that they don't all trip over each other.
 *
 * @author mwood
 *
 */
public class IntegerSmallIntTest {
    private PgSession pgSession;
    private DataSourceAdapter adapter;

    @BeforeClass
    public void init() {

        adapter = PgSimpleDataSourceAdapter.buildFromDefaultConfFile();
        pgSession = adapter.getSession();

        pgSession.run(DDL.config().sql("create temporary table test(col smallint)").done());
        pgSession.commit();
    }

    @AfterClass
    public void tearDown() {
        pgSession.close();
        adapter.close();
    }

    /**
     * Truncate the users table before each test.
     */
    @BeforeMethod
    public void truncateTable() {
        pgSession.run(DDL.config().sql("truncate table test").done());
        pgSession.commit();
    }

    @Test(priority = 1)
    public void testValue() {

        Integer expected = 3;

        pgSession.run(Insert.usingVariadicArgs()
                .sql("insert into test (col) values (#{java.lang.Integer})")
                .args(expected)
                .done());
        pgSession.commit();

        GuessScalarListHandler<Integer> handler = new GuessScalarListHandler<Integer>();
        pgSession.run(Select.<Integer> usingVariadicArgs()
                .sql("select col from test limit 1")
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        Integer actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "scalars must match");
    }

    @Test(priority = 2)
    public void testNull() {

        Integer expected = null;

        pgSession.run(Insert.usingVariadicArgs()
                .sql("insert into test (col) values (#{java.lang.Integer})")
                .args(expected)
                .done());
        pgSession.commit();

        GuessScalarListHandler<Integer> handler = new GuessScalarListHandler<Integer>();
        pgSession.run(Select.<Integer> usingVariadicArgs()
                .sql("select col from test limit 1")
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        Integer actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "scalars must match");
    }

    @Test(priority = 3)
    public void testSpecialValue1() {

        Integer expected = 32767;

        pgSession.run(Insert.usingVariadicArgs()
                .sql("insert into test (col) values (#{java.lang.Integer})")
                .args(expected)
                .done());
        pgSession.commit();

        GuessScalarListHandler<Integer> handler = new GuessScalarListHandler<Integer>();
        pgSession.run(Select.<Integer> usingVariadicArgs()
                .sql("select col from test limit 1")
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        Integer actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "scalars must match");
    }

    @Test(priority = 4)
    public void testSpecialValu21() {

        Integer expected = -32768;

        pgSession.run(Insert.usingVariadicArgs()
                .sql("insert into test (col) values (#{java.lang.Integer})")
                .args(expected)
                .done());
        pgSession.commit();

        GuessScalarListHandler<Integer> handler = new GuessScalarListHandler<Integer>();
        pgSession.run(Select.<Integer> usingVariadicArgs()
                .sql("select col from test limit 1")
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        Integer actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "scalars must match");
    }

}
