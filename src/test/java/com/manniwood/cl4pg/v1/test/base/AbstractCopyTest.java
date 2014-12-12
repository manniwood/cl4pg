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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.manniwood.cl4pg.v1.DataSourceAdapter;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.PgSessionPool;
import com.manniwood.cl4pg.v1.commands.CopyFileIn;
import com.manniwood.cl4pg.v1.commands.CopyFileOut;
import com.manniwood.cl4pg.v1.commands.DDL;
import com.manniwood.cl4pg.v1.commands.Insert;
import com.manniwood.cl4pg.v1.commands.Select;
import com.manniwood.cl4pg.v1.resultsethandlers.GuessScalarListHandler;
import com.manniwood.cl4pg.v1.test.etc.ImmutableUser;

/**
 * Please note that these tests must be run serially, and not all at once.
 * Although they depend as little as possible on state in the database, it is
 * very convenient to have them all use the same db session; so they are all run
 * one after the other so that they don't all trip over each other.
 *
 * @author mwood
 *
 */
public abstract class AbstractCopyTest {

    private PgSession pgSession;
    private PgSessionPool pool;

    @BeforeClass
    public void init() throws IOException {

        Files.deleteIfExists(Paths.get(AbstractPgSessionTest.TEST_COPY_FILE));

        DataSourceAdapter adapter = configureDataSourceAdapter();

        pool = new PgSessionPool(adapter);

        pgSession = pool.getSession();

        pgSession.ddlF("sql/create_temp_users_table.sql");
        pgSession.ddlF("sql/create_temp_dup_users_table.sql");
        pgSession.commit();

        List<ImmutableUser> usersToLoad = new ArrayList<>();
        usersToLoad.add(new ImmutableUser(UUID.fromString(AbstractPgSessionTest.ID_1),
                                          AbstractPgSessionTest.USERNAME_1,
                                          AbstractPgSessionTest.PASSWORD_1,
                                          AbstractPgSessionTest.EMPLOYEE_ID_1));
        usersToLoad.add(new ImmutableUser(UUID.fromString(AbstractPgSessionTest.ID_2),
                                          AbstractPgSessionTest.USERNAME_2,
                                          AbstractPgSessionTest.PASSWORD_2,
                                          AbstractPgSessionTest.EMPLOYEE_ID_2));
        usersToLoad.add(new ImmutableUser(UUID.fromString(AbstractPgSessionTest.ID_3),
                                          AbstractPgSessionTest.USERNAME_3,
                                          AbstractPgSessionTest.PASSWORD_3,
                                          AbstractPgSessionTest.EMPLOYEE_ID_3));

        for (ImmutableUser u : usersToLoad) {
            pgSession.<ImmutableUser>insertF(u, "sql/insert_user.sql");
        }pgSession.copyOut("copy users to stdout", "/tmp/the_users_file.copy");
        pgSession.commit();
    }

    protected abstract DataSourceAdapter configureDataSourceAdapter();

    @AfterClass
    public void tearDown() {
        pgSession.close();
        pool.close();
    }

    @Test(priority = 0)
    public void testCopy() {
        pgSession.copyOut("copy users to stdout", AbstractPgSessionTest.TEST_COPY_FILE);
        // can safely roll back, because file has already been created
        pgSession.rollback();

        pgSession.copyIn("copy dup_users from stdin", AbstractPgSessionTest.TEST_COPY_FILE);
        pgSession.commit();

        // Let's use sql to do the checking for us
        Long count = pgSession.selectOneScalar("select count(*) from (select * from users except select * from dup_users) as q", Long.class);

        Assert.assertEquals(count.longValue(),
                            0L,
                            "User tables must be the same after copy");

    }

}
