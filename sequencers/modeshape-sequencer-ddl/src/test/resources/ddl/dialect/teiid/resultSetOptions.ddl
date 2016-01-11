CREATE FOREIGN PROCEDURE SourceProc (
    IN airlineid string, 
    IN connectionid string, 
    IN flightdate timestamp ) 
RETURNS 
    OPTIONS (
        ANNOTATION 'procedure result desc', 
        UPDATECOUNT '2' )
    TABLE (
        ECONOMAX integer, 
        ECONOFREE integer, 
        BUSINMAX integer, 
        BUSINFREE integer, 
        FIRSTMAX integer, 
        FIRSTFREE integer )
OPTIONS (
    "teiid_odata:EntityType" 'RMTSAMPLEFLIGHT.FlightAvailability', 
    "teiid_odata:HttpMethod" 'GET' )

CREATE VIRTUAL FUNCTION SourceFunc (
    flag boolean, 
    msg varchar ) 
RETURNS 
    OPTIONS (
        ANNOTATION 'function result desc' )
    varchar 
OPTIONS (
    CATEGORY 'misc', 
    DETERMINISM 'DETERMINISTIC', 
    "NULL-ON-NULL" 'true', 
    JAVA_CLASS 'foo', 
    JAVA_METHOD 'bar', 
    RANDOM 'any', 
    UUID 'x' )