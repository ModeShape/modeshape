CREATE FOREIGN PROCEDURE invoke
(
OUT result xml RESULT,
IN binding string OPTIONS (ANNOTATION 'The invocation binding (HTTP, SOAP11, SOAP12).  May be set or allowed to default to null to use the default binding.'),
IN action string OPTIONS (ANNOTATION 'With a SOAP invocation, action sets the SOAPAction.  With HTTP it sets the HTTP Method (GET, POST - default, etc.).'),
IN request xml OPTIONS (ANNOTATION 'The XML document or root element that represents the request.  If the ExecutionFactory is configured in with a DefaultServiceMode of MESSAGE, then the SOAP request must contain the entire SOAP message.'),
IN endpoint string OPTIONS (ANNOTATION 'The relative or abolute endpoint to use.  May be set or allowed to default to null to use the default endpoint address.'),
IN stream boolean DEFAULT 'false' OPTIONS (ANNOTATION 'If the result should be streamed.')
)
OPTIONS (ANNOTATION 'Invokes a webservice that returns an XML result')

CREATE FOREIGN PROCEDURE invokeHttp
(
OUT result blob RESULT,
IN action string OPTIONS (ANNOTATION 'Sets the HTTP Method (GET, POST - default, etc.).'),
IN request object OPTIONS (ANNOTATION 'The String, XML, BLOB, or CLOB value containing a payload (only for POST).'),
IN endpoint string OPTIONS (ANNOTATION 'The relative or abolute endpoint to use.  May be set or allowed to default to null to use the default endpoint address.'),
IN stream boolean DEFAULT 'false' OPTIONS (ANNOTATION 'If the result should be streamed.'),
OUT contentType string
)
OPTIONS (ANNOTATION 'Invokes a webservice that returns an binary result')