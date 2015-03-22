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
package com.manniwood.cl4pg.v1.test.base;

import com.manniwood.cl4pg.v1.ConfigDefaults;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.commands.DDL;
import com.manniwood.cl4pg.v1.datasourceadapters.DataSourceAdapter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public abstract class AbstractRollbackTest {
    private final static Logger log = LoggerFactory.getLogger(AbstractRollbackTest.class);


    private PgSession pgSession;
    private DataSourceAdapter adapter;

    @BeforeClass
    public void init() {

        adapter = configureDataSourceAdapter();
        pgSession = adapter.getSession();

        pgSession.ddl("sql/create_temp_users_table.sql");
        pgSession.commit();
    }

    protected abstract DataSourceAdapter configureDataSourceAdapter();

    @AfterClass
    public void tearDown() {
        pgSession.close();
        adapter.close();
    }

    /**
     * Truncate the users table before each test.
     */
    @BeforeMethod
    public void truncateTableUsers() {
        pgSession.qDdl("truncate table users");
        pgSession.commit();
    }

    @Test(priority = 4)
    public void testRollback() {
        Cl4pgException expectedException = null;
        try {
            pgSession.run(DDL.config().sql("select flurby").done());
        } catch (Cl4pgException e) {
            expectedException = e;
        }
        Assert.assertNotNull(expectedException);
        log.info("test correctly caught following exception", expectedException);

        /*
         * Because of successful rollback, this next select should work, and we
         * should NOT get this following exception: ERROR: 25P02: current
         * transaction is aborted, commands ignored until end of transaction
         * block
         */
        Integer count = pgSession.qSelectOneScalar("select 1");
        Assert.assertEquals(count.intValue(),
                            1,
                            "Statement needs to return 1");
    }

}
