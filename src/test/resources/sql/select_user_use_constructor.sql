select id          as "java.util.UUID",
       name        as "java.lang.String",
       password    as "java.lang.String",
       employee_id as "java.lang.Integer"
  from users
 where id = #{java.util.UUID}


