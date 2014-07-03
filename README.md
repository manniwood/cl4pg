# Manni's PostgreSQL JDBC Wrapper

This is a template I use to start Java projects.

You'll find:

- the Java project directory layout popularized by Maven (and now used by other tools)
- a gradle file which can be used to build/test/run/clean/distro the project (more on that later)
- a basic HelloWorld program with logging configured (slf4j and logback)
- a basic TestNG test framework in place to test the HelloWorld program

The gradle files are configured to support the tasks you would expect.
As a general rule, you would do 

```
gradle clean build
```

and everything would just work.

When you are ready to deploy your work, you would do

```
gradle distZip
```

and a correctly named and versioned zip file will be built in ./build/distributions.

To run the application:

```
gradle run
```

To just compile:

```
gradle compileJava
```

To just test:

```
gradle test
```

