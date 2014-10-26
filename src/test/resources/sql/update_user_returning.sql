update users set
    name = #{getName},
    password = #{getPassword},
    employee_id = #{getEmployeeId}
 where id = #{getId}
returning id,
          name,
          password,
          employee_id

