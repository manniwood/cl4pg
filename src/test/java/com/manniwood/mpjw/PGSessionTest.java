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

import java.util.ArrayList;
import java.util.List;
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

    private PGSession pgSession;

    @BeforeClass
    public void init() {
        pgSession = new PGSession();
        pgSession.ddl("@sql/create_temp_users_table.sql");
        pgSession.commit();
    }

    @Test(priority=0)
    public void testInsertAndSelectOne() {


        User insertUser = new User();
        insertUser.setEmployeeId(TEST_EMPLOYEE_ID);
        insertUser.setId(UUID.fromString(TEST_ID));
        insertUser.setName(TEST_USERNAME);
        insertUser.setPassword(TEST_PASSWORD);
        pgSession.insertB("@sql/insert_user.sql", insertUser);
        pgSession.commit();

        User u = pgSession.selectOneVGuessSetters("@sql/select_user_guess_setters.sql", User.class, UUID.fromString(TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(u.getName(), TEST_USERNAME);
        Assert.assertEquals(u.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(u.getEmployeeId(), TEST_EMPLOYEE_ID);

        // When constructors are guessed, primitive types are never
        // guessed, only wrapper types; TODO: document this
        ImmutableUser iu = pgSession.selectOneVGuessConstructor("@sql/select_user_guess_setters.sql", ImmutableUser.class, UUID.fromString(TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(iu.getName(), TEST_USERNAME);
        Assert.assertEquals(iu.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu.getEmployeeId(), TEST_EMPLOYEE_ID);

        ImmutableUser iu2 = pgSession.selectOneVSpecifyConstructor("@sql/select_user_use_constructor.sql", ImmutableUser.class, UUID.fromString(TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(iu2.getName(), TEST_USERNAME);
        Assert.assertEquals(iu2.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu2.getEmployeeId(), TEST_EMPLOYEE_ID);

        User iu3 = pgSession.selectOneVSpecifySetters("@sql/select_user_use_setters.sql", User.class, UUID.fromString(TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(iu3.getName(), TEST_USERNAME);
        Assert.assertEquals(iu3.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu3.getEmployeeId(), TEST_EMPLOYEE_ID);


        User searchParam = new User();
        searchParam.setId(UUID.fromString(TEST_ID));
        // intentionally leave other attributes of searchParam blank

        User u4 = pgSession.selectOneBGuessSetters("@sql/select_user_guess_setters_bean_param.sql", User.class, searchParam);
        pgSession.rollback();
        Assert.assertEquals(u4.getName(), TEST_USERNAME);
        Assert.assertEquals(u4.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(u4.getEmployeeId(), TEST_EMPLOYEE_ID);

        // When constructors are guessed, primitive types are never
        // guessed, only wrapper types; TODO: document this
        ImmutableUser iu5 = pgSession.selectOneBGuessConstructor("@sql/select_user_guess_setters_bean_param.sql", ImmutableUser.class, searchParam);
        pgSession.rollback();
        Assert.assertEquals(iu5.getName(), TEST_USERNAME);
        Assert.assertEquals(iu5.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu5.getEmployeeId(), TEST_EMPLOYEE_ID);

        ImmutableUser iu6 = pgSession.selectOneBSpecifyConstructor("@sql/select_user_use_constructor_bean_param.sql", ImmutableUser.class, searchParam);
        pgSession.rollback();
        Assert.assertEquals(iu6.getName(), TEST_USERNAME);
        Assert.assertEquals(iu6.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu6.getEmployeeId(), TEST_EMPLOYEE_ID);

        User iu7 = pgSession.selectOneBSpecifySetters("@sql/select_user_use_setters_bean_param.sql", User.class, searchParam);
        pgSession.rollback();
        Assert.assertEquals(iu7.getName(), TEST_USERNAME);
        Assert.assertEquals(iu7.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(iu7.getEmployeeId(), TEST_EMPLOYEE_ID);
    }

    @Test(priority=1)
    public void testNulls() {

        User anotherUser = new User();
        anotherUser.setId(UUID.fromString(ANOTHER_TEST_ID));
        // leave other fields null
        pgSession.insertB("@sql/insert_user.sql", anotherUser);
        pgSession.commit();

        User u = pgSession.selectOneVGuessSetters("@sql/select_user_guess_setters.sql", User.class, UUID.fromString(ANOTHER_TEST_ID));
        pgSession.rollback();
        Assert.assertEquals(u.getId(), UUID.fromString(ANOTHER_TEST_ID));
        Assert.assertNull(u.getPassword(), "Should be null");
        Assert.assertNull(u.getName(), "Should be null");
        Assert.assertEquals(u.getEmployeeId(), 0, "Should be 0");
    }

    // XXX START HERE: then write and test
    // -3) Do lists_of_scalar bean variants
    // -3.5) Do all scalar variadic and bean variants
    // -2) Write better comments in files
    // 0) Clean up other select commands to divide column discovery / method
    // finding from method execution.
    // 1) This creates a lot of command beans; explicitly set them to null when done with them, as hint to gc
    // Or, should each instance of a PGSession instantiate an instance of each command for re-use? Or, is that overengineering?
    // 2) select that returns more than one row;
    // 3) select that returns just one element.
    // 6) row listener that can be fed to select methods
    // 8) selectReport that returns map of string:colname string:value
    // for use in quick reporting displays where all values
    // would end up being cast to string anyway.
    // 4) More type converters
    // 5) manual conversion, for complexity and performance reasons
    // 7) find and document that JVM setting that makes java turn
    // reflection calls into compiled code faster (instead of waiting
    // for the default number of invocations).
    // 8) get stored procs working
    // 9) get copy to/from working
    // 10) get listen/notify working

    @Test(priority=2)
    public void testDelete() {
        User user = new User();
        user.setEmployeeId(THIRD_EMPLOYEE_ID);
        user.setId(UUID.fromString(THIRD_ID));
        user.setName(THIRD_USERNAME);
        user.setPassword(THIRD_PASSWORD);
        pgSession.insertB("@sql/insert_user.sql", user);
        pgSession.commit();

        User foundUser = pgSession.selectOneVGuessSetters("@sql/select_user_guess_setters.sql", User.class, UUID.fromString(THIRD_ID));
        pgSession.rollback();

        Assert.assertNotNull(foundUser, "User must be found.");

        int numberDeleted = pgSession.deleteV("@sql/delete_user.sql", UUID.fromString(THIRD_ID));
        pgSession.commit();
        Assert.assertEquals(numberDeleted, 1, "One user must be deleted.");

        foundUser = pgSession.selectOneVGuessSetters("@sql/select_user_guess_setters.sql", User.class, UUID.fromString(THIRD_ID));
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
        pgSession.insertB("@sql/insert_user.sql", user);
        pgSession.commit();

        user.setEmployeeId(UPDATED_THIRD_EMPLOYEE_ID);
        user.setName(UPDATED_THIRD_USERNAME);
        user.setPassword(UPDATED_THIRD_PASSWORD);
        int numberUpdated = pgSession.updateB("@sql/update_user.sql", user);
        pgSession.commit();

        User foundUser = pgSession.selectOneVGuessSetters("@sql/select_user_guess_setters.sql", User.class, UUID.fromString(THIRD_ID));
        pgSession.rollback();

        Assert.assertNotNull(foundUser, "User must be found.");

        Assert.assertEquals(numberUpdated, 1, "One user must be updated.");

        Assert.assertEquals(foundUser.getId(), UUID.fromString(THIRD_ID));
        Assert.assertEquals(foundUser.getName(), UPDATED_THIRD_USERNAME);
        Assert.assertEquals(foundUser.getEmployeeId(), UPDATED_THIRD_EMPLOYEE_ID);
        Assert.assertEquals(foundUser.getPassword(), UPDATED_THIRD_PASSWORD);
    }

    @Test(priority=4)
    public void testSelectMany() {
        pgSession.dml("truncate table users");
        pgSession.commit();

        List<ImmutableUser> expected = new ArrayList<>();
        expected.add(new ImmutableUser(UUID.fromString(ID_1), USERNAME_1, PASSWORD_1, EMPLOYEE_ID_1));
        expected.add(new ImmutableUser(UUID.fromString(ID_2), USERNAME_2, PASSWORD_2, EMPLOYEE_ID_2));
        expected.add(new ImmutableUser(UUID.fromString(ID_3), USERNAME_3, PASSWORD_3, EMPLOYEE_ID_3));
        for (ImmutableUser u : expected) {
            pgSession.insertB("@sql/insert_user.sql", u);
        }
        pgSession.commit();
        // TODO: this would be nice: pgSession.insertList("@sql/insert_list_of_users.sql", expected); // insert () values (), (), ();

        List<ImmutableUser> found = pgSession.selectListVGuessConstructor("@sql/select_all_users.sql", ImmutableUser.class);
        pgSession.rollback();
        // XXX: do a deep compare; already provided by List or Collections?
        Assert.assertTrue(expected.equals(found), "List of users must be the same");
    }

    @Test(priority=5)
    public void testSelectListOfScalar() {
        pgSession.dml("truncate table users");
        pgSession.commit();

        List<ImmutableUser> usersToLoad = new ArrayList<>();
        usersToLoad.add(new ImmutableUser(UUID.fromString(ID_1), USERNAME_1, PASSWORD_1, EMPLOYEE_ID_1));
        usersToLoad.add(new ImmutableUser(UUID.fromString(ID_2), USERNAME_2, PASSWORD_2, EMPLOYEE_ID_2));
        usersToLoad.add(new ImmutableUser(UUID.fromString(ID_3), USERNAME_3, PASSWORD_3, EMPLOYEE_ID_3));
        for (ImmutableUser u : usersToLoad) {
            pgSession.insertB("@sql/insert_user.sql", u);
        }
        pgSession.commit();
        // TODO: this would be nice: pgSession.insertList("@sql/insert_list_of_users.sql", expected); // insert () values (), (), ();

        List<Integer> expected = new ArrayList<>();
        expected.add(2);
        expected.add(3);

        List<Integer> found1 = pgSession.selectListVGuessScalar("@sql/select_employee_ids_guess_scalar.sql", Integer.class, 1);
        pgSession.rollback();
        Assert.assertTrue(expected.equals(found1), "List of employee_ids must be the same");

        List<Integer> found2 = pgSession.selectListVSpecifyScalar("@sql/select_employee_ids_specify_scalar.sql", Integer.class, 1);
        pgSession.rollback();
        Assert.assertTrue(expected.equals(found2), "List of employee_ids must be the same");

        User findUser = new User();
        findUser.setEmployeeId(1);
        // purposefully leave other attribs unset

        List<Integer> found3 = pgSession.selectListBGuessScalar("@sql/select_employee_ids_guess_scalar_bean_param.sql", Integer.class, findUser);
        pgSession.rollback();
        Assert.assertTrue(expected.equals(found3), "List of employee_ids must be the same");

        List<Integer> found4 = pgSession.selectListBSpecifyScalar("@sql/select_employee_ids_specify_scalar_bean_param.sql", Integer.class, findUser);
        pgSession.rollback();
        Assert.assertTrue(expected.equals(found4), "List of employee_ids must be the same");
    }
}
