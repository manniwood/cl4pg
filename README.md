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
pgSession.copyIn("copy users from stdin", "/tmp/users.copy");
pgSesion.commit();
```

PosgtreSQL's proprietary copy format is a first-class citizen
with Cl4pg.

## Select

### One Row, One Column as an Object

Let's select a count of how many users we have.

```Java
Long count = pgSession.selectOneScalar("select count(*) from users");
```

Simple things should be simple. Cl4pg guesses the correct type converter and
converts the column "count( * )" to a Java long.

### One Row, Many Columns as a Bean

Let's select a user by user id, and return that result as a Java bean.

Let's assume the following immutable bean definition:

```Java
package com.manniwood.cl4pg.v1.test.etc;

import java.util.Objects;
import java.util.UUID;

public class ImmutableUser {

    private final UUID id;
    private final String name;
    private final String password;
    private final int employeeId;

    public ImmutableUser(UUID id, String name, String password, Integer employeeId) {
        super();
        this.id = id;
        this.name = name;
        this.password = password;
        this.employeeId = employeeId.intValue();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, password, employeeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ImmutableUser other = (ImmutableUser) obj;
        return Objects.equals(id, other.id)
                && Objects.equals(name, other.name)
                && Objects.equals(password, other.password)
                && Objects.equals(employeeId, other.employeeId);
    }
}
```

Let's also assume you have the following contents in a file named
`sql/find_user_by_id.sql` in your classpath:

```SQL
select id,
       name,
       password,
       employee_id
  from users
 where id = #{java.util.UUID}
```

You could then search for any particular user by ID like so:

```Java
ImmutableUser user = pgSession.selectOneF("sql/find_user_by_id.sql",
                         ImmutableUser.class,
                         UUID.fromString("99999999-a4fa-49fc-b6b4-62eca118fbf7"));
```

Cl4pg does a few things for us here.

`pgSession.selectOneF`'s first argument is the sql file on our classpath.

The second argument is the return type.

All remaining arguments are variadic, and of type Object; these get
substituted in the provided sql statement, in the order provided,
and are cast to the class given inside `#{}`. In our example, the
string `#{java.util.UUID}` gets filled in with the UUID we provide
as the first and only variadic arg, above.

> That is, under the covers, `#{}` becomes a `?` in a prepared statement, 
and it gets filled in like so: `preparedStatement.setObject(1, theUUID)`.

> A more interesting example might have a SQL template with
`where foo = #{java.lang.String} and bar = #{java.lang.Integer}`. If we
provided the variadic args to `pgSession.selectOneF` as "Hello" and 42,
under the covers, the SQL template would become `where foo = ? and bar = ?` 
as a prepared statement, and that prepared statement would get filled in 
with `pstmt.setString(1, "Hello")` followed by
`pstmt.setInt(2, 42)`. Finally, if the last argument,
was `null` instead of 42, the final prepared statement setter would 
have ended up being `pstmt.setNull(2, Types.INTEGER)`.

But that's just mapping the arguments going *in* to the SQL. What
about the rows coming *out* of the SQL? How do those create an
instance of ImmutableUser?

The second argument of `pgSession.selectOneF` is the return type, so
Cl4pg knows what type of bean it is trying to return. It then looks
at the return column types, in the order given, using 
`ResultSetMetaData.getColumnClassName()`. So in our example,
`id, name, password, employee_id` would correspond to 
UUID, String, String, Integer. We would therefore look for a constructor
matching the signature `ImmutableUser(UUID, String, String, Integer)`,
and use that constructor to build our ImmutableUser instance.

### Many Rows, Many Columns as a List of Beans

Let's say you want to return a list of users whose `employee_id`s are
greater than 42.

Let's assume a file named `sql/find_user_gt_emp_id.sql` in your classpath 
that has the following contents:

```SQL
select id,
       name,
       password,
       employee_id
  from users
 where employee_id > #{java.lang.Integer}
```

You could select a list of users whose `employee_id`s are greater than
42 like so:

```Java
List<ImmutableUser> users = pgSession.selectF("sql/find_user_gt_emp_id.sql",
                         ImmutableUser.class,
                         42);
```


## Exception Handling

## Listen/Notify

to be written

## Insert

to be written




