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
package com.manniwood.pg4j;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.postgresql.PGNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.manniwood.mpjw.test.etc.User;
import com.manniwood.pg4j.argsetters.SimpleBeanArgSetter;
import com.manniwood.pg4j.argsetters.SimpleVariadicArgSetter;
import com.manniwood.pg4j.commands.DDL;
import com.manniwood.pg4j.commands.Insert;
import com.manniwood.pg4j.commands.Select;
import com.manniwood.pg4j.resultsethandlers.GuessSettersListHandler;

/**
 * Please note that these tests must be run serially, and not all at once.
 * Although they depend as litte as possible on state in the database, it is
 * very convenient to have them all use the same db session; so they are all run
 * one after the other so that they don't all trip over each other.
 *
 * @author mwood
 *
 */
public class PgSessionTest {
    private final static Logger log                       = LoggerFactory.getLogger(PgSession.class);

    public static final String  TEST_COPY_FILE            = "/tmp/users.copy";

    public static final String  TEST_PASSWORD             = "passwd";
    public static final String  TEST_USERNAME             = "Hubert";
    public static final int     TEST_EMPLOYEE_ID          = 13;
    public static final String  TEST_ID                   = "99999999-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String  ANOTHER_TEST_ID           = "88888888-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String  THIRD_PASSWORD            = "blarg";
    public static final String  THIRD_USERNAME            = "Manni";
    public static final int     THIRD_EMPLOYEE_ID         = 12;
    public static final String  THIRD_ID                  = "77777777-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String  UPDATED_THIRD_PASSWORD    = "updated blarg";
    public static final String  UPDATED_THIRD_USERNAME    = "Updated Manni";
    public static final int     UPDATED_THIRD_EMPLOYEE_ID = 89;

    public static final String  ID_1                      = "11111111-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String  ID_2                      = "22222222-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String  ID_3                      = "33333333-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String  USERNAME_1                = "user one";
    public static final String  USERNAME_2                = "user two";
    public static final String  USERNAME_3                = "user three";
    public static final String  PASSWORD_1                = "password one";
    public static final String  PASSWORD_2                = "password two";
    public static final String  PASSWORD_3                = "password three";
    public static final int     EMPLOYEE_ID_1             = 1;
    public static final int     EMPLOYEE_ID_2             = 2;
    public static final int     EMPLOYEE_ID_3             = 3;

    private PgSession           pgSession;

    @BeforeClass
    public void init() {
        pgSession = new PgSession();
        pgSession.run(DDL.config().file("sql/create_temp_users_table.sql").done());
        pgSession.commit();
    }

    private User createExpectedUser() {
        User expected;
        expected = new User();
        expected.setEmployeeId(TEST_EMPLOYEE_ID);
        expected.setId(UUID.fromString(TEST_ID));
        expected.setName(TEST_USERNAME);
        expected.setPassword(TEST_PASSWORD);
        return expected;
    }

    /**
     * Truncate the users table before each test.
     */
    @BeforeMethod
    public void truncateTableUsers() {
        pgSession.run(DDL.config().sql("truncate table users").done());
        pgSession.commit();
    }

    @Test(priority = 0)
    public void testInsertAndSelectOneVariadic() {

        User expected = createExpectedUser();
        pgSession.run(Insert.usingVariadicArgs()
                .file("sql/insert_user_variadic.sql")
                .argSetter(new SimpleVariadicArgSetter())
                .args(expected.getId(), expected.getName(), expected.getPassword(), expected.getEmployeeId())
                .done());
        pgSession.commit();

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
    public void testInsertAndSelectOneB() {

        User expected = createExpectedUser();
        pgSession.run(Insert.<User> usingBeanArg().file("sql/insert_user.sql").argSetter(new SimpleBeanArgSetter<User>()).arg(expected).done());
        pgSession.commit();

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

    @Test(priority = 2)
    public void testListenNotify() {
        // According to Pg docs, "Except for dropping later instances of
        // duplicate notifications,
        // NOTIFY guarantees that notifications from the same transaction get
        // delivered in the order they were sent."
        // So, an ordered list should be a good thing to test against.
        List<String> expected = new ArrayList<>();
        expected.add("bar");
        expected.add("baz");
        expected.add("bal");

        PgSession pgSession2 = new PgSession();

        pgSession2.pgListen("foo");
        pgSession2.commit();

        for (String s : expected) {
            pgSession.pgNotify("foo", s);
        }
        pgSession.commit();

        // Ensure you can do other queries on the listening session and not lose
        // the notifications just because you have run a query and committed,
        // but
        // not yet retrieved the notifications.
        GuessSettersListHandler<User> handler = new GuessSettersListHandler<User>(User.class);
        pgSession.run(Select.usingVariadicArgs()
                .file("sql/select_user_guess_setters.sql")
                .argSetter(new SimpleVariadicArgSetter())
                .args(UUID.fromString(TEST_ID))
                .resultSetHandler(handler)
                .done());
        pgSession2.commit();

        PGNotification[] notifications = pgSession2.getNotifications();
        pgSession2.commit();

        List<String> actual = new ArrayList<>();
        for (PGNotification notification : notifications) {
            log.info("notification name {}, parameter: {}, pid: {}", notification.getName(), notification.getParameter(), notification.getPID());
            actual.add(notification.getParameter());
        }
        Assert.assertEquals(actual, expected, "Notifications must all be recieved, in the same order");
    }

    @Test(priority = 3)
    public void testExceptions() {
        pgSession.run(DDL.config().sql("drop table users").done());
        pgSession.run(DDL.config().file("sql/create_temp_constrained_users_table.sql").done());
        pgSession.commit();

        User expected = createExpectedUser();
        pgSession.run(Insert.<User> usingBeanArg().file("sql/insert_user.sql").argSetter(new SimpleBeanArgSetter<User>()).arg(expected).done());
        pgSession.commit();
        // START HERE; this will throw an exception, so this is a good
        // place to figure out exception handling
        try {
            pgSession.run(Insert.<User> usingBeanArg().file("sql/insert_user.sql").argSetter(new SimpleBeanArgSetter<User>()).arg(expected).done());
            pgSession.commit();
        } catch (Pg4jPgSqlException e) {
            log.info("Cannot insert user twice!");
            log.info("Exception: " + e.toString(), e);
        }
        // START HERE: so we could obviously make an exceptionmapper
        // that uses the attributes of ServerErrorMessage. Our
        // example can use table and constraint ant sqlState
        // matching to throw a DuplicateUserInsert exception.
        // Exception: Pg4jPgSqlException [
        // sqlState=23505, serverMessage=duplicate key value violates unique
        // constraint "users_pk",
        // severity=ERROR,
        // detail=Key (id)=(99999999-a4fa-49fc-b6b4-62eca118fbf7) already
        // exists.,
        // hint=null, position=0, where=null,
        // schema=pg_temp_2,
        // table=users,
        // column=null, dataType=null,
        // constraint=users_pk, file=nbtinsert.c, line=398,
        // routine=_bt_check_unique, internalQuery=null, internalPosition=0]

        // put the original tmp users table back
        pgSession.run(DDL.config().sql("drop table users").done());
        pgSession.run(DDL.config().file("sql/create_temp_users_table.sql").done());
        pgSession.commit();

    }

}
