create or replace function swap_them(inout first int, inout second int)
immutable
as
$body$
declare
    tmp int;
begin
    tmp := first;
    first := second;
    second := tmp;
end;
$body$
language plpgsql;
