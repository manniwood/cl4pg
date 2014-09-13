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
package com.manniwood.pg4j.v1.test;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.manniwood.mpjw.test.etc.ImmutableUser;
import com.manniwood.mpjw.test.etc.User;
import com.manniwood.mpjw.test.etc.Users;
import com.manniwood.pg4j.v1.PgSession;
import com.manniwood.pg4j.v1.argsetters.SimpleBeanArgSetter;
import com.manniwood.pg4j.v1.argsetters.SimpleVariadicArgSetter;
import com.manniwood.pg4j.v1.commands.DDL;
import com.manniwood.pg4j.v1.commands.Insert;
import com.manniwood.pg4j.v1.commands.Select;
import com.manniwood.pg4j.v1.resultsethandlers.ExplicitConstructorListHandler;
import com.manniwood.pg4j.v1.resultsethandlers.ExplicitSettersListHandler;
import com.manniwood.pg4j.v1.resultsethandlers.GuessConstructorListHandler;
import com.manniwood.pg4j.v1.resultsethandlers.GuessSettersListHandler;
import com.manniwood.pg4j.v1.test.exceptionmappers.TestExceptionConverter;

/**
 * Please note that these tests must be run serially, and not all at once.
 * Although they depend as little as possible on state in the database, it is
 * very convenient to have them all use the same db session; so they are all run
 * one after the other so that they don't all trip over each other.
 *
 * @author mwood
 *
 */
public class PgSessionSelectTest {
    private final static Logger log = LoggerFactory.getLogger(PgSessionSelectTest.class);

    public static final String TEST_COPY_FILE = "/tmp/users.copy";

    public static final String TEST_PASSWORD = "passwd";
    public static final String TEST_USERNAME = "Hubert";
    public static final int TEST_EMPLOYEE_ID = 13;
    public static final String TEST_ID = "99999999-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String USER_WITH_NULLS_TEST_ID = "88888888-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String THIRD_PASSWORD = "blarg";
    public static final String THIRD_USERNAME = "Manni";
    public static final int THIRD_EMPLOYEE_ID = 12;
    public static final String THIRD_ID = "77777777-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String UPDATED_THIRD_PASSWORD = "updated blarg";
    public static final String UPDATED_THIRD_USERNAME = "Updated Manni";
    public static final int UPDATED_THIRD_EMPLOYEE_ID = 89;

    public static final String ID_1 = "11111111-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String ID_2 = "22222222-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String ID_3 = "33333333-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String USERNAME_1 = "user one";
    public static final String USERNAME_2 = "user two";
    public static final String USERNAME_3 = "user three";
    public static final String PASSWORD_1 = "password one";
    public static final String PASSWORD_2 = "password two";
    public static final String PASSWORD_3 = "password three";
    public static final int EMPLOYEE_ID_1 = 1;
    public static final int EMPLOYEE_ID_2 = 2;
    public static final int EMPLOYEE_ID_3 = 3;

    private PgSession pgSession;
    private static final User expected = createExpectedUser();
    private static final ImmutableUser expectedImmutable = createExpectedImmutableUser();
    private static final User userWithNulls = createUserWithNulls();

    private static User createExpectedUser() {
        User expected;
        expected = new User();
        expected.setEmployeeId(TEST_EMPLOYEE_ID);
        expected.setId(UUID.fromString(TEST_ID));
        expected.setName(TEST_USERNAME);
        expected.setPassword(TEST_PASSWORD);
        return expected;
    }

    private static ImmutableUser createExpectedImmutableUser() {
        return new ImmutableUser(
                                 UUID.fromString(TEST_ID),
                                 TEST_USERNAME,
                                 TEST_PASSWORD,
                                 TEST_EMPLOYEE_ID);
    }

    private static User createUserWithNulls() {
        User expected;
        expected = new User();
        expected.setId(UUID.fromString(USER_WITH_NULLS_TEST_ID));
        return expected;
    }

    @BeforeClass
    public void init() {
        pgSession = PgSession.configure()
                .exceptionConverter(new TestExceptionConverter())
                .done();
        pgSession.run(DDL.config().file("sql/create_temp_users_table.sql").done());
        pgSession.commit();

        pgSession.run(Insert.usingVariadicArgs()
                .file("sql/insert_user_variadic.sql")
                .argSetter(new SimpleVariadicArgSetter())
                .args(expected.getId(), expected.getName(), expected.getPassword(), expected.getEmployeeId())
                .done());
        pgSession.run(Insert.<User> usingBeanArg()
                .file("sql/insert_user.sql")
                .argSetter(new SimpleBeanArgSetter<User>())
                .arg(userWithNulls)
                .done());
        pgSession.commit();
    }

    @Test(priority = 0)
    public void testGuessSettersListHandler() {
        GuessSettersListHandler<User> handler = new GuessSettersListHandler<User>(User.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_guess_setters.sql")
                .argSetter(new SimpleVariadicArgSetter())
                .args(UUID.fromString(TEST_ID))
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
                .argSetter(new SimpleVariadicArgSetter())
                .args(UUID.fromString(TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "users must match");
    }

    @Test(priority = 2)
    public void testGuessConstructorListHandler() {
        GuessConstructorListHandler<ImmutableUser> handler = new GuessConstructorListHandler<ImmutableUser>(ImmutableUser.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_guess_setters.sql")
                .argSetter(new SimpleVariadicArgSetter())
                .args(UUID.fromString(TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        ImmutableUser actualImmutable = handler.getList().get(0);

        Assert.assertTrue(Users.equals(actualImmutable, expected), "users must match");
    }

    @Test(priority = 3)
    public void testExplicitConstructorListHandler() {
        ExplicitConstructorListHandler<ImmutableUser> handler = new ExplicitConstructorListHandler<ImmutableUser>(ImmutableUser.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_use_constructor.sql")
                .argSetter(new SimpleVariadicArgSetter())
                .args(UUID.fromString(TEST_ID))
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
                .argSetter(new SimpleBeanArgSetter<User>())
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
                .argSetter(new SimpleBeanArgSetter<User>())
                .arg(expected)
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        Assert.assertEquals(actual, expected, "users must match");
    }

    @Test(priority = 6)
    public void testGuessConstructorListHandlerBeanArg() {
        GuessConstructorListHandler<ImmutableUser> handler = new GuessConstructorListHandler<ImmutableUser>(ImmutableUser.class);
        pgSession.run(Select.<User> usingBeanArg()
                .file("sql/select_user_guess_setters_bean_param.sql")
                .argSetter(new SimpleBeanArgSetter<User>())
                .arg(expected)
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        ImmutableUser actualImmutable = handler.getList().get(0);

        Assert.assertTrue(Users.equals(actualImmutable, expected), "users must match");
    }

    @Test(priority = 7)
    public void testExplicitConstructorListHandlerBeanArg() {
        ExplicitConstructorListHandler<ImmutableUser> handler = new ExplicitConstructorListHandler<ImmutableUser>(ImmutableUser.class);
        pgSession.run(Select.<User> usingBeanArg()
                .file("sql/select_user_use_constructor_bean_param.sql")
                .argSetter(new SimpleBeanArgSetter<User>())
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
                .argSetter(new SimpleVariadicArgSetter())
                .args(UUID.fromString(USER_WITH_NULLS_TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession.rollback();
        User actual = handler.getList().get(0);

        // XXX: document why these nulls work, and why wrapper types should
        // be used instead of primitive types when nulls are required.

        Assert.assertEquals(actual, userWithNulls, "users must match");
    }
}
