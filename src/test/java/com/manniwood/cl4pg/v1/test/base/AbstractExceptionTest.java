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
import com.manniwood.cl4pg.v1.test.etc.User;
import com.manniwood.cl4pg.v1.test.exceptions.UserAlreadyExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
public abstract class AbstractExceptionTest {
    private final static Logger log = LoggerFactory.getLogger(AbstractExceptionTest.class);

    public static final String TEST_COPY_FILE = "/tmp/users.copy";

    public static final String TEST_PASSWORD = "passwd";
    public static final String TEST_USERNAME = "Hubert";
    public static final Integer TEST_EMPLOYEE_ID = 13;
    public static final String TEST_ID = "99999999-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String USER_WITH_NULLS_TEST_ID = "88888888-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String THIRD_PASSWORD = "blarg";
    public static final String THIRD_USERNAME = "Manni";
    public static final Integer THIRD_EMPLOYEE_ID = 12;
    public static final String THIRD_ID = "77777777-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String UPDATED_THIRD_PASSWORD = "updated blarg";
    public static final String UPDATED_THIRD_USERNAME = "Updated Manni";
    public static final Integer UPDATED_THIRD_EMPLOYEE_ID = 89;

    public static final String ID_1 = "11111111-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String ID_2 = "22222222-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String ID_3 = "33333333-a4fa-49fc-b6b4-62eca118fbf7";
    public static final String USERNAME_1 = "user one";
    public static final String USERNAME_2 = "user two";
    public static final String USERNAME_3 = "user three";
    public static final String PASSWORD_1 = "password one";
    public static final String PASSWORD_2 = "password two";
    public static final String PASSWORD_3 = "password three";
    public static final Integer EMPLOYEE_ID_1 = 1;
    public static final Integer EMPLOYEE_ID_2 = 2;
    public static final Integer EMPLOYEE_ID_3 = 3;

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
        pgSession.qDdl("truncate table users");
        pgSession.commit();
    }

    @Test(priority = 3)
    public void testExceptions() {
        pgSession.qDdl("drop table users");
        pgSession.ddl("sql/create_temp_constrained_users_table.sql");
        pgSession.commit();

        User expected = createExpectedUser();
        pgSession.insert(expected, "sql/insert_user.sql");
        pgSession.commit();
        boolean correctlyCaughtException = false;
        try {
            pgSession.insert(expected, "sql/insert_user.sql");
            pgSession.commit();
        } catch (UserAlreadyExistsException e) {
            log.info("Cannot insert user twice!");
            log.info("Exception: " + e.toString(), e);
            correctlyCaughtException = true;
        }

        // put the original tmp users table back
        pgSession.qDdl("drop table users");
        pgSession.ddl("sql/create_temp_users_table.sql");
        pgSession.commit();
        Assert.assertTrue(correctlyCaughtException, "Had to catch custom exception");
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

    @Test(priority = 5)
    public void testApplicationNameIsSet() {
        String actualAppName = pgSession.qSelectOne(
                "select application_name from pg_stat_activity where application_name = #{java.lang.String}",
                String.class,
                ConfigDefaults.DEFAULT_APP_NAME);
        Assert.assertEquals(actualAppName, ConfigDefaults.DEFAULT_APP_NAME, "App name needs to be " + ConfigDefaults.DEFAULT_APP_NAME);
    }
}
