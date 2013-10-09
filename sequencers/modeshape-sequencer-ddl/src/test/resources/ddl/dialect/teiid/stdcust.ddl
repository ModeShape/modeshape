CREATE TABLE customer
(
   CustomerId varchar(12) PRIMARY KEY NOT NULL,
   FirstName varchar(25),
   LastName varchar(25),
   MiddleName varchar(15),
   StreetAddress varchar(50),
   StreetAddress2 varchar(25),
   City varchar(25),
   StateProvince varchar(25),
   PostalCode varchar(15),
   Country varchar(15),
   PhoneNumber varchar(30)
)
;


CREATE TABLE account
(
   AccountID decimal(10,0) PRIMARY KEY NOT NULL,
   CustomerId varchar(12) NOT NULL,
   AccountType varchar(10) NOT NULL,
   AccountStatus varchar(10) NOT NULL,
   DateOpened timestamp,
   DateClosed timestamp
)
;
ALTER TABLE account
ADD CONSTRAINT FK_Account_CustId
FOREIGN KEY (CustomerId)
REFERENCES customer(CustomerId)
;


CREATE TABLE accountholdings
(
   TransactionID varchar(15) PRIMARY KEY NOT NULL,
   AccountID decimal(10,0) NOT NULL,
   ProductID varchar(12) NOT NULL,
   PurchaseDate timestamp,
   ProductShares decimal(10,5) NOT NULL
)
;
ALTER TABLE accountholdings
ADD CONSTRAINT FK_AcctHoldings_AcctID
FOREIGN KEY (AccountID)
REFERENCES account(AccountID)
;

