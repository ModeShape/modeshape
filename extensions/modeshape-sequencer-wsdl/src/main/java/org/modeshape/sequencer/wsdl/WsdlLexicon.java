package org.modeshape.sequencer.wsdl;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within the XSD sequencer.
 */
@Immutable
public class WsdlLexicon {
    public static class Namespace {
        public static final String URI = "http://schemas.xmlsoap.org/wsdl/";
        public static final String PREFIX = "wsdl";
    }

    public static final Name WSDL_DOCUMENT = new BasicName(Namespace.URI, "wsdlDocument");
    public static final Name SCHEMA = new BasicName(Namespace.URI, "schema");
    public static final Name IMPORTED_XSD = new BasicName(Namespace.URI, "importedXsd");
    public static final Name INCLUDED_XSD = new BasicName(Namespace.URI, "includedXsd");
    public static final Name REDEFINED_XSD = new BasicName(Namespace.URI, "redefinedXsd");

    public static final Name NC_NAME = new BasicName(Namespace.URI, "ncName");
    public static final Name NAMESPACE = new BasicName(Namespace.URI, "namespace");

    public static final Name MESSAGES = new BasicName(Namespace.URI, "messages");
    public static final Name MESSAGE = new BasicName(Namespace.URI, "message");
    public static final Name PART = new BasicName(Namespace.URI, "part");
    public static final Name ELEMENT_REFERENCE = new BasicName(Namespace.URI, "element");
    public static final Name ELEMENT_NAME = new BasicName(Namespace.URI, "elementName");
    public static final Name ELEMENT_NAMESPACE = new BasicName(Namespace.URI, "elementNamespace");
    public static final Name TYPE_REFERENCE = new BasicName(Namespace.URI, "type");
    public static final Name TYPE_NAME = new BasicName(Namespace.URI, "typeName");
    public static final Name TYPE_NAMESPACE = new BasicName(Namespace.URI, "typeNamespace");

    public static final Name OPERATION = new BasicName(Namespace.URI, "operation");
    public static final Name INPUT = new BasicName(Namespace.URI, "input");
    public static final Name OUTPUT = new BasicName(Namespace.URI, "output");
    public static final Name FAULT = new BasicName(Namespace.URI, "fault");
    public static final Name MESSAGE_REFERENCE = new BasicName(Namespace.URI, "message");
    public static final Name MESSAGE_NAME = new BasicName(Namespace.URI, "messageName");
    public static final Name MESSAGE_NAMESPACE = new BasicName(Namespace.URI, "messageNamespace");

    public static final Name OPERATION_INPUT = new BasicName(Namespace.URI, "operationInput");
    public static final Name OPERATION_OUTPUT = new BasicName(Namespace.URI, "operationOutput");
    public static final Name PARAMETER_ORDER = new BasicName(Namespace.URI, "parameterOrder");

    public static final Name PORT_TYPES = new BasicName(Namespace.URI, "portTypes");
    public static final Name PORT_TYPE = new BasicName(Namespace.URI, "portType");

    public static final Name BINDINGS = new BasicName(Namespace.URI, "bindings");
    public static final Name BINDING = new BasicName(Namespace.URI, "binding");
    public static final Name BINDING_OPERATION = new BasicName(Namespace.URI, "bindingOperation");
    public static final Name BINDING_OPERATION_INPUT = new BasicName(Namespace.URI, "bindingOperationInput");
    public static final Name BINDING_OPERATION_OUTPUT = new BasicName(Namespace.URI, "bindingOperationOutput");
    public static final Name BINDING_OPERATION_FAULT = new BasicName(Namespace.URI, "bindingOperationFault");
    public static final Name INPUT_REFERENCE = new BasicName(Namespace.URI, "input");
    public static final Name INPUT_NAME = new BasicName(Namespace.URI, "inputName");
    public static final Name OUTPUT_REFERENCE = new BasicName(Namespace.URI, "output");
    public static final Name OUTPUT_NAME = new BasicName(Namespace.URI, "outputName");

    public static final Name SERVICES = new BasicName(Namespace.URI, "services");
    public static final Name SERVICE = new BasicName(Namespace.URI, "service");
    public static final Name PORT = new BasicName(Namespace.URI, "port");
    public static final Name BINDING_REFERENCE = new BasicName(Namespace.URI, "binding");
    public static final Name BINDING_NAME = new BasicName(Namespace.URI, "bindingName");
    public static final Name BINDING_NAMESPACE = new BasicName(Namespace.URI, "bindingNamespace");

    /*
     * SOAP Extensions
     */

    public static final Name SOAP_ADDRESS = new BasicName(Namespace.URI, "soapAddress");
    public static final Name SOAP_LOCATION = new BasicName(Namespace.URI, "soapLocation");

    public static final Name SOAP_BINDING = new BasicName(Namespace.URI, "soapBinding");
    public static final Name STYLE = new BasicName(Namespace.URI, "style");
    public static final Name TRANSPORT = new BasicName(Namespace.URI, "transport");

    public static final Name SOAP_OPERATION = new BasicName(Namespace.URI, "soapOperation");
    public static final Name SOAP_ACTION = new BasicName(Namespace.URI, "soapAction");

    public static final Name SOAP_BODY = new BasicName(Namespace.URI, "soapBody");
    public static final Name ENCODING_STYLE = new BasicName(Namespace.URI, "encodingStyle");
    public static final Name PARTS = new BasicName(Namespace.URI, "parts");
    public static final Name USE = new BasicName(Namespace.URI, "use");

    public static final Name SOAP_FAULT = new BasicName(Namespace.URI, "soapFault");
    public static final Name SOAP_HEADER = new BasicName(Namespace.URI, "soapHeader");
    public static final Name SOAP_HEADER_FAULT = new BasicName(Namespace.URI, "soapHeaderFault");

    /*
     * MIME Extensions
     */

    public static final Name MIME_MULTIPART_RELATED = new BasicName(Namespace.URI, "mimeMultipartRelated");
    public static final Name MIME_PART = new BasicName(Namespace.URI, "mimePart");
    public static final Name MIME_TYPE = new BasicName(Namespace.URI, "mimeType");
    public static final Name MIME_CONTENT = new BasicName(Namespace.URI, "mimeContent");
    public static final Name MIME_XML = new BasicName(Namespace.URI, "mimeXml");

    /*
     * HTTP Extensions
     */

    public static final Name HTTP_URL_REPLACEMENT = new BasicName(Namespace.URI, "httpUrlReplacement");
    public static final Name HTTP_URL_ENCODED = new BasicName(Namespace.URI, "httpUrlEncoded");
    public static final Name HTTP_ADDRESS = new BasicName(Namespace.URI, "httpAddress");
    public static final Name HTTP_BINDING = new BasicName(Namespace.URI, "httpBinding");
    public static final Name HTTP_OPERATION = new BasicName(Namespace.URI, "httpOperation");
    public static final Name LOCATION = new BasicName(Namespace.URI, "location");
    public static final Name VERB = new BasicName(Namespace.URI, "verb");

}
