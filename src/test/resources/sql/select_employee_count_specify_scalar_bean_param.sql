  select count(*) as "java.lang.Integer"
    from users
   where employee_id > #{getEmployeeId}
order by employee_id


