# cl4pg: The Convenience Library for PostgreSQL

## Why?

For people who need the reverse of a classic ORM. Instead of abstracting
away what makes PostgreSQL unique, cl4pg lets you code directly in SQL,
and expostes key PostgreSQL functionality like listen/notify, and copy.

[More](docs/philosophy/why.md)

## Connect

Let's assume you have a file in your classpath `cl4pg/PgSimpleDataSourceAdapter.properties`,
(in `src/main/resources` in your Java project if you are using the Maven project layout)
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
// For PgSimpleDataSourceAdapter, every getSession()
// opens a new connection to the database, and every close() closes it.
// However, for PgPoolingDataSourceAdapter, HikariCpDataSourceAdapter,
// and TomcatJDBCDataSourceAdapter, getSession() and close() get and return
// database connections from the pools managed by those adapters.
DataSourceAdapter adapter = PgSimpleDataSourceAdapter.buildFromDefaultConfFile();

// Start a session at somehost:5432/somedb, according to our example config
PgSession pgSession = adapter.getSession();
```

[How to configure the other data source adapters...](docs/more/data_source_adapters.md)

## DDL

Let's create a users table.

```Java
pgSession.qDdl("create temporary table users ( "
        + "id uuid, "
        + "name text, "
        + "password text, "
        + "employee_id int) ");
pgSession.commit();  // yes! cl4pg lets you manage your own transactions!
```

Of course, Java lacks multiline string literals, so it's nicer to keep our
SQL in standalone files. Let's assume we have a file named `sql/create_temp_users_table.sql`
on our classpath
(in `src/main/resources` in your Java project if you are using the Maven project layout),
whose contents look like this:

```SQL
create temporary table users (
    id uuid,
    name text,
    password text,
    employee_id int);
```

And let's assume we have a file named `cl4pg/SqlCache.txt` in our classpath whose contents
look like this:

```
###########################################################
## Any file listed in here gets cached by cl4pg on startup.
## cl4pg will expect the file to be on the classpath.
###########################################################
sql/create_temp_users_table.sql
```

Then, we would just use this version of PgSession's ddl method:

```Java
pgSession.ddl("sql/create_temp_users_table.sql");
pgSession.commit();
```

### `q` versus non-`q` naming convention

Most methods in cl4pg have a corresponding `q` method. So, for instance, the
`select` method has a corresponding `qSelect` method, the `insert` method has
a corresponding `qInsert` method, etc. The `q` in the `q` methods stands for
"quick", and is used for quick one-offs were the string literal argument is
a SQL statement. The non-`q` methods, which will presumably get more use on
larger projects with more complex sql statements, treat the string literal as
the name of a file on the classpath containing a SQL statement.

When using SQL files that get loaded from the classpath, cl4pg loads files 
from the classpath once, on startup,
into an unmodifiable map, allowing multiple threads to read
from the cache of loaded SQL files without contention or slowdown.

## Load

Let's assume we have the following PostgreSQL copy file named /tmp/users.copy

```
11111111-a4fa-49fc-b6b4-62eca118fbf7	user one	password one	1
22222222-a4fa-49fc-b6b4-62eca118fbf7	user two	password two	2
33333333-a4fa-49fc-b6b4-62eca118fbf7	user three	password three	3
```

We could load our users table from that file like so:

```Java
pgSession.qCopyIn("copy users from stdin", "/tmp/users.copy");
pgSesion.commit();
```

PosgtreSQL's proprietary copy format is a first-class citizen
with cl4pg.

## Select

### One Row, One Column as an Object

Let's select a count of how many users we have.

```Java
Long count = pgSession.qSelectOneScalar("select count(*) from users");
pgSession.rollback();  // no need to commit
```

Simple things should be simple. Cl4pg determines the correct type converter
based on the return type (`Long`, in this example) and
converts the column "count( * )" to a Java Long object.

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
    private final Integer employeeId;

    public ImmutableUser(UUID id, String name, String password, Integer employeeId) {
        super();
        this.id = id;
        this.name = name;
        this.password = password;
        this.employeeId = employeeId;
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

    public Integer getEmployeeId() {
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
`sql/find_user_by_id.sql` in your classpath
(in `src/main/resources` in your Java project if you are using the Maven project layout):

```SQL
select id,
       name,
       password,
       employee_id
  from users
 where id = #{java.util.UUID}
