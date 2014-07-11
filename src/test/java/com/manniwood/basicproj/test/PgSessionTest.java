package com.manniwood.basicproj.test;

import java.util.UUID;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.manniwood.basicproj.PgSession;
import com.manniwood.basicproj.User;

public class PgSessionTest {

    @Test
    public void testPgSesion() {
        PgSession pgSession = new PgSession();
        User u = pgSession.selectOne("", User.class);
        Assert.assertEquals(u.getName(), "Foo");
        Assert.assertEquals(u.getId(), UUID.fromString("910c80af-a4fa-49fc-b6b4-62eca118fbf7"));
        Assert.assertEquals(u.getEmployeeId(), 42);
    }
}
