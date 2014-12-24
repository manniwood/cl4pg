There are already other ORMs / SQL mappers in Java. Ones that

- [write SQL automagically](http://hibernate.org/orm/)
- [provide powerful ways to write and map SQL](http://mybatis.github.io/mybatis-3/)
- [provide fluent ways to map and write SQL](http://jdbi.org/)

They all make no assumptions about the database that will be used, and therefore
have to be somewhat database-agnostic (though the SQL-based ones at least allow
using your database's SQL dialect more directly).

Cl4pg assumes you have picked [PostgreSQL](http://www.postgresql.org/) and are
sticking with it. You are looking for a library that does not hide PostgreSQL's
unique features, but instead makes them easy to use.