```

And let's assume we have a file named `cl4pg/SqlCache.txt` in our classpath whose contents
now look like this (adding to the cache file from our previous example):

```
###########################################################
## Any file listed in here gets cached by cl4pg on startup.
## cl4pg will expect the file to be on the classpath.
###########################################################
sql/create_temp_users_table.sql
sql/find_user_by_id.sql
```

You could then search for any particular user by ID like so:

```Java
ImmutableUser user = pgSession.selectOne("sql/find_user_by_id.sql",
                         ImmutableUser.class,
                         UUID.fromString("99999999-a4fa-49fc-b6b4-62eca118fbf7"));
pgSession.rollback();  // no need to commit
```

Cl4pg does a few things for us here.

`pgSession.selectOne`'s first argument is the sql file on our classpath.

The second argument is the return type.

All remaining arguments are variadic, and of type Object; these get
substituted in the provided sql statement, in the order provided,
and are cast to the class given inside `#{}`. In our example, the
string `#{java.util.UUID}` gets filled in with the UUID we provide
as the first and only variadic arg, above.

That is, under the covers, `#{}` becomes a `?` in a prepared statement,
and it gets filled in like so: `preparedStatement.setObject(1, theUUID)`.

A more interesting example might have a SQL template with
`where foo = #{java.lang.String} and bar = #{java.lang.Integer}`. If we
provided the variadic args to `pgSession.selectOne` as "Hello" and 42,
under the covers, the SQL template would become `where foo = ? and bar = ?`
as a prepared statement, and that prepared statement would get filled in
with `pstmt.setString(1, "Hello")` followed by
`pstmt.setInt(2, 42)`. Finally, if the last argument,
was `null` instead of 42, the final prepared statement setter would
have ended up being `pstmt.setNull(2, Types.INTEGER)`.

But that's just mapping the arguments going *in* to the SQL. What
about the rows coming *out* of the SQL? How do those create an
instance of ImmutableUser?

The second argument of `pgSession.selectOne` is the return type, so
cl4pg knows what type of bean it is trying to return. It then looks
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
(in `src/main/resources` in your Java project if you are using the Maven project layout)
that has the following contents:

```SQL
select id,
       name,
       password,
       employee_id
  from users
 where employee_id > #{java.lang.Integer}
```

And let's assume we have a file named `cl4pg/SqlCache.txt` in our classpath whose contents
now look like this (adding to the cache file from our previous example):

```
###########################################################
## Any file listed in here gets cached by cl4pg on startup.
## cl4pg will expect the file to be on the classpath.
###########################################################
sql/create_temp_users_table.sql
sql/find_user_by_id.sql
sql/find_user_gt_emp_id.sql
```

You could select a list of users whose `employee_id`s are greater than
42 like so:

```Java
List<ImmutableUser> users = pgSession.select("sql/find_user_gt_emp_id.sql",
                         ImmutableUser.class,
                         42);
pgSession.rollback();  // no need to commit
```

### Using a Bean Instead of Variadic Args

So far, we have shown the utility of selecting our ImmutableUser by variadic args.
If we only have an ID for a user, we can just hand that ID to `pgSession.select` or
`pgSession.select_` as a variadic arg, and cl4pg will fill in `#{java.util.UUID}`
in the correct spot in our SQL template.

There may be other times when you have values you would like to get pulled out of
a bean to fill in the parameters of our SQL template. This is possible to! Instead of using
`pgSession.select("some SQL", SomeReturn.class, variadic args...)`, you would use
`pgSession.select(SomeBean, "some SQL", SomeReturn.class)`. It's a nice calling convention:
if you want to use variadic args, they go as the last arguments to `pgSession.select`, but
if you want to use a bean, it is the first argument of `pgSession.select`.

