insert into users (
    id,  -- UUID
    name,  -- text
    password,  -- text
    employee_id)  -- int
values (#{getId},
        #{getName},
        #{getPassword},
        #{getEmployeeId})

-- Would have do
-- count the offset
-- find getId()
-- find getId()'s return type
-- call pstmt.set$Type($offset, getId())
