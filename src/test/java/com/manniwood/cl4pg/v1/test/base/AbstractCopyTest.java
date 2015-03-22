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

import com.manniwood.cl4pg.v1.datasourceadapters.DataSourceAdapter;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.test.etc.ImmutableUser;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
    private DataSourceAdapter adapter;

    @BeforeClass
    public void init() throws IOException {

        Files.deleteIfExists(Paths.get(AbstractSetApplicationNameTest.TEST_COPY_FILE));

        adapter = configureDataSourceAdapter();

        pgSession = adapter.getSession();

        pgSession.ddl("sql/create_temp_users_table.sql");
        pgSession.ddl("sql/create_temp_dup_users_table.sql");
        pgSession.commit();

        List<ImmutableUser> usersToLoad = new ArrayList<>();
        usersToLoad.add(new ImmutableUser(UUID.fromString(AbstractSetApplicationNameTest.ID_1),
                                          AbstractSetApplicationNameTest.USERNAME_1,
                                          AbstractSetApplicationNameTest.PASSWORD_1,
                                          AbstractSetApplicationNameTest.EMPLOYEE_ID_1));
        usersToLoad.add(new ImmutableUser(UUID.fromString(AbstractSetApplicationNameTest.ID_2),
                                          AbstractSetApplicationNameTest.USERNAME_2,
                                          AbstractSetApplicationNameTest.PASSWORD_2,
                                          AbstractSetApplicationNameTest.EMPLOYEE_ID_2));
        usersToLoad.add(new ImmutableUser(UUID.fromString(AbstractSetApplicationNameTest.ID_3),
                                          AbstractSetApplicationNameTest.USERNAME_3,
                                          AbstractSetApplicationNameTest.PASSWORD_3,
                                          AbstractSetApplicationNameTest.EMPLOYEE_ID_3));

        for (ImmutableUser u : usersToLoad) {
            pgSession.<ImmutableUser>insert(u, "sql/insert_user.sql");
        }pgSession.qCopyOut("copy users to stdout", "/tmp/the_users_file.copy");
        pgSession.commit();
    }

    protected abstract DataSourceAdapter configureDataSourceAdapter();

    @AfterClass
    public void tearDown() {
        pgSession.close();
        adapter.close();
    }

    @Test(priority = 0)
    public void testCopy() {
        pgSession.qCopyOut("copy users to stdout", AbstractSetApplicationNameTest.TEST_COPY_FILE);
        // can safely roll back, because file has already been created
        pgSession.rollback();

        pgSession.qCopyIn("copy dup_users from stdin", AbstractSetApplicationNameTest.TEST_COPY_FILE);
        pgSession.commit();

        // Let's use sql to do the checking for us
        Long count = pgSession.qSelectOneScalar("select count(*) from (select * from users except select * from dup_users) as q");

        Assert.assertEquals(count.longValue(),
                            0L,
                            "User tables must be the same after copy");

    }

}
