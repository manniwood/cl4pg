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
package com.manniwood.mpjw;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.manniwood.mpjw.test.etc.ImmutableUser;
import com.manniwood.mpjw.test.etc.User;

public class PGSessionTest {

    public static final String TEST_PASSWORD = "passwd";
    public static final String TEST_USERNAME = "Hubert";
    public static final int TEST_EMPLOYEE_ID = 13;
    public static final String TEST_ID = "99999999-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String ANOTHER_TEST_ID = "88888888-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String THIRD_PASSWORD = "blarg";
    public static final String THIRD_USERNAME = "Manni";
    public static final int THIRD_EMPLOYEE_ID = 12;
    public static final String THIRD_ID = "77777777-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String UPDATED_THIRD_PASSWORD = "updated blarg";
    public static final String UPDATED_THIRD_USERNAME = "Updated Manni";
    public static final int UPDATED_THIRD_EMPLOYEE_ID = 89;


    private PGSession pgSession;

    @BeforeClass
    public void init() {
        pgSession = new PGSession();
        pgSession.ddl("@sql/create_temp_users_table.sql");
        pgSession.commit();
    }

    @Test(priority=0)
    public void testInsertAndSelectOneBare() {


        User insertUser = new User();
        insertUser.setEmployeeId(TEST_EMPLOYEE_ID);
        insertUser.setId(UUID.fromString(TEST_ID));
        insertUser.setName(TEST_USERNAME);
        insertUser.setPassword(TEST_PASSWORD);
        pgSession.insert("@sql/insert_user.sql", insertUser);
        pgSession.commit();

        User u = pgSession.selectOneV("@sql/select_user.sql", User.class, BeanBuildStyle.GUESS_SETTERS, UUID.fromString(TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(u.getName(), TEST_USERNAME);
        Assert.assertEquals(u.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(u.getEmployeeId(), TEST_EMPLOYEE_ID);

        // When constructors are guessed, primitive types are never
        // guessed, only wrapper types; TODO: document this
        ImmutableUser iu = pgSession.selectOneV("@sql/select_user.sql", ImmutableUser.class, BeanBuildStyle.GUESS_CONSTRUCTOR, UUID.fromString(TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(iu.getName(), TEST_USERNAME);
        Assert.assertEquals(iu.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu.getEmployeeId(), TEST_EMPLOYEE_ID);

        ImmutableUser iu2 = pgSession.selectOneV("@sql/select_immutable_user.sql", ImmutableUser.class, BeanBuildStyle.SPECIFY_CONSTRUCTOR, UUID.fromString(TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(iu2.getName(), TEST_USERNAME);
        Assert.assertEquals(iu2.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu2.getEmployeeId(), TEST_EMPLOYEE_ID);
    }

    @Test(priority=1)
    public void testNulls() {

        User anotherUser = new User();
        anotherUser.setId(UUID.fromString(ANOTHER_TEST_ID));
        // leave other fields null
        pgSession.insert("@sql/insert_user.sql", anotherUser);
        pgSession.commit();

        User u = pgSession.selectOneV("@sql/select_user.sql", User.class, BeanBuildStyle.GUESS_SETTERS, UUID.fromString(ANOTHER_TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(u.getId(), UUID.fromString(ANOTHER_TEST_ID));
        Assert.assertNull(u.getPassword(), "Should be null");
        Assert.assertNull(u.getName(), "Should be null");
        Assert.assertEquals(u.getEmployeeId(), 0, "Should be 0");
    }

    // XXX START HERE: then write and test
    // 1) select that uses setters to set all values, using explicit setter names
    // 2) select that returns more than one row;
    // 3) select that returns just one element.
    // 4) More type converters
    // 5) manual conversion, for complexity and performance reasons
    // 6) row listener that can be fed to select methods
    // 7) find and document that JVM setting that makes java turn
    // reflection calls into compiled code faster (instead of waiting
    // for the default number of invocations).
    // 8) selectReport that returns map of string:colname string:value
    // for use in quick reporting displays where all values
    // would end up being cast to string anyway.

    @Test(priority=2)
    public void testDelete() {
        User user = new User();
        user.setEmployeeId(THIRD_EMPLOYEE_ID);
        user.setId(UUID.fromString(THIRD_ID));
        user.setName(THIRD_USERNAME);
        user.setPassword(THIRD_PASSWORD);
        pgSession.insert("@sql/insert_user.sql", user);
        pgSession.commit();

        User foundUser = pgSession.selectOneV("@sql/select_user.sql", User.class, BeanBuildStyle.GUESS_SETTERS, UUID.fromString(THIRD_ID));
        pgSession.rollback();

        Assert.assertNotNull(foundUser, "User must be found.");

        int numberDeleted = pgSession.deleteV("@sql/delete_user.sql", UUID.fromString(THIRD_ID));
        pgSession.commit();
        Assert.assertEquals(numberDeleted, 1, "One user must be deleted.");

        foundUser = pgSession.selectOneV("@sql/select_user.sql", User.class, BeanBuildStyle.GUESS_SETTERS, UUID.fromString(THIRD_ID));
        pgSession.rollback();
        Assert.assertNull(foundUser, "User must be found.");
    }

    @Test(priority=3)
    public void testUpdate() {
        User user = new User();
        user.setId(UUID.fromString(THIRD_ID));
        user.setEmployeeId(THIRD_EMPLOYEE_ID);
        user.setName(THIRD_USERNAME);
        user.setPassword(THIRD_PASSWORD);
        pgSession.insert("@sql/insert_user.sql", user);
        pgSession.commit();

        user.setEmployeeId(UPDATED_THIRD_EMPLOYEE_ID);
        user.setName(UPDATED_THIRD_USERNAME);
        user.setPassword(UPDATED_THIRD_PASSWORD);
        int numberUpdated = pgSession.update("@sql/update_user.sql", user);
        pgSession.commit();

        User foundUser = pgSession.selectOneV("@sql/select_user.sql", User.class, BeanBuildStyle.GUESS_SETTERS, UUID.fromString(THIRD_ID));
        pgSession.rollback();

        Assert.assertNotNull(foundUser, "User must be found.");

        Assert.assertEquals(numberUpdated, 1, "One user must be updated.");

        Assert.assertEquals(foundUser.getId(), UUID.fromString(THIRD_ID));
        Assert.assertEquals(foundUser.getName(), UPDATED_THIRD_USERNAME);
        Assert.assertEquals(foundUser.getEmployeeId(), UPDATED_THIRD_EMPLOYEE_ID);
        Assert.assertEquals(foundUser.getPassword(), UPDATED_THIRD_PASSWORD);

    }
}
