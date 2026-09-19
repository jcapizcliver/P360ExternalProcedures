select owner, table_name, tablespace_name from all_tables
where owner = 'P360_EXPLOIT'
and table_name in ('TT_PRODUCT_COMPLETENESS_WORK','TB_COMPLETENESS_SNAPSHOT');

select owner, table_name, column_name, data_type, data_length, data_precision, data_scale
from all_tab_columns where owner='P360_EXPLOIT'
and table_name in ('TT_PRODUCT_COMPLETENESS_WORK','TB_COMPLETENESS_SNAPSHOT')
order by table_name,column_id;

select owner, table_name, constraint_name, constraint_type, status
from all_constraints where owner='P360_EXPLOIT'
and table_name in ('TT_PRODUCT_COMPLETENESS_WORK','TB_COMPLETENESS_SNAPSHOT');

select tablespace_name from user_tablespaces
where tablespace_name in ('P360_EXPLOIT_DATA','P360_EXPLOIT_INDEX');
