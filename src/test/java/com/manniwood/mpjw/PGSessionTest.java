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

import com.manniwood.mpjw.test.etc.User;

public class PGSessionTest {

    public static final String TEST_PASSWORD = "passwd";
    public static final String TEST_USERNAME = "Hubert";
    public static final int TEST_EMPLOYEE_ID = 13;
    public static final String TEST_ID = "99999999-a4fa-49fc-b6b4-62eca118fbf7";

    public static final String ANOTHER_TEST_ID = "88888888-a4fa-49fc-b6b4-62eca118fbf7";

    private PGSession pgSession;

    @BeforeClass
    public void init() {
        pgSession = new PGSession();
        pgSession.ddl("@sql/create_temp_users_table.sql");
        pgSession.commit();
    }

    @Test(priority=0)
    public void testPgSesion() {


        User insertUser = new User();
        insertUser.setEmployeeId(TEST_EMPLOYEE_ID);
        insertUser.setId(UUID.fromString(TEST_ID));
        insertUser.setName(TEST_USERNAME);
        insertUser.setPassword(TEST_PASSWORD);
        pgSession.insert("@sql/insert_user.sql", insertUser);
        pgSession.commit();

        User findUser = new User();
        findUser.setId(UUID.fromString(TEST_ID));

        User u = pgSession.selectOne("@sql/select_user.sql", User.class, findUser);
        Assert.assertEquals(u.getName(), TEST_USERNAME);
        Assert.assertEquals(u.getId(), UUID.fromString(TEST_ID));
        Assert.assertEquals(u.getEmployeeId(), TEST_EMPLOYEE_ID);
    }

    @Test(priority=1)
    public void testNulls() {

        User anotherUser = new User();
        anotherUser.setId(UUID.fromString(ANOTHER_TEST_ID));
        // leave other fields null
        pgSession.insert("@sql/insert_user.sql", anotherUser);
        pgSession.commit();

        User findAnotherUser = new User();
        findAnotherUser.setId(UUID.fromString(ANOTHER_TEST_ID));

        User u = pgSession.selectOne("@sql/select_user.sql", User.class, findAnotherUser);
        Assert.assertEquals(u.getId(), UUID.fromString(ANOTHER_TEST_ID));
        Assert.assertNull(u.getPassword(), "Should be null");
        Assert.assertNull(u.getName(), "Should be null");
        Assert.assertEquals(u.getEmployeeId(), 0, "Should be 0");
    }
}
