CREATE FOREIGN TABLE BookingCollection (
	carrid string(3) NOT NULL,
	connid string(4) NOT NULL,
	fldate timestamp NOT NULL,
	bookid string(8) NOT NULL,
	CUSTOMID string(8) NOT NULL,
	CUSTTYPE string(1) NOT NULL,
	SMOKER string(1) NOT NULL,
	WUNIT string(3) NOT NULL,
	LUGGWEIGHT bigdecimal NOT NULL,
	INVOICE string(1) NOT NULL,
	CLASS string(1) NOT NULL,
	FORCURAM bigdecimal NOT NULL,
	FORCURKEY string(5) NOT NULL,
	LOCCURAM bigdecimal NOT NULL,
	LOCCURKEY string(5) NOT NULL,
	ORDER_DATE timestamp NOT NULL,
	COUNTER string(8) NOT NULL,
	AGENCYNUM string(8) NOT NULL,
	CANCELLED string(1) NOT NULL,
	RESERVED string(1) NOT NULL,
	PASSNAME string(25) NOT NULL,
	PASSFORM string(15) NOT NULL,
	PASSBIRTH timestamp NOT NULL,
	PRIMARY KEY(carrid, connid, fldate, bookid),
	CONSTRAINT BookingFlight FOREIGN KEY(fldate, connid, carrid) REFERENCES FlightCollection (fldate, connid, carrid),
	CONSTRAINT FlightBookings FOREIGN KEY(fldate, connid, carrid) REFERENCES FlightCollection (fldate, connid, carrid)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Booking');

CREATE FOREIGN TABLE CarrierCollection (
	carrid string(3) NOT NULL,
	CARRNAME string(20) NOT NULL,
	CURRCODE string(5) NOT NULL,
	URL string(255) NOT NULL,
	mimeType string(128) NOT NULL,
	PRIMARY KEY(carrid)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Carrier');

CREATE FOREIGN TABLE FlightCollection (
	flightDetails_countryFrom string(3) NOT NULL OPTIONS (NAMEINSOURCE 'countryFrom', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_cityFrom string(20) NOT NULL OPTIONS (NAMEINSOURCE 'cityFrom', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_airportFrom string(3) NOT NULL OPTIONS (NAMEINSOURCE 'airportFrom', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_countryTo string(3) NOT NULL OPTIONS (NAMEINSOURCE 'countryTo', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_cityTo string(20) NOT NULL OPTIONS (NAMEINSOURCE 'cityTo', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_airportTo string(3) NOT NULL OPTIONS (NAMEINSOURCE 'airportTo', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_flightTime integer NOT NULL OPTIONS (NAMEINSOURCE 'flightTime', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_departureTime time NOT NULL OPTIONS (NAMEINSOURCE 'departureTime', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_arrivalTime time NOT NULL OPTIONS (NAMEINSOURCE 'arrivalTime', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_distance bigdecimal NOT NULL OPTIONS (NAMEINSOURCE 'distance', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_distanceUnit string(3) NOT NULL OPTIONS (NAMEINSOURCE 'distanceUnit', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_flightType string(1) NOT NULL OPTIONS (NAMEINSOURCE 'flightType', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	flightDetails_period byte NOT NULL OPTIONS (NAMEINSOURCE 'period', "teiid_odata:ColumnGroup" 'flightDetails', "teiid_odata:ComplexType" 'FlightDetails'),
	carrid string(3) NOT NULL,
	connid string(4) NOT NULL,
	fldate timestamp NOT NULL,
	PRICE bigdecimal NOT NULL,
	CURRENCY string(5) NOT NULL,
	PLANETYPE string(10) NOT NULL,
	SEATSMAX integer NOT NULL,
	SEATSOCC integer NOT NULL,
	PAYMENTSUM bigdecimal NOT NULL,
	SEATSMAX_B integer NOT NULL,
	SEATSOCC_B integer NOT NULL,
	SEATSMAX_F integer NOT NULL,
	SEATSOCC_F integer NOT NULL,
	PRIMARY KEY(carrid, connid, fldate),
	CONSTRAINT CarrierToFlight FOREIGN KEY(carrid) REFERENCES CarrierCollection 
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Flight');

CREATE FOREIGN TABLE NotificationCollection (
	ID string(32) NOT NULL,
	collection string(40) NOT NULL,
	title string NOT NULL,
	updated timestamp NOT NULL,
	changeType string(30) NOT NULL,
	entriesOfInterest integer NOT NULL,
	recipient string(112) NOT NULL,
	PRIMARY KEY(ID)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Notification');

CREATE FOREIGN TABLE SubscriptionCollection (
	ID string(32) NOT NULL,
	"user" string(12) NOT NULL,
	updated timestamp NOT NULL,
	title string(255) NOT NULL,
	deliveryAddress string NOT NULL,
	persistNotifications boolean NOT NULL,
	collection string(40) NOT NULL,
	"filter" string NOT NULL,
	"select" string(255) NOT NULL,
	changeType string(30) NOT NULL,
	PRIMARY KEY(ID)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Subscription');

CREATE FOREIGN TABLE TravelAgencies (
	agencynum string(8) NOT NULL,
	NAME string(25) NOT NULL,
	STREET string(30) NOT NULL,
	POSTBOX string(10) NOT NULL,
	POSTCODE string(10) NOT NULL,
	CITY string(25) NOT NULL,
	COUNTRY string(3) NOT NULL,
	REGION string(3) NOT NULL,
	TELEPHONE string(30) NOT NULL,
	URL string(255) NOT NULL,
	LANGU string(2) NOT NULL,
	CURRENCY string(5) NOT NULL,
	mimeType string(128) NOT NULL,
	PRIMARY KEY(agencynum)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Travelagency');

CREATE FOREIGN TABLE TravelagencyCollection (
	agencynum string(8) NOT NULL,
	NAME string(25) NOT NULL,
	STREET string(30) NOT NULL,
	POSTBOX string(10) NOT NULL,
	POSTCODE string(10) NOT NULL,
	CITY string(25) NOT NULL,
	COUNTRY string(3) NOT NULL,
	REGION string(3) NOT NULL,
	TELEPHONE string(30) NOT NULL,
	URL string(255) NOT NULL,
	LANGU string(2) NOT NULL,
	CURRENCY string(5) NOT NULL,
	mimeType string(128) NOT NULL,
	PRIMARY KEY(agencynum)
) OPTIONS (UPDATABLE TRUE, "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Travelagency');

CREATE FOREIGN PROCEDURE CheckFlightAvailability(IN airlineid string, IN connectionid string, IN flightdate timestamp) RETURNS TABLE (ECONOMAX integer, ECONOFREE integer, BUSINMAX integer, BUSINFREE integer, FIRSTMAX integer, FIRSTFREE integer)
OPTIONS ("teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.FlightAvailability', "teiid_odata:HttpMethod" 'GET')

CREATE FOREIGN PROCEDURE GetAgencyDetails(IN agency_id string) RETURNS TABLE (agencynum string, NAME string, STREET string, POSTBOX string, POSTCODE string, CITY string, COUNTRY string, REGION string, TELEPHONE string, URL string, LANGU string, CURRENCY string, mimeType string)
OPTIONS ("teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Travelagency', "teiid_odata:HttpMethod" 'GET')

CREATE FOREIGN PROCEDURE GetAvailableFlights(IN fromdate timestamp, IN todate timestamp, IN cityfrom string, IN cityto string) RETURNS TABLE (flightDetails_countryFrom string, flightDetails_cityFrom string, flightDetails_airportFrom string, flightDetails_countryTo string, flightDetails_cityTo string, flightDetails_airportTo string, flightDetails_flightTime integer, flightDetails_departureTime time, flightDetails_arrivalTime time, flightDetails_distance bigdecimal, flightDetails_distanceUnit string, flightDetails_flightType string, flightDetails_period byte, carrid string, connid string, fldate timestamp, PRICE bigdecimal, CURRENCY string, PLANETYPE string, SEATSMAX integer, SEATSOCC integer, PAYMENTSUM bigdecimal, SEATSMAX_B integer, SEATSOCC_B integer, SEATSMAX_F integer, SEATSOCC_F integer)
OPTIONS ("teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Flight', "teiid_odata:HttpMethod" 'GET')

CREATE FOREIGN PROCEDURE GetFlightDetails(IN airlineid string, IN connectionid string) RETURNS TABLE (countryFrom string, cityFrom string, airportFrom string, countryTo string, cityTo string, airportTo string, flightTime integer, departureTime time, arrivalTime time, distance bigdecimal, distanceUnit string, flightType string, period byte)
OPTIONS ("teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.FlightDetails', "teiid_odata:HttpMethod" 'GET')

CREATE FOREIGN PROCEDURE UpdateAgencyPhoneNo(IN agency_id string, IN telephone string) RETURNS TABLE (agencynum string, NAME string, STREET string, POSTBOX string, POSTCODE string, CITY string, COUNTRY string, REGION string, TELEPHONE string, URL string, LANGU string, CURRENCY string, mimeType string)
OPTIONS ("teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.Travelagency', "teiid_odata:HttpMethod" 'PUT')
