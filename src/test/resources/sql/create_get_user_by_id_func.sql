create or replace function get_user_by_id(a_id uuid)
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
     where id = a_id;

    return mycurs;
end;
$body$ language plpgsql;