Your SQL template will need to change, too. Now, it will look like this (in a file
`sql/find_user_by_bean_id.sql`):

```SQL
select id,
       name,
       password,
       employee_id
  from users
 where id = #{getId}
```

Our `cl4pg/SqlCache.txt` file now looks like this:

```
###########################################################
## Any file listed in here gets cached by cl4pg on startup.
## cl4pg will expect the file to be on the classpath.
###########################################################
sql/create_temp_users_table.sql
sql/find_user_by_id.sql
sql/find_user_gt_emp_id.sql
sql/find_user_by_bean_id.sql
```

Our java call:

```Java
// Note we only bother correctly filling in the one attribute we need
ImmutableUser findMe = new ImmutableUser(
    "99999999-a4fa-49fc-b6b4-62eca118fbf7",
    null,
    null,
    0);

ImmutableUser actualImmutable = pgSession.selectOne(findMe,
                                 "sql/find_user_by_bean_id.sql",
                                 ImmutableUser.class);
pgSession.rollback();  // no need to commit
```

Or, returning to our example of finding users whose `employee_id` is greater than 42:

Let's assume a file named `sql/find_user_gt_emp_id_bean.sql` in your classpath
(in `src/main/resources` in your Java project if you are using the Maven project layout)

```SQL
select id,
       name,
       password,
       employee_id
  from users
 where employee_id > #{getEmployeeId}
```

Our `cl4pg/SqlCache.txt` file now looks like this:

```
###########################################################
## Any file listed in here gets cached by cl4pg on startup.
## cl4pg will expect the file to be on the classpath.
###########################################################
sql/create_temp_users_table.sql
sql/find_user_by_id.sql
sql/find_user_gt_emp_id.sql
sql/find_user_by_bean_id.sql
sql/find_user_gt_emp_id_bean.sql
```

Our Java call:

```Java
// Note we only bother correctly filling in the one attribute we need
ImmutableUser findMe = new ImmutableUser(
    "00000000-a4fa-49fc-b6b4-62eca118fbf7",
    null,
    null,
    42);

ImmutableUser actualImmutable = pgSession.selectOne(findMe,
                                 "sql/find_user_gt_emp_id_bean.sql",
                                 ImmutableUser.class);
pgSession.rollback();  // no need to commit
```

### Using Setters Instead of Constructors, and More!

There are many more ways to select data from cl4pg and map it to your Java
objects. For instance, you do not have to use immutable beans: You can use
beans with null constructors and build them using setter methods instead!
[Details here.](docs/more/select.md)

## Insert

Cl4pg's `insert` method names and signatures follow the same conventions as `select`:

- If the method is named `qInsert`, the SQL is assumed to be right in the string, whereas
if the method is named `insert`, the SQL is assumed to be a file in the classpath with that name.

- If the String comes first in the `insert`/`qInsert` method, it is assumed that variadic args come next.
If an Object is first, followed by a String, the Object is a bean whose getters will be used to
fill in parameters.

Let's assume this SQL file on the classpath
(in `src/main/resources` in your Java project if you are using the Maven project layout):

`sql/insert_user_variadic.sql`:

```SQL
insert into users (
    id,  -- UUID
    name,  -- text
    password,  -- text
    employee_id)  -- int
values (#{java.util.UUID},
        #{java.lang.String},
        #{java.lang.String},
        #{java.lang.Integer})
```

Our `cl4pg/SqlCache.txt` file now looks like this:

```
###########################################################
## Any file listed in here gets cached by cl4pg on startup.
## cl4pg will expect the file to be on the classpath.
###########################################################
sql/create_temp_users_table.sql
sql/find_user_by_id.sql
sql/find_user_gt_emp_id.sql
sql/find_user_by_bean_id.sql
sql/find_user_gt_emp_id_bean.sql
sql/insert_user_variadic.sql
```

We can insert a user like this:


```Java
pgSession.insert("sql/insert_user_variadic.sql",
    "00000000-a4fa-49fc-b6b4-62eca118fbf7",
    null,
    "password",
    42);
pgSession.commit();  // don't forget!
```

