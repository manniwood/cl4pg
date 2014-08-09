create or replace function add_and_return(first int, second int)
returns int
immutable
as
$body$
begin
    return first + second;
end;
$body$
language plpgsql;
