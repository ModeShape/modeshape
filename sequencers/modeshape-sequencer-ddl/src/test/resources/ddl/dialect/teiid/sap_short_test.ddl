CREATE FOREIGN TABLE BookingCollection (
	carrid string(3) NOT NULL,
	connid string(4) NOT NULL,
	fldate timestamp NOT NULL,
	bookid string(8) NOT NULL,
	PRIMARY KEY(carrid),
	CONSTRAINT BookingFlight FOREIGN KEY(carrid) REFERENCES FlightCollection (carrid),
	CONSTRAINT FlightBookings FOREIGN KEY(carrid) REFERENCES FlightCollection (carrid)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Booking');

CREATE FOREIGN TABLE CarrierCollection (
	carrid string(3) NOT NULL,
	CARRNAME string(20) NOT NULL,
	PRIMARY KEY(carrid)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Carrier');

CREATE FOREIGN TABLE FlightCollection (

	carrid string(3) NOT NULL,
	connid string(4) NOT NULL,
	fldate timestamp NOT NULL,
	PRICE bigdecimal NOT NULL,
	CURRENCY string(5) NOT NULL,
	PRIMARY KEY(carrid),
	CONSTRAINT CarrierToFlight FOREIGN KEY(carrid) REFERENCES CarrierCollection 
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Flight');
