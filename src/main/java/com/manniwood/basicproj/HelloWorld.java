package com.manniwood.basicproj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld {

    private final static Logger log = LoggerFactory.getLogger(HelloWorld.class);

    public static void main(String[] args) {
        HelloWorldEchoer hwe = new HelloWorldEchoer();
        log.info("Hello World Echoer says: {}", hwe.getMessage());
    }
}
