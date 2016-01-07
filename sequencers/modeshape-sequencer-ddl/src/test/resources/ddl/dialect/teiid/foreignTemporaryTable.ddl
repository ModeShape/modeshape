create foreign temporary table x (
  e1 string options (nameinsource 'a'), 
  e2 integer, 
  e3 string, 
  primary key (e1)
) options (
  cardinality 1000, 
  updatable true
) on pm1