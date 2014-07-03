package com.manniwood.basicproj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Explore {

    private final static Logger log = LoggerFactory.getLogger(Explore.class);

    public static void main(String[] args) {
        HelloWorldEchoer hwe = new HelloWorldEchoer();
        log.info("Hello World Echoer says: {}", hwe.getMessage());
        Object o = "Hello";
        log.info("object class is {}", o.getClass().getSimpleName());

        // get all methods
        // collect those that begin with "set"
        // put them in a map of <setterName, paramType>
    }
}
