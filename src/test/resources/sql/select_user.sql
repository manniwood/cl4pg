select id,
       name,
       password,
       employee_id
  from users
 where id = #{java.util.UUID}


