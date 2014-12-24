# Cl4pg: The Convenience Library for PostgreSQL

## Why?

For people who need the reverse of a classic ORM. Instead of abstracting
away what makes PostgreSQL unique, Cl4pg lets you code directly in SQL,
and expostes key PostgreSQL functionality like listen/notify, and copy.

[More](docs/philosophy/why.md)

## Connect

Let's assume you have a file in your classpath `cl4pg/PgSimpleDataSourceAdapter.properties`,
and let's assume it has the following contents:

```
hostname=somehost
port=5432  # didn't have to be in this file; default
database=somedb
user=someuser
password=somepassword
ApplicationName=My Great App  # shows up in pg_stat_activity.application_name
TransactionIsolationLevel=READ COMMITTED # didn't have to be in this file; default
```

> Please note that the irregular use of upper and lower case in the parameter
names reflects the actual parameter names used by the underlying PostgreSQL
JDBC driver.

You get a connection to the database like so:

```Java
// Build a data source adapter for the PostgreSQL JDBC driver's included
// PGSimpleDataSource from cl4pg/PgSimpleDataSourceAdapter.properties
// in the classpath --- the default conf file for this adapter.
DataSourceAdapter adapter = PgSimpleDataSourceAdapter.buildFromDefaultConfFile();

// Hand the adapter to a PgSessionPool. When we hand PgSessionPool a
// PgSimpleDataSourceAdapter, there really is no pool at all: Every getSession()
// opens a new connection to the database, and every close() closes it.
// However, for PgPoolingDataSourceAdapter, HikariCpDataSourceAdapter,
// and TomcatJDBCDataSourceAdapter, getSession() and close() get and return
// database connections from the pools managed by those adapters.
PgSessionPool pool = new PgSessionPool(adapter);

// Start a session at somehost:5432/somedb, according to our example config
PgSession pgSession = pool.getSession();
```

[How to configure the other data source adapters...](docs/more/data_source_adapters.md)

## DDL

Let's create a users table.

```Java
pgSession.ddl("create temporary table users ( "
        + "id uuid, "
        + "name text, "
        + "password text, "
        + "employee_id int) ");
pgSession.commit();  // yes! Cl4pg lets you manage your own transactions!
```

Of course, Java lacks multiline string literals, so it's nicer to keep our
SQL in standalone files. Let's assume we have a file named `sql/create_temp_users_table.sql`
on our classpath, whose contents look like this:

```SQL
create temporary table users (
    id uuid,
    name text,
    password text,
    employee_id int);
```

Then, we would just use the `F` version of PgSession's ddl method:

```Java
pgSession.ddlF("sql/create_temp_users_table.sql");
pgSession.commit();
```

> As a general rule of thumb, for every method that takes a string literal
containing SQL, PgSesion will have a corresponding `F` method that instead
treats the string literal as a .sql file to be loaded from the classpath.

> Please also note that Cl4pg actually only loads files from the classpath once,
and then caches them in memory forever more, so subsequent `F` methods
using the same files fetch them from memory, not disk.

## Load

Let's assume we have the following PostgreSQL copy file named /tmp/users.copy

```
11111111-a4fa-49fc-b6b4-62eca118fbf7	user one	password one	1
22222222-a4fa-49fc-b6b4-62eca118fbf7	user two	password two	2
33333333-a4fa-49fc-b6b4-62eca118fbf7	user three	password three	3
```

We could load our users table from that file like so:

```Java
pgSession.copyIn("copy dup_users from stdin", "/tmp/users.copy");
pgSesion.commit();
```

PosgtreSQL's proprietary copy format is a first-class citizen
with Cl4pg.

## Listen/Notify

to be written

## Insert

to be written

## Select

to be written


