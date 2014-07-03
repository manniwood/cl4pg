package com.manniwood.basicproj.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.manniwood.basicproj.HelloWorldEchoer;

public class TestHelloWorldEchoer {
    private final static Logger log = LoggerFactory.getLogger(TestHelloWorldEchoer.class);

    @Test
    public void test() {
        log.info("Testing Hello World");
        HelloWorldEchoer hwe = new HelloWorldEchoer();
        String message = hwe.getMessage();
        Assert.assertEquals(message, HelloWorldEchoer.MESSAGE, "Messages need to match.");
    }
}