If we assume this SQL file on the classpath
(in `src/main/resources` in your Java project if you are using the Maven project layout):

`sql/insert_user.sql`:

```SQL
insert into users (
    id,  -- UUID
    name,  -- text
    password,  -- text
    employee_id)  -- int
values (#{getId},
        #{getName},
        #{getPassword},
        #{getEmployeeId})
```

Our `cl4pg/SqlCache.txt` file now looks like this:

```
###########################################################
## Any file listed in here gets cached by cl4pg on startup.
## cl4pg will expect the file to be on the classpath.
###########################################################
sql/create_temp_users_table.sql
sql/find_user_by_id.sql
sql/find_user_gt_emp_id.sql
sql/find_user_by_bean_id.sql
sql/find_user_gt_emp_id_bean.sql
sql/insert_user_variadic.sql
sql/insert_user.sql
```

We can a use a bean like this:


```Java
ImmutableUser newUser = new ImmutableUser(
    "99999999-a4fa-49fc-b6b4-62eca118fbf7",
    "Bob",
    "easypassword",
    1);
pgSession.insert(newUser, "sql/insert_user.sql");
pgSession.commit();  // don't forget!
```

## Exception Handling

Whenever an exception is encountered, cl4pg *automatically does a rollback*, and throws
a `Cl4pgException`, or a subclass of `Cl4pgException`, which is an unchecked exception.

[Find out why Cl4pgException is unchecked.](docs/philosophy/exceptions.md)

The original, underlying exception can always be accessed using `getCause()`.

Furthermore, if the underlying exception was a `PSQLException`, it will get wrapped in a
`Cl4pgPgSqlException`, which will have many useful access methods such as `getTable()`
and `getConstraint()`.

Finally, when the underlying exception is a `PSQLException`, which is in turn wrapped
in a `Cl4pgPgSqlException`, that `Cl4pgPgSqlException` actually goes through an exception
converter before being thrown. The default exception converter is quite simple, it just does
a pass-through:

```Java
public class DefaultExceptionConverter implements ExceptionConverter {
    @Override
    public Cl4pgException convert(Cl4pgPgSqlException e) {
        return e;
    }
}
```

However, you can write your own exception converter to throw more specific exceptions instead.

For instance, let's assume this table definition:

```SQL
create table users (
    id uuid constraint users_pk primary key not null,
    name text not null,
    password text not null,
    employee_id int constraint users_employee_id_uniq not null);
```

Let's assume a file named `sql/insert_user.sql` on the classpath and listed 
in your `cl4pg/SqlCache.txt` file:
 
```Java
insert into users (
    id,  -- UUID
    name,  -- text
    password,  -- text
    employee_id)  -- int
values (#{getId},
        #{getName},
        #{getPassword},
        #{getEmployeeId})
```

Let's assume we want to throw the following exception every time we try to
create a user that already exists:

```Java
import org.postgresql.util.ServerErrorMessage;

import com.manniwood.cl4pg.v1.exceptions.Cl4pgPgSqlException;

public class UserAlreadyExistsException extends Cl4pgPgSqlException {

    private static final long serialVersionUID = 1L;

    public UserAlreadyExistsException() {
    }

    public UserAlreadyExistsException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public UserAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserAlreadyExistsException(String message) {
        super(message);
    }

    public UserAlreadyExistsException(Throwable cause) {
        super(cause);
    }

    public UserAlreadyExistsException(ServerErrorMessage sem) {
        super(sem);
    }

    public UserAlreadyExistsException(ServerErrorMessage sem, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(sem, message, cause, enableSuppression, writableStackTrace);
    }

    public UserAlreadyExistsException(ServerErrorMessage sem, String message, Throwable cause) {
        super(sem, message, cause);
    }

    public UserAlreadyExistsException(ServerErrorMessage sem, String message) {
        super(sem, message);
    }

    public UserAlreadyExistsException(ServerErrorMessage sem, Throwable cause) {
        super(sem, cause);
    }
}
```

Now let's assume the following exception converter:

