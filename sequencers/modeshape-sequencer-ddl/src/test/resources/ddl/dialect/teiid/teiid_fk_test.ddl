CREATE FOREIGN TABLE table_1 (
	column_1 string(3) NOT NULL,
	column_2 string(4) NOT NULL,
	column_3 timestamp NOT NULL,
	column_4 string(8) NOT NULL,
	PRIMARY KEY(column_1, column_2, column_3, column_4),
	CONSTRAINT fk_to_table_2 FOREIGN KEY(column_1, column_2, column_3) REFERENCES table_2 (column_1, column_2, column_2),
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Booking');

CREATE FOREIGN TABLE table_3 (
	column_1 string(3) NOT NULL,
	CARRNAME string(20) NOT NULL,
	PRIMARY KEY(column_1)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Carrier');

CREATE FOREIGN TABLE table_2 {
	column_1 string(3) NOT NULL,
	column_2 string(4) NOT NULL,
	column_3 timestamp NOT NULL,
	column_5 bigdecimal NOT NULL,
	PRIMARY KEY(column_1, column_2, column_3),
	CONSTRAINT fk_to_table_3 FOREIGN KEY(column_1) REFERENCES table_3 
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Flight');