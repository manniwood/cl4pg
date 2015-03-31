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

import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.commands.DDL;
import com.manniwood.cl4pg.v1.commands.Insert;
import com.manniwood.cl4pg.v1.commands.Select;
import com.manniwood.cl4pg.v1.datasourceadapters.DataSourceAdapter;
import com.manniwood.cl4pg.v1.resultsethandlers.ExplicitConstructorListHandler;
import com.manniwood.cl4pg.v1.resultsethandlers.ExplicitSettersListHandler;
import com.manniwood.cl4pg.v1.resultsethandlers.GuessSettersListHandler;
import com.manniwood.cl4pg.v1.test.etc.ImmutableUser;
import com.manniwood.cl4pg.v1.test.etc.User;
import com.manniwood.cl4pg.v1.test.etc.Users;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
public abstract class AbstractFluentApiTest {

    private PgSession pgSession;
    private static final User expected = createExpectedUser();
    private static final User userWithNulls = createUserWithNulls();
    private DataSourceAdapter adapter;

    private static User createExpectedUser() {
        User expected;
        expected = new User();
        expected.setEmployeeId(AbstractSetApplicationNameTest.TEST_EMPLOYEE_ID);
        expected.setId(UUID.fromString(AbstractSetApplicationNameTest.TEST_ID));
        expected.setName(AbstractSetApplicationNameTest.TEST_USERNAME);
        expected.setPassword(AbstractSetApplicationNameTest.TEST_PASSWORD);
        return expected;
    }

    private static User createUserWithNulls() {
        User expected;
        expected = new User();
        expected.setId(UUID.fromString(AbstractSetApplicationNameTest.USER_WITH_NULLS_TEST_ID));
        return expected;
    }

    @BeforeClass
    public void init() {

        adapter = configureDataSourceAdapter();

        pgSession = adapter.getSession();

        pgSession.run(DDL.config().file("sql/create_temp_users_table.sql").done());
        pgSession.commit();

        pgSession.run(Insert.usingVariadicArgs()
                .file("sql/insert_user_variadic.sql")
                .args(expected.getId(), expected.getName(), expected.getPassword(), expected.getEmployeeId())
                .done());
        pgSession.run(Insert.usingBeanArg().file("sql/insert_user.sql").arg(userWithNulls).done());
        pgSession.commit();
    }

    protected abstract DataSourceAdapter configureDataSourceAdapter();

    @AfterClass
    public void tearDown() {
        pgSession.close();
        adapter.close();
    }

    @Test(priority = 0)
    public void testGuessSettersListHandler() {
        GuessSettersListHandler<User> handler = new GuessSettersListHandler<User>(User.class);
        pgSession.run(Select.<User> usingVariadicArgs()
                .file("sql/select_user_guess_setters.sql")
                .args(UUID.fromString(AbstractSetApplicationNameTest.TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "users must match");
    }

    // NOTE that the fluent API for selecting gets used a lot in AbstractSelectTest as well.

    // TODO: test GuessScalarListHandler
    // TODO: test ExplicitScalarListHandler
}
