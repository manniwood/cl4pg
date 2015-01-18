# Configuring DataSourceAdapters

Usefully, `java.sql.DataSource` allows for differing behaviors for 
`getConnection()`, and for `java.sql.Connection`'s `close()` method.

For some data sources, such as the PostgreSQL JDBC driver's 
`org.postgresql.ds.PGSimpleDataSource`, `getConnection()` opens
a new connection to the database every time, and the provided
connection's `close()` method closes the connection to the database.

Other implementations of `java.sql.DataSource` generally provide
database connection pooling facilities, so that `getConnection()`
and `close()` only get and return a database connection to a pool.

Cl4pg currently provides four data source adapters:

Name                         | Default Conf File                            | Wraps this DataSource
-----------------------------|----------------------------------------------|------------------
PgSimpleDataSourceAdapter    | cl4pg/PgSimpleDataSourceAdapter.properties   | PGSimpleDataSource
PgPoolingDataSourceAdapter   | cl4pg/PgPoolingDataSourceAdapter.properties  | PGPoolingDataSource
HikariCpDataSourceAdapter    | cl4pg/HikariCpDataSourceAdapter.properties   | [HikariDataSource](http://brettwooldridge.github.io/HikariCP/)
TomcatJDBCDataSourceAdapter  | cl4pg/TomcatJDBCDataSourceAdapter.properties | [TomcatJDBCDataSource](https://people.apache.org/~fhanik/jdbc-pool/jdbc-pool.html)

The reason why each is called a data source adapter is because
each implementation of `com.manniwood.cl4pg.v1.datasourceadapters.DataSourceAdapter`
implements methods for unwrapping the underlying `PGConnection` and `PGStatement`
objects from any implementation-specific wrapper objects, not to mention the standard
`java.sql.Connection`, `java.sql.PreparedStatement`, and `java.sql.CallableStatement` 
interfaces. Cl4pg's data source adapters allow its `PgSession` to expose PostgreSQL-specific 
functionality, such as copy, and listen/notify.

The easiest way to configure each data source adapter is with its
default configuration file. Put the configuration file at the expected place
in your classpath, and the following snippet of Java will get you started:

```Java
DataSourceAdapter dsa = PgSimpleDataSourceAdapter.buildFromDefaultConfFile();
DataSourceAdapter dsa = PgPoolingDataSourceAdapter.buildFromDefaultConfFile();
DataSourceAdapter dsa = HikariCpDataSourceAdapter.buildFromDefaultConfFile();
DataSourceAdapter dsa = TomcatJDBCDataSourceAdapter.buildFromDefaultConfFile();
```

However, if you need the configuration file to have a different name, each
data source adapter also has a static `buildFromConfFile("foo/bar.properties")` method.

Finally, if you want to programatically configure any of the data source adapters,
a handy fluent interface is provided. Here is an example:

```Java
DataSourceAdapter dsa = HikariCpDataSourceAdapter
        .configure()
        .database("foo")
        .hostname("somehost")
        .initialConnections(10)
        .maxConnections(20)
        .username("myuser")
        .password("password")
        .done();
```

Here are the configuration options for each of the data source adapters, using
the names that you will need for the configuration files. Please note that the
property names do not follow a case convention; this reflects that fact that
some of the property names are what the underlying PostgreSQL JDBC driver uses,
and the underlying JDBC driver did not follow a case convention for property names.

## Common to all adapters

Name                          | Default                   | Notes
------------------------------|---------------------------|--------
hostname                      | localhost                 | 
port                          | 5432                      | 
database                      | postgres                  | 
user                          | postgres                  | 
password                      | postgres                  | 
ApplicationName               | cl4pg                     | Shows up in the `application_name` 
                              |                           | column of `pg_stat_activity`
ExceptionConverter            | com.manniwood.            |
                              | cl4pg.v1.                 |
                              | exceptionconverters.      |
                              | DefaultExceptionConverter |
TransactionIsolationLevel     | read committed            | Other valid values are
                              |                           | read uncommitted
                              |                           | repeatable read
                              |                           | serializable
                              |                           | though please note that 
                              |                           | read uncommitted
                              |                           | doesn't actually work for 
                              |                           | PostgreSQL
AutoCommit                    | false                     |
ScalarResultSetHandlerBuilder | com.manniwood.            |
                              | cl4pg.v1.                 |
                              | resultsethandlers.        |
                              | GuessScalarResult-        |
                              | SetHandlerBuilder         | 
RowResultSetHandlerBuilder    | com.manniwood.            |
                              | cl4pg.v1.                 |
                              | resultsethandlers.        |
                              | GuessConstructorResult-   |
                              | SetHandlerBuilder         |


TODO: start here; document adapter-specific properties next, and make sure they
are even processed by each adapter


