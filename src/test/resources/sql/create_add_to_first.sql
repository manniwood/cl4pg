create or replace function add_to_first(inout first int, in second int)
immutable
as
$body$
begin
    first := first + second;
end;
$body$
language plpgsql;
