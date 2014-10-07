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
package com.manniwood.cl4pg.test;

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

import com.manniwood.cl4pg.PgSession;
import com.manniwood.cl4pg.PgSessionPool;
import com.manniwood.cl4pg.PgSimpleDataSourceAdapter;
import com.manniwood.cl4pg.commands.CopyFileIn;
import com.manniwood.cl4pg.commands.CopyFileOut;
import com.manniwood.cl4pg.commands.DDL;
import com.manniwood.cl4pg.commands.Insert;
import com.manniwood.cl4pg.commands.Select;
import com.manniwood.cl4pg.resultsethandlers.GuessScalarListHandler;
import com.manniwood.cl4pg.test.etc.ImmutableUser;
import com.manniwood.cl4pg.test.exceptionmappers.TestExceptionConverter;

/**
 * Please note that these tests must be run serially, and not all at once.
 * Although they depend as little as possible on state in the database, it is
 * very convenient to have them all use the same db session; so they are all run
 * one after the other so that they don't all trip over each other.
 *
 * @author mwood
 *
 */
public class PgSessionCopyTest {

    private PgSession pgSession;

    @BeforeClass
    public void init() throws IOException {

        Files.deleteIfExists(Paths.get(PgSessionTest.TEST_COPY_FILE));

        PgSimpleDataSourceAdapter adapter = PgSimpleDataSourceAdapter.configure()
                .exceptionConverter(new TestExceptionConverter())
                .done();

        PgSessionPool pool = new PgSessionPool(adapter);

        pgSession = pool.getSession();

        pgSession.run(DDL.config().file("sql/create_temp_users_table.sql").done());
        pgSession.run(DDL.config().file("sql/create_temp_dup_users_table.sql").done());
        pgSession.commit();

        List<ImmutableUser> usersToLoad = new ArrayList<>();
        usersToLoad.add(new ImmutableUser(UUID.fromString(PgSessionTest.ID_1),
                                          PgSessionTest.USERNAME_1,
                                          PgSessionTest.PASSWORD_1,
                                          PgSessionTest.EMPLOYEE_ID_1));
        usersToLoad.add(new ImmutableUser(UUID.fromString(PgSessionTest.ID_2),
                                          PgSessionTest.USERNAME_2,
                                          PgSessionTest.PASSWORD_2,
                                          PgSessionTest.EMPLOYEE_ID_2));
        usersToLoad.add(new ImmutableUser(UUID.fromString(PgSessionTest.ID_3),
                                          PgSessionTest.USERNAME_3,
                                          PgSessionTest.PASSWORD_3,
                                          PgSessionTest.EMPLOYEE_ID_3));

        for (ImmutableUser u : usersToLoad) {
            pgSession.run(Insert.<ImmutableUser> usingBeanArg()
                    .file("sql/insert_user.sql")
                    .arg(u)
                    .done());
        }
        pgSession.commit();
    }

    @AfterClass
    public void tearDown() {
        pgSession.close();
    }

    @Test(priority = 0)
    public void testCopy() {
        pgSession.run(CopyFileOut.config()
                .copyFile(PgSessionTest.TEST_COPY_FILE)
                .sql("copy users to stdout")
                .done());
        // can safely roll back, because file has already been created
        pgSession.rollback();

        pgSession.run(CopyFileIn.config()
                .copyFile(PgSessionTest.TEST_COPY_FILE)
                .sql("copy dup_users from stdin")
                .done());
        pgSession.commit();

        // Let's use sql to do the checking for us
        GuessScalarListHandler<Long> handler = new GuessScalarListHandler<Long>();
        pgSession.run(Select.usingVariadicArgs()
                .sql("select count(*) from (select * from users except select * from dup_users) as q")
                .resultSetHandler(handler)
                .done());
        Long count = handler.getList().get(0);
        Assert.assertEquals(count.longValue(),
                            0L,
                            "User tables must be the same after copy");

    }

}
