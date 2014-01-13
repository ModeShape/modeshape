-- ModeShape (http://www.modeshape.org)
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--       http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.

CREATE TABLE IF NOT EXISTS chain (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL);
CREATE TABLE IF NOT EXISTS area (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, CHAIN_ID INT NOT NULL);
CREATE TABLE IF NOT EXISTS region (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, AREA_ID INT NOT NULL);
CREATE TABLE IF NOT EXISTS district (ID INT NOT NULL PRIMARY KEY, NAME VARCHAR(30) NOT NULL, REGION_ID INT NOT NULL);

CREATE TABLE IF NOT EXISTS sales (ID INT NOT NULL, SALES_DATE DATE NOT NULL, DISTRICT_ID INT NOT NULL, AMOUNT INT NULL);

ALTER TABLE sales ADD CONSTRAINT PK_SALES PRIMARY KEY (ID, SALES_DATE);

ALTER TABLE area ADD CONSTRAINT FK_CHAIN FOREIGN KEY(CHAIN_ID) REFERENCES CHAIN(ID) ON DELETE CASCADE;
ALTER TABLE region ADD CONSTRAINT FK_AREA FOREIGN KEY(AREA_ID) REFERENCES AREA(ID) ON DELETE CASCADE;
ALTER TABLE district ADD CONSTRAINT FK_REGION FOREIGN KEY(REGION_ID) REFERENCES REGION(ID) ON DELETE CASCADE;