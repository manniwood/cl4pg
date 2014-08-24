insert into users (
    id,  -- UUID
    name,  -- text
    password,  -- text
    employee_id)  -- int
values (#{java.util.UUID},
        #{java.lang.String},
        #{java.lang.String},
        #{java.lang.Integer})

