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

import java.util.List;
import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.manniwood.cl4pg.v1.DataSourceAdapter;
import com.manniwood.cl4pg.v1.PgSession;
import com.manniwood.cl4pg.v1.PgSessionPool;
import com.manniwood.cl4pg.v1.commands.Select;
import com.manniwood.cl4pg.v1.resultsethandlers.ExplicitConstructorListHandler;
import com.manniwood.cl4pg.v1.resultsethandlers.ExplicitSettersListHandler;
import com.manniwood.cl4pg.v1.resultsethandlers.GuessSettersListHandler;
import com.manniwood.cl4pg.v1.test.etc.ImmutableUser;
import com.manniwood.cl4pg.v1.test.etc.User;
import com.manniwood.cl4pg.v1.test.etc.Users;

/**
 * Please note that these tests must be run serially, and not all at once.
 * Although they depend as little as possible on state in the database, it is
 * very convenient to have them all use the same db session; so they are all run
 * one after the other so that they don't all trip over each other.
 *
 * @author mwood
 *
 */
public abstract class AbstractSelectTest {

    private PgSession pgSession;
    private PgSessionPool pool;
    private static final User expected = createExpectedUser();
    private static final User userWithNulls = createUserWithNulls();

    private static User createExpectedUser() {
        User expected;
        expected = new User();
        expected.setEmployeeId(AbstractPgSessionTest.TEST_EMPLOYEE_ID);
        expected.setId(UUID.fromString(AbstractPgSessionTest.TEST_ID));
        expected.setName(AbstractPgSessionTest.TEST_USERNAME);
        expected.setPassword(AbstractPgSessionTest.TEST_PASSWORD);
        return expected;
    }

    private static User createUserWithNulls() {
        User expected;
        expected = new User();
        expected.setId(UUID.fromString(AbstractPgSessionTest.USER_WITH_NULLS_TEST_ID));
        return expected;
    }

    @BeforeClass
    public void init() {

        DataSourceAdapter adapter = configureDataSourceAdapter();

        pool = new PgSessionPool(adapter);

        pgSession = pool.getSession();

        pgSession.ddlF("sql/create_temp_users_table.sql");
        pgSession.commit();

        pgSession.insertF("sql/insert_user_variadic.sql", expected.getId(), expected.getName(), expected.getPassword(), expected.getEmployeeId());
        pgSession.insertF(userWithNulls, "sql/insert_user.sql");
        pgSession.commit();
    }

    protected abstract DataSourceAdapter configureDataSourceAdapter();

    @AfterClass
    public void tearDown() {
        pgSession.close();
        pool.close();
    }

    @Test(priority = 0)
    public void testGuessSettersListHandler() {
        GuessSettersListHandler<User> handler = new GuessSettersListHandler<User>(User.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_guess_setters.sql")
                .args(UUID.fromString(AbstractPgSessionTest.TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "users must match");
    }

    @Test(priority = 1)
    public void testExplicitSettersListHandler() {

        ExplicitSettersListHandler<User> handler = new ExplicitSettersListHandler<User>(User.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_use_setters.sql")
                .args(UUID.fromString(AbstractPgSessionTest.TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "users must match");
    }

    @Test(priority = 2)
    public void testGuessConstructorListHandler() {
        List<ImmutableUser> users = pgSession.selectF("sql/select_user_guess_setters.sql",
                                                      ImmutableUser.class,
                                                      UUID.fromString(AbstractPgSessionTest.TEST_ID));
        pgSession.rollback();
        ImmutableUser actualImmutable = users.get(0);

        Assert.assertTrue(Users.equals(actualImmutable, expected), "users must match");
    }

    @Test(priority = 3)
    public void testExplicitConstructorListHandler() {
        ExplicitConstructorListHandler<ImmutableUser> handler = new ExplicitConstructorListHandler<ImmutableUser>(ImmutableUser.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_use_constructor.sql")
                .args(UUID.fromString(AbstractPgSessionTest.TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        ImmutableUser actualImmutable = handler.getList().get(0);

        Assert.assertTrue(Users.equals(actualImmutable, expected), "users must match");
    }

    @Test(priority = 4)
    public void testGuessSettersListHandlerBeanArg() {
        GuessSettersListHandler<User> handler = new GuessSettersListHandler<User>(User.class);
        pgSession.run(Select.<User> usingBeanArg()
                .file("sql/select_user_guess_setters_bean_param.sql")
                .arg(expected)
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "users must match");
    }

    @Test(priority = 5)
    public void testExplicitSettersListHandlerBeanArg() {

        ExplicitSettersListHandler<User> handler = new ExplicitSettersListHandler<User>(User.class);
        pgSession.run(Select.<User> usingBeanArg()
                .file("sql/select_user_use_setters_bean_param.sql")
                .arg(expected)
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "users must match");
    }

    @Test(priority = 6)
    public void testGuessConstructorListHandlerBeanArg() {
        List<ImmutableUser> users = pgSession.selectF(expected,
                                                      "sql/select_user_guess_setters_bean_param.sql",
                                                      ImmutableUser.class);
        pgSession.rollback();
        ImmutableUser actualImmutable = users.get(0);

        Assert.assertTrue(Users.equals(actualImmutable, expected), "users must match");
    }

    @Test(priority = 7)
    public void testExplicitConstructorListHandlerBeanArg() {
        ExplicitConstructorListHandler<ImmutableUser> handler = new ExplicitConstructorListHandler<ImmutableUser>(ImmutableUser.class);
        pgSession.run(Select.<User> usingBeanArg()
                .file("sql/select_user_use_constructor_bean_param.sql")
                .arg(expected)
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        ImmutableUser actualImmutable = handler.getList().get(0);

        Assert.assertTrue(Users.equals(actualImmutable, expected), "users must match");
    }

    @Test(priority = 20)
    public void testNulls() {
        GuessSettersListHandler<User> handler = new GuessSettersListHandler<User>(User.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_guess_setters.sql")
                .args(UUID.fromString(AbstractPgSessionTest.USER_WITH_NULLS_TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        // XXX: document why these nulls work, and why wrapper types should
        // be used instead of primitive types when nulls are required.

        Assert.assertEquals(actual, userWithNulls, "users must match");
    }

    // TODO: test GuessScalarListHandler
    // TODO: test ExplicitScalarListHandler
}
