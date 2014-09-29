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

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.manniwood.cl4pg.PgSession;
import com.manniwood.cl4pg.commands.DDL;
import com.manniwood.cl4pg.commands.Insert;
import com.manniwood.cl4pg.commands.Select;
import com.manniwood.cl4pg.commands.Update;
import com.manniwood.cl4pg.commands.UpdateB;
import com.manniwood.cl4pg.commands.UpdateV;
import com.manniwood.cl4pg.resultsethandlers.ExplicitSettersListHandler;
import com.manniwood.cl4pg.test.etc.User;
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
public class PgSessionUpdateTest {

    private PgSession pgSession;

    private User expectedUser;
    private User updatedUser;

    @BeforeClass
    public void init() {
        pgSession = PgSession.configure()
                .exceptionConverter(new TestExceptionConverter())
                .done();
        pgSession.run(DDL.config().file("sql/create_temp_users_table.sql").done());
        pgSession.commit();

        expectedUser = new User();
        expectedUser.setId(UUID.fromString(PgSessionTest.THIRD_ID));
        expectedUser.setEmployeeId(PgSessionTest.THIRD_EMPLOYEE_ID);
        expectedUser.setName(PgSessionTest.THIRD_USERNAME);
        expectedUser.setPassword(PgSessionTest.THIRD_PASSWORD);

        updatedUser = new User();
        updatedUser.setId(UUID.fromString(PgSessionTest.THIRD_ID));
        updatedUser.setEmployeeId(PgSessionTest.UPDATED_THIRD_EMPLOYEE_ID);
        updatedUser.setName(PgSessionTest.UPDATED_THIRD_USERNAME);
        updatedUser.setPassword(PgSessionTest.UPDATED_THIRD_PASSWORD);
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
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_use_setters.sql")
                .args(UUID.fromString(PgSessionTest.THIRD_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actualUser = handler.getList().get(0);

        Assert.assertNotNull(actualUser, "User must be found.");
    }

    @AfterMethod
    public void assertUserIsUpdated() {
        ExplicitSettersListHandler<User> handler = new ExplicitSettersListHandler<User>(User.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_use_setters.sql")
                .args(UUID.fromString(PgSessionTest.THIRD_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actualUser = handler.getList().get(0);

        Assert.assertEquals(actualUser.getId(), UUID.fromString(PgSessionTest.THIRD_ID));
        Assert.assertEquals(actualUser.getName(), PgSessionTest.UPDATED_THIRD_USERNAME);
        Assert.assertEquals(actualUser.getEmployeeId(), PgSessionTest.UPDATED_THIRD_EMPLOYEE_ID);
        Assert.assertEquals(actualUser.getPassword(), PgSessionTest.UPDATED_THIRD_PASSWORD);
    }

    @Test(priority = 0)
    public void testUpdateB() {

        UpdateB<User> del = Update.<User> usingBeanArg()
                .file("sql/update_user.sql")
                .arg(updatedUser)
                .done();
        pgSession.run(del);
        pgSession.commit();
        int numberUpdated = del.getNumberOfRowsAffected();

        Assert.assertEquals(numberUpdated, 1, "One user must be updated.");
    }

    @Test(priority = 1)
    public void testUpdateV() {

        UpdateV del = Update.usingVariadicArgs()
                .file("sql/update_user_variadic.sql")
                .args(updatedUser.getName(), updatedUser.getPassword(), updatedUser.getEmployeeId(), UUID.fromString(PgSessionTest.THIRD_ID))
                .done();
        pgSession.run(del);
        pgSession.commit();
        int numberUpdated = del.getNumberOfRowsAffected();

        Assert.assertEquals(numberUpdated, 1, "One user must be deleted.");
    }

    // TODO: implement updateReturning
}