```Java
import com.manniwood.cl4pg.v1.exceptionconverters.ExceptionConverter;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgException;
import com.manniwood.cl4pg.v1.exceptions.Cl4pgPgSqlException;
import com.manniwood.cl4pg.v1.test.exceptions.UserAlreadyExistsException;

public class TestExceptionConverter implements ExceptionConverter {
    @Override
    public Cl4pgException convert(Cl4pgPgSqlException e) {
        // http://www.postgresql.org/docs/9.3/static/errcodes-appendix.html
        // shows that sql state 23505 is "unique_violation". When we created
        // our users table, we named the primary key constraint "users_pk".
        if ("23505".equals(e.getSqlState()) && "users_pk".equals(e.getConstraint())) {
            return new UserAlreadyExistsException(e);
        } else {
            return e;
        }
    }
}
```

We plug our exception converter simply by putting this line in our configuration
file:

```INI
ExceptionConverter=com.something.exceptionmappers.TestExceptionConverter
```

And now, this piece of code should work as expected:

```Java
ImmutableUser newUser = new ImmutableUser(
    "99999999-a4fa-49fc-b6b4-62eca118fbf7",
    "Bob",
    "easypassword",
    1);
pgSession.insert(newUser, "sql/insert_user.sql");
pgSession.commit();
boolean correctlyCaughtException = false;
try {
    pgSession.insert(newUser, "sql/insert_user.sql");
    pgSession.commit();
} catch (UserAlreadyExistsException e) {
    log.info("Cannot insert user twice!");
    log.info("Exception: " + e.toString(), e);
    correctlyCaughtException = true;
}
Assert.assertTrue(correctlyCaughtException, "Had to catch custom exception");
```

What's particularly nice is that any truly exceptional exception (database not
reachable, database ran out of storage, database exploded) will not be caught
here, and, instead, can percolate up to whatever part of your application
handles more "exceptional" exceptions.


## Listen/Notify

Listen/Notify are first-class citizens with cl4pg.

One session decides to listen for notifications:

```Java
pgSession1.pgListen("listening_place");
pgSession1.commit();
```

Another session decides to send some notifications:

```Java
pgSession2.pgNotify("listening_place", "Hello, world!");
pgSession2.pgNotify("listening_place", "Lalalalala!");
pgSession2.commit();
```

And our first session decides to check for messages:

```Java
// the PGNotificaton type is provided by the PostgreSQL JDBC driver
PGNotification[] notifications = pgSession1.getNotifications();
pgSession1.commit();
for (PGNotification notification : notifications) {
    log.info("notification name {}, parameter: {}, pid: {}", 
        notification.getName(), 
        notification.getParameter(), 
        notification.getPID());
}
```

## Stored Procedures

Let's say you have the following stored procedure which swaps the values of
its arguments:

```sql
create or replace function swap_them(inout first int, inout second int)
immutable
as
$body$
declare
    tmp int;
begin
    tmp := first;
    first := second;
    second := tmp;
end;
$body$
language plpgsql;
```

You could make a Java bean with two attribues and swap them by calling
the stored procedure like so.

Here is our bean,

```Java
public class TwoInts {
    private int first;
    private int second;
    public int getFirst() {
        return first;
    }
    public void setFirst(int first) {
        this.first = first;
    }
    public int getSecond() {
        return second;
    }
    public void setSecond(int second) {
        this.second = second;
    }
}
```

and here is how we can swap its attributes:


```Java
TwoInts actual = new TwoInts();
actual.setFirst(1);
actual.setSecond(2);

pgSession.qProcInOut(
    actual, 
    "{ call swap_them( #{getFirst/setFirst}, #{getSecond/setSecond} ) }");
pgSession.rollback();
```

As you can see, for inout params, our SQL string literal lists the bean's getter
and setter methods separated by a slash, so that cl4pg can figure out how to
use the bean for the in and out capabilities of inout parameters.

Let's say you have this stored procedure defined:

```sql
create or replace function add_to_first(inout first int, in second int)
immutable
as
$body$
begin
    first := first + second;
end;
$body$
language plpgsql;
```

You would call this stored procedure like so:

