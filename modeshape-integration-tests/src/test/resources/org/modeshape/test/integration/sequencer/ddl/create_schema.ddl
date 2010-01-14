CREATE SCHEMA hollywood
    CREATE TABLE films (title varchar(255), release date, producerName varchar(255))
    CREATE VIEW winners AS SELECT title, release FROM films WHERE producerName IS NOT NULL;
