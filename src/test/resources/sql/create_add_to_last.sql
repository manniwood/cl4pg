create or replace function add_to_last(in first int, inout second int)
immutable
as
$body$
begin
    second := first + second;
end;
$body$
language plpgsql;
