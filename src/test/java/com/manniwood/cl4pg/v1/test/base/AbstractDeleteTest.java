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

import com.manniwood.cl4pg.v1.DataSourceAdapter;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.commands.*;
import com.manniwood.cl4pg.v1.resultsethandlers.ExplicitSettersListHandler;
import com.manniwood.cl4pg.v1.test.etc.User;
import org.testng.Assert;
import org.testng.annotations.*;

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
public abstract class AbstractDeleteTest {

    private PgSession pgSession;
    private DataSourceAdapter adapter;

    private User expectedUser;

    @BeforeClass
    public void init() {

        adapter = configureDataSourceAdapter();

        pgSession = adapter.getSession();

        pgSession.run(DDL.config().file("sql/create_temp_users_table.sql").done());
        pgSession.commit();

        expectedUser = new User();
        expectedUser.setEmployeeId(AbstractPgSessionTest.THIRD_EMPLOYEE_ID);
        expectedUser.setId(UUID.fromString(AbstractPgSessionTest.THIRD_ID));
        expectedUser.setName(AbstractPgSessionTest.THIRD_USERNAME);
        expectedUser.setPassword(AbstractPgSessionTest.THIRD_PASSWORD);
    }

    protected abstract DataSourceAdapter configureDataSourceAdapter();

    @AfterClass
    public void tearDown() {
        pgSession.close();
        adapter.close();
    }

    @BeforeMethod
    public void resetTable() {
        pgSession.run(DDL.config().sql("truncate table users").done());
        pgSession.commit();

        pgSession.run(Insert.<User> usingBeanArg()
                .file("sql/insert_user.sql")
                .arg(expectedUser)
                .done());
        pgSession.commit();

        ExplicitSettersListHandler<User> handler = new ExplicitSettersListHandler<User>(User.class);
        pgSession.run(Select.<User> usingVariadicArgs()
                .file("sql/select_user_use_setters.sql")
                .args(UUID.fromString(AbstractPgSessionTest.THIRD_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actualUser = handler.getList().get(0);

        Assert.assertNotNull(actualUser, "User must be found.");
    }

    @AfterMethod
    public void assertUserIsDeleted() {
        ExplicitSettersListHandler<User> handler = new ExplicitSettersListHandler<User>(User.class);
        pgSession.run(Select.<User> usingVariadicArgs()
                .file("sql/select_user_use_setters.sql")
                .args(UUID.fromString(AbstractPgSessionTest.THIRD_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        List<User> foundUsers = handler.getList();
        pgSession.rollback();

        Assert.assertTrue(foundUsers.isEmpty(), "Deleted user must not be found.");
    }

    @Test(priority = 0)
    public void testDeleteB() {

        UpdateB<User> del = Update.<User> usingBeanArg()
                .file("sql/delete_user_bean.sql")
                .arg(expectedUser)
                .done();
        pgSession.run(del);
        pgSession.commit();
        int numberDeleted = del.getNumberOfRowsAffected();

        Assert.assertEquals(numberDeleted, 1, "One user must be deleted.");
    }

    @Test(priority = 1)
    public void testDeleteV() {

        UpdateV del = Update.usingVariadicArgs()
                .file("sql/delete_user.sql")
                .args(UUID.fromString(AbstractPgSessionTest.THIRD_ID))
                .done();
        pgSession.run(del);
        pgSession.commit();
        int numberDeleted = del.getNumberOfRowsAffected();

        Assert.assertEquals(numberDeleted, 1, "One user must be deleted.");
    }

    // TODO: implement/test deleteReturning
}
