select id          as "java.util.UUID",
       name        as "java.lang.String",
       password    as "java.lang.String,
       employee_id as "int"
  from users
 where id = #{java.util.UUID}


