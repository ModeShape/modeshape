/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.sequencer.wsdl;

import static org.modeshape.sequencer.wsdl.WsdlLexicon.Namespace.PREFIX;
import org.modeshape.common.annotation.Immutable;


/**
 * A lexicon of names used within the XSD sequencer.
 */
@Immutable
public class WsdlLexicon {
    public static class Namespace {
        public static final String URI = "http://schemas.xmlsoap.org/wsdl/";
        public static final String PREFIX = "wsdl";
    }

    public static final String WSDL_DOCUMENT = PREFIX + ":wsdlDocument";
    public static final String SCHEMA = PREFIX + ":schema";
    public static final String IMPORTED_XSD = PREFIX + ":importedXsd";
    public static final String INCLUDED_XSD = PREFIX + ":includedXsd";
    public static final String REDEFINED_XSD = PREFIX + ":redefinedXsd";

    public static final String NC_NAME = PREFIX + ":ncName";
    public static final String NAMESPACE = PREFIX + ":namespace";

    public static final String MESSAGES = PREFIX + ":messages";
    public static final String MESSAGE = PREFIX + ":message";
    public static final String PART = PREFIX + ":part";
    public static final String ELEMENT_REFERENCE = PREFIX + ":element";
    public static final String ELEMENT_NAME = PREFIX + ":elementName";
    public static final String ELEMENT_NAMESPACE = PREFIX + ":elementNamespace";
    public static final String TYPE_REFERENCE = PREFIX + ":type";
    public static final String TYPE_NAME = PREFIX + ":typeName";
    public static final String TYPE_NAMESPACE = PREFIX + ":typeNamespace";

    public static final String OPERATION = PREFIX + ":operation";
    public static final String INPUT = PREFIX + ":input";
    public static final String OUTPUT = PREFIX + ":output";
    public static final String FAULT = PREFIX + ":fault";
    public static final String MESSAGE_REFERENCE = PREFIX + ":message";
    public static final String MESSAGE_NAME = PREFIX + ":messageName";
    public static final String MESSAGE_NAMESPACE = PREFIX + ":messageNamespace";

    public static final String OPERATION_INPUT = PREFIX + ":operationInput";
    public static final String OPERATION_OUTPUT = PREFIX + ":operationOutput";
    public static final String PARAMETER_ORDER = PREFIX + ":parameterOrder";

    public static final String PORT_TYPES = PREFIX + ":portTypes";
    public static final String PORT_TYPE = PREFIX + ":portType";

    public static final String BINDINGS = PREFIX + ":bindings";
    public static final String BINDING = PREFIX + ":binding";
    public static final String BINDING_OPERATION = PREFIX + ":bindingOperation";
    public static final String BINDING_OPERATION_INPUT = PREFIX + ":bindingOperationInput";
    public static final String BINDING_OPERATION_OUTPUT = PREFIX + ":bindingOperationOutput";
    public static final String BINDING_OPERATION_FAULT = PREFIX + ":bindingOperationFault";
    public static final String INPUT_REFERENCE = PREFIX + ":input";
    public static final String INPUT_NAME = PREFIX + ":inputName";
    public static final String OUTPUT_REFERENCE = PREFIX + ":output";
    public static final String OUTPUT_NAME = PREFIX + ":outputName";

    public static final String SERVICES = PREFIX + ":services";
    public static final String SERVICE = PREFIX + ":service";
    public static final String PORT = PREFIX + ":port";
    public static final String BINDING_REFERENCE = PREFIX + ":binding";
    public static final String BINDING_NAME = PREFIX + ":bindingName";
    public static final String BINDING_NAMESPACE = PREFIX + ":bindingNamespace";

    /*
     * SOAP Extensions
     */

    public static final String SOAP_ADDRESS = PREFIX + ":soapAddress";
    public static final String SOAP_LOCATION = PREFIX + ":soapLocation";

    public static final String SOAP_BINDING = PREFIX + ":soapBinding";
    public static final String STYLE = PREFIX + ":style";
    public static final String TRANSPORT = PREFIX + ":transport";

    public static final String SOAP_OPERATION = PREFIX + ":soapOperation";
    public static final String SOAP_ACTION = PREFIX + ":soapAction";

    public static final String SOAP_BODY = PREFIX + ":soapBody";
    public static final String ENCODING_STYLE = PREFIX + ":encodingStyle";
    public static final String PARTS = PREFIX + ":parts";
    public static final String USE = PREFIX + ":use";

    public static final String SOAP_FAULT = PREFIX + ":soapFault";
    public static final String SOAP_HEADER = PREFIX + ":soapHeader";
    public static final String SOAP_HEADER_FAULT = PREFIX + ":soapHeaderFault";

    /*
     * MIME Extensions
     */

    public static final String MIME_MULTIPART_RELATED = PREFIX + ":mimeMultipartRelated";
    public static final String MIME_PART = PREFIX + ":mimePart";
    public static final String MIME_TYPE = PREFIX + ":mimeType";
    public static final String MIME_CONTENT = PREFIX + ":mimeContent";
    public static final String MIME_XML = PREFIX + ":mimeXml";

    /*
     * HTTP Extensions
     */

    public static final String HTTP_URL_REPLACEMENT = PREFIX + ":httpUrlReplacement";
    public static final String HTTP_URL_ENCODED = PREFIX + ":httpUrlEncoded";
    public static final String HTTP_ADDRESS = PREFIX + ":httpAddress";
    public static final String HTTP_BINDING = PREFIX + ":httpBinding";
    public static final String HTTP_OPERATION = PREFIX + ":httpOperation";
    public static final String LOCATION = PREFIX + ":location";
    public static final String VERB = PREFIX + ":verb";

}
