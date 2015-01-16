create or replace function get_user_by_gt_emp_id(a_employee_id integer)
returns refcursor
as
$body$
declare
    mycurs refcursor;
begin
    open mycurs for
    select id,
           name,
           password,
           employee_id
      from users
     where employee_id > a_employee_id;

    return mycurs;
end;
$body$ language plpgsql;
