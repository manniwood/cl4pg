delete from users
 where id = #{getId}
returning id,
          name,
          password,
          employee_id

