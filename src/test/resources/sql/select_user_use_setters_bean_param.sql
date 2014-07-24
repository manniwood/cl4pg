select id          as "setId",
       name        as "setName",
       password    as "setPassword",
       employee_id as "setEmployeeId"
  from users
 where id = #{getId}


