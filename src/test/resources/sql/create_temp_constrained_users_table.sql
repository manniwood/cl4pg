create temporary table users (
    id uuid constraint users_pk primary key not null,
    name text not null,
    password text not null,
    employee_id int constraint users_employee_id_uniq not null);

