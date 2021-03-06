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
import com.manniwood.cl4pg.v1.test.etc.TwoInts;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
public abstract class AbstractStoredProcTest {

    private PgSession pgSession;
    private DataSourceAdapter adapter;

    @BeforeClass
    public void init() {

        adapter = configureDataSourceAdapter();

        pgSession = adapter.getSession();

        pgSession.ddl("sql/create_temp_users_table.sql");
        pgSession.ddl("sql/create_swap_func.sql");
        pgSession.ddl("sql/create_add_to_first.sql");
        pgSession.ddl("sql/create_add_to_last.sql");
        pgSession.ddl("sql/create_add_and_return.sql");
        pgSession.ddl("sql/create_get_user_by_id_func.sql");
        pgSession.ddl("sql/create_get_user_by_gt_emp_id_func.sql");
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
            pgSession.insert(u, "sql/insert_user.sql");
        }
        pgSession.commit();

    }

    protected abstract DataSourceAdapter configureDataSourceAdapter();

    @AfterClass
    public void tearDown() {
        pgSession.ddl("sql/drop_swap_func.sql");
        pgSession.ddl("sql/drop_add_to_first.sql");
        pgSession.ddl("sql/drop_add_to_last.sql");
        pgSession.ddl("sql/drop_add_and_return.sql");
        pgSession.ddl("sql/drop_get_user_by_id_func.sql");
        pgSession.qDdl("drop function get_user_by_gt_emp_id(a_employee_id integer);");
        pgSession.commit();
        pgSession.close();
        adapter.close();
    }

    @Test(priority = 0)
    public void testSwapFunc() {

        TwoInts expected = new TwoInts();
        expected.setFirst(2);
        expected.setSecond(1);

        TwoInts actual = new TwoInts();
        actual.setFirst(1);
        actual.setSecond(2);

        pgSession.procInOut(actual, "sql/swap.sql");
        pgSession.rollback();

        Assert.assertEquals(actual, expected, "Swap needs to have happened.");
    }

    @Test(priority = 1)
    public void testQSwapFunc() {

        TwoInts expected = new TwoInts();
        expected.setFirst(2);
        expected.setSecond(1);

        TwoInts actual = new TwoInts();
        actual.setFirst(1);
        actual.setSecond(2);

        pgSession.qProcInOut(actual, "{ call swap_them( #{getFirst/setFirst}, #{getSecond/setSecond} ) }");
        pgSession.rollback();

        Assert.assertEquals(actual, expected, "Swap needs to have happened.");
    }

    @Test(priority = 2)
    public void testAddToFirstFunc() {

        TwoInts expected = new TwoInts();
        expected.setFirst(5);
        expected.setSecond(3);

        TwoInts actual = new TwoInts();
        actual.setFirst(2);
        actual.setSecond(3);

        pgSession.procInOut(actual, "sql/add_to_first.sql");
        pgSession.rollback();

        Assert.assertEquals(actual,
                expected,
                "Add to first needs to have happened.");
    }

    @Test(priority = 3)
    public void testQAddToFirstFunc() {

        TwoInts expected = new TwoInts();
        expected.setFirst(5);
        expected.setSecond(3);

        TwoInts actual = new TwoInts();
        actual.setFirst(2);
        actual.setSecond(3);

        pgSession.qProcInOut(actual, "{ call add_to_first( #{getFirst/setFirst}, #{getSecond} ) }");
        pgSession.rollback();

        Assert.assertEquals(actual,
                expected,
                "Add to first needs to have happened.");
    }

    @Test(priority = 4)
    public void testAddToLastFunc() {

        TwoInts expected = new TwoInts();
        expected.setFirst(2);
        expected.setSecond(5);

        TwoInts actual = new TwoInts();
        actual.setFirst(2);
        actual.setSecond(3);

        pgSession.procInOut(actual, "sql/add_to_last.sql");
        pgSession.rollback();

        Assert.assertEquals(actual,
                expected,
                "Add to last needs to have happened.");
    }

    @Test(priority = 5)
    public void testQAddToLastFunc() {

        TwoInts expected = new TwoInts();
        expected.setFirst(2);
        expected.setSecond(5);

        TwoInts actual = new TwoInts();
        actual.setFirst(2);
        actual.setSecond(3);

        pgSession.qProcInOut(actual, "{ call add_to_last( #{getFirst}, #{getSecond/setSecond} ) }");
        pgSession.rollback();

        Assert.assertEquals(actual,
                expected,
                "Add to last needs to have happened.");
    }

    @Test(priority = 6)
    public void testAddAndReturnV() {
        Integer sum = pgSession.qSelectOneScalar(
                "select add_and_return from add_and_return(#{java.lang.Integer}, #{java.lang.Integer})",
                1,
                2);

        Assert.assertEquals(sum.intValue(),
                            3,
                            "Stored proc needs to return 3");
    }

    @Test(priority = 7)
    public void testAddAndReturnB() {

        TwoInts addends = new TwoInts();
        addends.setFirst(2);
        addends.setSecond(3);

        Integer sum2 = pgSession.qSelectOneScalar(
                addends,
                "select add_and_return from add_and_return(#{getFirst}, #{getSecond})");

        Assert.assertEquals(sum2.intValue(),
                            5,
                            "Stored proc needs to return 3");
    }

    @Test(priority = 8)
    public void testRefCursorProcV() {
        ImmutableUser expected = new ImmutableUser(UUID.fromString(AbstractSetApplicationNameTest.ID_3),
                                                   AbstractSetApplicationNameTest.USERNAME_3,
                                                   AbstractSetApplicationNameTest.PASSWORD_3,
                                                   AbstractSetApplicationNameTest.EMPLOYEE_ID_3);
        ImmutableUser actual = pgSession.qProcSelectOne(
                expected,
                "{ #{refcursor} = call get_user_by_id(#{getId}) }",
                ImmutableUser.class);
        pgSession.rollback();
        Assert.assertEquals(actual, expected, "Found user needs to match.");
    }

    @Test(priority = 9)
    public void testRefCursorProcB() {
        ImmutableUser expected = new ImmutableUser(UUID.fromString(AbstractSetApplicationNameTest.ID_3),
                                                   AbstractSetApplicationNameTest.USERNAME_3,
                                                   AbstractSetApplicationNameTest.PASSWORD_3,
                                                   AbstractSetApplicationNameTest.EMPLOYEE_ID_3);
        ImmutableUser actual = pgSession.qProcSelectOne(
                "{ #{refcursor} = call get_user_by_id(#{java.util.UUID}) }",
                ImmutableUser.class,
                expected.getId());
        pgSession.rollback();
        Assert.assertEquals(actual, expected, "Found user needs to match.");
    }

    @Test(priority = 10)
    public void testRefCursorProcVList() {
        List<ImmutableUser> users = pgSession.qProcSelect(
                "{ #{refcursor} = call get_user_by_gt_emp_id(#{java.lang.Integer}) }",
                ImmutableUser.class,
                1);
        pgSession.rollback();
        Assert.assertTrue(users.size() == 2);
    }

    // TODO: select list
    // TODO: select list of scalar?
}
