# Build and Run

## Running the Test Suite

The test suite assumes a PostgreSQL instance is running on the default hostname
[localhost](http://www.postgresql.org/docs/9.4/static/auth-pg-hba-conf.html),
and on the default port [5432](http://www.postgresql.org/docs/9.4/static/app-postgres.html).
The test suite assumes the default user named 
[postgres](http://www.postgresql.org/docs/9.4/interactive/database-roles.html)
exists, and that
the default database named [postgres](http://www.postgresql.org/docs/9.4/static/app-initdb.html) 
exists.

If you need to override any of the default settings, the src/test/resources directory
has .properties files for the data source adapters needed to run the tests (as of this
writing, HikariCpDataSourceAdapter2.properties, PgSimpleDataSourceAdapter2.properties,
HikariCpDataSourceAdapter.properties, PgSimpleDataSourceAdapter.properties). Add one or more
of the following settings, as reqired: `hostname`, `port`, `database`, `user`, `password`.
Apologies for having to do this in 4 places, but, for now, the tests really do assume
the PostgreSQL defaults.

Ensure your PostgreSQL installation is running, or these tests will fail because there
will be no database to connect to.

The tests themselves use temporary tables whereever possible, and, where non-temporary items
have to be created, the tests remove those items at test completion. One goal of the
test suite is to leave the postgresql database in the same state that it began in, and not
leave test artifacts lying around.

All tests were written against PostgreSQL 9.3, in particular taking advantage of
`where not exists` clauses in many of the object creation DDL, so these tests may
not play well with earlier versions of PostgreSQL.

Once you have git cloned this repository, the top-level gradle wrapper file
is all that should be required to run the tests successfully (assuming your PostgreSQL
instance is running as just detailed).

```
./gradlew clean build
```

is all that should be necessary.

The build is driven by [Gradle](https://www.gradle.org/),
and the test suite uses [TestNG](http://testng.org/doc/index.html).

## IDE support

No particular development environment (Eclipse, Netbeans, IntelliJ) is assumed, 
and the directory layout follows the Maven Java project directory structure.

However, if your IDE has a Gradle plugin, cl4pg's build.gradle file and other Gradle goodies
will work fine with your IDE's Gradle plugin.

Also, no IDE-specific plugins are used in the build.gradle file. For instance, instead
of using Gradle's Eclipse plugin to generate Eclipse's project files, instead, it is assumed
that you will use Eclipse's Gradle plugin to build the project in Eclipse. This way, Gradle's
Eclipse plugin and Eclipse's Gradle plugin won't second-guess each other.

Same thing for IntelliJ: IntelliJ has a great Gradle plugin, so the build.gradle file does
not use an IntellJ plugin. Again, so that no second-guessing has to occur.



