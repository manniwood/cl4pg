  select id,
         name,
         password,
         employee_id
    from users
   where employee_id > #{getEmployeeId}
order by employee_id


