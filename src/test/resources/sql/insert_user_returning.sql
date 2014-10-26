insert into users (
    id,  -- UUID
    name,  -- text
    password,  -- text
    employee_id)  -- int
values (#{getId},
        #{getName},
        #{getPassword},
        #{getEmployeeId})
returning id,
          name,
          password,
          employee_id

