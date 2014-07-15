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
import org.testng.annotations.Test;

import com.manniwood.mpjw.test.etc.User;

public class PGSessionTest {

    @Test
    public void testPgSesion() {
        PGSession pgSession = new PGSession();

        pgSession.ddl("@sql/create_test_table.sql");
        pgSession.commit();

        User insertUser = new User();
        insertUser.setEmployeeId(13);
        insertUser.setId(UUID.fromString("99999999-a4fa-49fc-b6b4-62eca118fbf7"));
        insertUser.setName("Hubert");
        insertUser.setPassword("passwd");
        pgSession.insert("@sql/insert_user.sql", insertUser);
        pgSession.commit();

        User u = pgSession.selectOne("", User.class);
        Assert.assertEquals(u.getName(), "Foo");
        Assert.assertEquals(u.getId(), UUID.fromString("910c80af-a4fa-49fc-b6b4-62eca118fbf7"));
        Assert.assertEquals(u.getEmployeeId(), 42);
    }
}