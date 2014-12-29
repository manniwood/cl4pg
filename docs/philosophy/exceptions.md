# Exceptions

Item 59 of Joshua Bloch's _Effective Java, Second Edition_, is titled 
"Avoid Unnecessary use of checked exceptions". He offers sage advice as to when
checked versus unchecked exceptions should be used.

Databases provide us with plenty of scenarios where checked or unchecked
exceptions would be the most appropriate choice.


The problem is,
whereas the PostgreSQL documentation lists over 
[200 different error codes](www.postgresql.org/docs/9.3/static/errcodes-appendix.html)
JDBC uses SQLException for absolutely all of them, from the most innocuous error,
to the most horrific catastrophe.

Wrong password or database name when trying to connect to your database? SQLException.

Violate a unique constraint? SQLException. 

Database run out of storage? SQLException. 

Violate a not null constraint? SQLException.

Database get nuked from orbit? SQLException.

Obviously, some of these exceptions are more recoverable than others, but to
JDBC, via SQLException, they are all equally exceptional.

The decision in Cl4pg, then, was to make all exceptions unchecked exceptions, *but*
to allow the user control over exactly which unchecked exceptions get thrown, so that
there is more control over what gets caught and what gets ignored as unrecoverable.



