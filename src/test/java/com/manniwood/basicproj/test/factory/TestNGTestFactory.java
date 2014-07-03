package com.manniwood.basicproj.test.factory;

import org.testng.annotations.Factory;

import com.manniwood.basicproj.test.TestHelloWorldEchoer;

public class TestNGTestFactory {
    @Factory
    public Object[] allTests() {
        return new Object[] {
                new TestHelloWorldEchoer()
        };
    }

}
