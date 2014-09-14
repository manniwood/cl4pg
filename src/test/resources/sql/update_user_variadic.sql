update users set
    name = #{java.lang.String},
    password = #{java.lang.String},
    employee_id = #{java.lang.Integer}
where
    id = #{java.util.UUID}



