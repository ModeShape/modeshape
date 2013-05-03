CREATE FOREIGN PROCEDURE getFiles
(
IN pathAndPattern string OPTIONS (ANNOTATION 'The path and pattern of what files to return.  Currently the only pattern supported is *.<ext>, which returns only the files matching the given extension at the given path.')
)
RETURNS TABLE (file blob, filePath string)
OPTIONS (ANNOTATION 'Returns files that match the given path and pattern as BLOBs')

CREATE FOREIGN PROCEDURE getTextFiles
(
IN pathAndPattern string OPTIONS (ANNOTATION 'The path and pattern of what files to return.  Currently the only pattern supported is *.<ext>, which returns only the files matching the given extension at the given path.')
)
RETURNS TABLE (file clob, filePath string)
OPTIONS (ANNOTATION 'Returns text files that match the given path and pattern as CLOBs')

CREATE FOREIGN PROCEDURE saveFile
(
IN filePath string,
IN file object OPTIONS (ANNOTATION 'The contents to save.  Can be one of CLOB, BLOB, or XML')
)
OPTIONS (ANNOTATION 'Saves the given value to the given path.  Any existing file will be overriden.')