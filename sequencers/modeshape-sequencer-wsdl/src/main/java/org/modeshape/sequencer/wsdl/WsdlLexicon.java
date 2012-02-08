/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.sequencer.wsdl;

import org.modeshape.common.annotation.Immutable;
import static org.modeshape.sequencer.wsdl.WsdlLexicon.Namespace.PREFIX;


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
