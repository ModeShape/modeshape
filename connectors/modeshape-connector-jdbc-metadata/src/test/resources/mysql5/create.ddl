-- ModeShape (http://www.modeshape.org)
-- See the COPYRIGHT.txt file distributed with this work for information
-- regarding copyright ownership.  Some portions may be licensed
-- to Red Hat, Inc. under one or more contributor license agreements.
-- See the AUTHORS.txt file in the distribution for a full listing of 
-- individual contributors. 
--
-- ModeShape is free software. Unless otherwise indicated, all code in ModeShape
-- is licensed to you under the terms of the GNU Lesser General Public License as
-- published by the Free Software Foundation; either version 2.1 of
-- the License, or (at your option) any later version.
--
-- ModeShape is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
-- Lesser General Public License for more details.
--
-- You should have received a copy of the GNU Lesser General Public
-- License along with this software; if not, write to the Free
-- Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
-- 02110-1301 USA, or see the FSF site: http://www.fsf.org.

CREATE TABLE IF NOT EXISTS chain (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL);
CREATE TABLE IF NOT EXISTS area (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, CHAIN_ID INT NOT NULL);
CREATE TABLE IF NOT EXISTS region (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, AREA_ID INT NOT NULL);
CREATE TABLE IF NOT EXISTS district (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, REGION_ID INT NOT NULL);

CREATE TABLE IF NOT EXISTS sales (ID INT NOT NULL, SALES_DATE DATE NOT NULL, DISTRICT_ID INT NOT NULL, AMOUNT INT NULL);

ALTER TABLE sales ADD CONSTRAINT PK_SALES PRIMARY KEY (ID, SALES_DATE);

ALTER TABLE area ADD CONSTRAINT FK_CHAIN FOREIGN KEY(CHAIN_ID) REFERENCES CHAIN(ID) ON DELETE CASCADE;
ALTER TABLE region ADD CONSTRAINT FK_AREA FOREIGN KEY(AREA_ID) REFERENCES AREA(ID) ON DELETE CASCADE;
ALTER TABLE district ADD CONSTRAINT FK_REGION FOREIGN KEY(REGION_ID) REFERENCES REGION(ID) ON DELETE CASCADE;