```Java
pgSession.qProcInOut(actual, "{ call add_to_first( #{getFirst/setFirst}, #{getSecond} ) }");
pgSession.rollback();
```

Here, only the first argument of the stored procedure is an inout; the second is just an in,
so in the second `#{}`, we only need to tell cl4pg about the bean's getter method for
feeding the in parameter.

Let's say this stored procedure is defined:

```sql
create or replace function add_and_return(first int, second int)
returns int
immutable
as
$body$
begin
    return first + second;
end;
$body$
language plpgsql;
```

Here, you can use a regular select, either like so:

```Java
Integer sum = pgSession.qSelectOneScalar(
        "select add_and_return from add_and_return(#{java.lang.Integer}, #{java.lang.Integer})",
        1,
        2);
```

or like so:

```Java
TwoInts addends = new TwoInts();
addends.setFirst(2);
addends.setSecond(3);

Integer sum2 = pgSession.qSelectOneScalar(
        addends,
        "select add_and_return from add_and_return(#{getFirst}, #{getSecond})");
```

Finally, there is handling for stored procedures that return result sets.

Let's say you have the following stored procedure defined:

```sql
create or replace function get_user_by_id(a_id uuid)
returns refcursor
as
$body$
declare
    mycurs refcursor;
begin
    open mycurs for
    select id, 
           name, 
           password, 
           employee_id
      from users
     where id = a_id;

    return mycurs;
end;
$body$ language plpgsql;
```

You could call the stored procedure either like so:

```Java
ImmutableUser actual = pgSession.qProcSelectOne(
        expected,
        "{ #{refcursor} = call get_user_by_id(#{getId}) }",
        ImmutableUser.class);
pgSession.rollback();
```

or like so:

```Java
ImmutableUser actual = pgSession.qProcSelectOne(
        "{ #{refcursor} = call get_user_by_id(#{java.util.UUID}) }",
        ImmutableUser.class,
        expected.getId());
pgSession.rollback();
```

Notice the special `#{refcursor}` notation that has to be the first
variable in the SQL string; it tells cl4pg to get a ResultSet from
the function call and process it.

Naturally, we can return lists of things. Assume the following stored
procedure:

```Java
create or replace function get_user_by_gt_emp_id(a_employee_id integer)
returns refcursor
as
$body$
declare
    mycurs refcursor;
begin
    open mycurs for
    select id,
           name,
           password,
           employee_id
      from users
     where employee_id > a_employee_id;

    return mycurs;
end;
$body$ language plpgsql;
```

You would get a list of `ImmutableUser`s by calling this stored
procedure like so:

```Java
List<ImmutableUser> users = pgSession.qProcSelect(
        "{ #{refcursor} = call get_user_by_gt_emp_id(#{java.lang.Integer}) }",
        ImmutableUser.class,
        1);
pgSession.rollback();
```

## Opening and Closing Sessions

For some applications, it will suffice to get one session, hold
it open for the duration of the application, and close it at the end:

```Java
DataSourceAdapter adapter = PgSimpleDataSourceAdapter.buildFromDefaultConfFile();
PgSession pgSession = adapter.getSession();
// do stuff
pgSession.close();
```

For other applications, such as web apps, you will use one of the
data source adapters that uses a connection pool, such as
`HikariCpDataSourceAdapter` or `TomcatJDBCDataSourceAdapter`.
(Feel free to use PgPoolingDataSourceAdapter as well, seeing as
it conveniently wraps the PostgreSQL JDBC driver's `PGPoolingDataSource`.
Just bear in mind that it is not a very sophisticated connection pool.)

For this sort of adapter, your usage will look more like this:

```Java
// This will happen once at application startup
DataSourceAdapter adapter = HikariCpDataSourceAdapter.buildFromDefaultConfFile();

// This will be a typical use:

// Get a session from the connection pool
try (PgSession pgSession = adapter.getSession()) {
    // do stuff
    pgSession.commit();  // Unless you have enabled autocommit
} // Java 7 automatically calls "finally { pgSession.close(); }",
// returning pgSession to the connection pool.
```



