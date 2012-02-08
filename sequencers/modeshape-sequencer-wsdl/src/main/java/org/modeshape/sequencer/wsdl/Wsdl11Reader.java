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

import com.ibm.wsdl.Constants;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.WSDLElement;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.http.HTTPOperation;
import javax.wsdl.extensions.http.HTTPUrlEncoded;
import javax.wsdl.extensions.http.HTTPUrlReplacement;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMimeXml;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPHeaderFault;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.extensions.soap12.SOAP12Body;
import javax.wsdl.extensions.soap12.SOAP12Fault;
import javax.wsdl.extensions.soap12.SOAP12Header;
import javax.wsdl.extensions.soap12.SOAP12HeaderFault;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.wsdl.xml.WSDLLocator;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.api.mimetype.MimeTypeConstants;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.sramp.NamespaceEntityResolver;
import org.modeshape.sequencer.sramp.SrampLexicon;
import org.modeshape.sequencer.sramp.SymbolSpace;
import org.modeshape.sequencer.xsd.XsdLexicon;
import org.modeshape.sequencer.xsd.XsdReader;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that can parse WSDL 1.1 definitions and derive a node structure from the content
 */
@NotThreadSafe
public class Wsdl11Reader extends WsdlReader<javax.wsdl.Definition> {

    protected static final SymbolSpace MESSAGES = new SymbolSpace("Messages");
    protected static final SymbolSpace PORT_TYPES = new SymbolSpace("PortTypes");
    protected static final SymbolSpace BINDINGS = new SymbolSpace("Bindings");
    protected static final SymbolSpace PORTS = new SymbolSpace("Ports");
    protected static final SymbolSpace SERVICES = new SymbolSpace("Services");


    protected Map<WSDLElement, String> uuidForComponent;
    protected Map<WSDLElement, String> nameForComponent;

    public Wsdl11Reader( Sequencer.Context context,
                         String baseUri ) {
        super(context, baseUri);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Definition parse( InputSource source ) throws Exception {
        WSDLReader wsdlReader = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader();
        wsdlReader.setFeature(Constants.FEATURE_VERBOSE, false);
        wsdlReader.setFeature(Constants.FEATURE_IMPORT_DOCUMENTS, false);
        // We use a custom WSDLLocator that never tries to resolve the import locations for WSDLs or XSDs.
        WSDLLocator locator = new CustomWSDLLocator(source, baseUri);
        return wsdlReader.readWSDL(locator);
    }

    @Override
    @SuppressWarnings( "unchecked" )
    protected void process( Definition definition,
                            javax.jcr.Node outputNode,
                            long sizeOfFile ) throws Exception {
        uuidForComponent = new HashMap<WSDLElement, String>();
        nameForComponent = new HashMap<WSDLElement, String>();

        outputNode.setProperty(SrampLexicon.CONTENT_TYPE, MimeTypeConstants.WSDL);
        outputNode.setProperty(SrampLexicon.CONTENT_SIZE, sizeOfFile);

        processDocumentation(definition, outputNode);
        processExtensibilityElements(definition, outputNode);

        processTypes(definition.getTypes(), outputNode);
        processMessages(definition.getMessages(), outputNode);
        processPortTypes(definition.getPortTypes(), outputNode);
        processBindings(definition.getBindings(), outputNode);
        processServices(definition.getServices(), outputNode);
    }

    protected void processTypes( Types types,
                                 Node parentNode ) throws Exception {
        if (types == null) {
            return;
        }
        XsdReader xsdReader = new XsdReader(context);
        for (Object obj : types.getExtensibilityElements()) {
            if (obj instanceof Schema) {
                processSchema((Schema)obj, parentNode, xsdReader);
            }
        }
    }

    protected void processSchema( Schema schema,
                                  Node parentNode,
                                  XsdReader xsdReader ) throws Exception {
        Element schemaElement = schema.getElement();

        // We need to see what this schema contains. If it contains only references to other schemas,
        // we can parse the elements ourselves. Otherwise, we'll need to parse it and embed it within our output.
        NodeList childNodes = schemaElement.getChildNodes();

        for (int i = 0, len = childNodes.getLength(); i != len; ++i) {
            org.w3c.dom.Node child = childNodes.item(i);
            switch (child.getNodeType()) {
                case org.w3c.dom.Node.ELEMENT_NODE:
                    Element element = (Element)child;
                    if (processXsdReference(element, parentNode)) {
                        // then this is an 'import', 'include', or 'reference' ...
                    } else if (processXsdAnnotation(element, parentNode)) {
                        // then this is an 'annotation' ...
                    } else {
                        // If there is more than just 'import', 'include', 'redefine', or 'annotation' elements ...
                        String schemaContent = writeXml(schemaElement);
                        InputSource source = new InputSource(new StringReader(schemaContent));

                        Node schemaNode = parentNode.addNode(WsdlLexicon.SCHEMA, XsdLexicon.SCHEMA_DOCUMENT);
                        // Process the XSD
                        xsdReader.read(source, schemaNode);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    protected boolean processXsdReference( Element element,
                                           Node parentNode ) throws RepositoryException {
        String localName = element.getLocalName();
        // Figure out the type of element ...
        String type = null;
        if (XsdLexicon.IMPORT.equals(localName)) {
            type = WsdlLexicon.IMPORTED_XSD;
        } else if (XsdLexicon.INCLUDE.equals(localName)) {
            type = WsdlLexicon.INCLUDED_XSD;
        } else if (XsdLexicon.REDEFINE.equals(localName)) {
            type = WsdlLexicon.REDEFINED_XSD;
        } else {
            return false;
        }

        Node xsdNode = parentNode.addNode(type, type);
        String namespace = getAttributeValue(element, XsdLexicon.Namespace.URI, XsdLexicon.NAMESPACE);
        String location = getAttributeValue(element, XsdLexicon.Namespace.URI, XsdLexicon.SCHEMA_LOCATION);
        String id = getAttributeValue(element, XsdLexicon.Namespace.URI, XsdLexicon.ID);
        xsdNode.setProperty(XsdLexicon.SCHEMA_LOCATION, location);
        if (namespace != null) {
            xsdNode.setProperty(XsdLexicon.NAMESPACE, namespace);
        }
        if (id != null) {
            xsdNode.setProperty(XsdLexicon.ID, id);
        }
        return true;
    }

    protected String getAttributeValue( Element element,
                                        String namespaceUri,
                                        String localName ) {
        String result = element.getAttributeNS(namespaceUri, localName);
        if (result != null && result.length() != 0) return result;

        result = element.getAttribute(localName);
        if (result != null && result.length() != 0) return result;

        Attr namespaceAttr = element.getAttributeNode(localName);
        return namespaceAttr != null ? namespaceAttr.getValue() : null;
    }

    protected boolean processXsdAnnotation( Element element,
                                            Node parentNode ) {
        logger.warn("Ignoring xsd annotation");
        String localName = element.getLocalName();
        // Currently ignoring the annotation
        return XsdLexicon.ANNOTATION.equals(localName);
    }

    protected void processMessages( Map<String, Message> messages,
                                    Node parentNode ) throws Exception {
        if (messages.isEmpty()) {
            return;
        }
        Node messagesNode = parentNode.addNode(WsdlLexicon.MESSAGES, WsdlLexicon.MESSAGES);
        for (Map.Entry<String, Message> entry : messages.entrySet()) {
            Message message = entry.getValue();
            processMessage(message, messagesNode);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processMessage( Message message,
                                   Node messagesNode ) throws Exception {
        Node messageNode = addChildNode(messagesNode, message.getQName(),  WsdlLexicon.MESSAGE);
        setReferenceable(messageNode, MESSAGES, message.getQName());
        processDocumentation(message, messageNode);
        processExtensibilityElements(message, messageNode);
        for (Map.Entry<String, Part> entry : ((Map<String, Part>)message.getParts()).entrySet()) {
            processPart(entry.getValue(), messageNode);
        }
    }

    protected void processPart( Part part,
                                Node parentNode ) throws Exception {
        String partName = context.valueFactory().createName(part.getName());
        Node partNode = parentNode.addNode(partName, WsdlLexicon.PART);
        partNode.setProperty(WsdlLexicon.NC_NAME, part.getName());

        QName elementName = part.getElementName();
        if (elementName != null) {
            partNode.setProperty(WsdlLexicon.ELEMENT_NAME, elementName.getLocalPart());
            partNode.setProperty(WsdlLexicon.ELEMENT_NAMESPACE, elementName.getNamespaceURI());

            setReference(partNode, WsdlLexicon.ELEMENT_REFERENCE, XsdReader.ELEMENT_DECLARATION, elementName);
        }

        QName typeName = part.getTypeName();
        if (typeName != null) {
            partNode.setProperty(WsdlLexicon.TYPE_NAME, typeName.getLocalPart());
            partNode.setProperty(WsdlLexicon.TYPE_NAMESPACE, typeName.getNamespaceURI());
            setReference(partNode, WsdlLexicon.TYPE_REFERENCE, XsdReader.TYPE_DEFINITIONS, typeName);
        }

        processDocumentation(part, partNode);
        processExtensibilityElements(part, partNode);
    }

    protected void processPortTypes( Map<String, PortType> portTypes,
                                     Node parentNode ) throws Exception {
        if (portTypes.isEmpty()) {
            return;
        }
        Node portTypesNode = parentNode.addNode(WsdlLexicon.PORT_TYPES, WsdlLexicon.PORT_TYPES);
        for (PortType portType : portTypes.values()) {
            processPortType(portType, portTypesNode);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processPortType( PortType portType,
                                    Node portTypesNode ) throws Exception {
        QName qname = portType.getQName();
        Node portTypeNode = addChildNode(portTypesNode, qname, WsdlLexicon.PORT_TYPE);
        setReferenceable(portTypeNode, PORT_TYPES, qname);

        for (Operation operation : (List<Operation>)portType.getOperations()) {
            processOperation(operation, portTypeNode);
        }

        processDocumentation(portType, portTypeNode);
        processExtensibilityElements(portType, portTypeNode);
    }

    @SuppressWarnings( "unchecked" )
    protected void processOperation( Operation operation,
                                     Node parentNode ) throws Exception {
        String operationName = operation.getName();
        Node operationNode = parentNode.addNode(context.valueFactory().createName(operationName), WsdlLexicon.OPERATION);
        operationNode.setProperty(WsdlLexicon.NC_NAME, operationName);

        OperationType style = operation.getStyle();
        processInput(operation.getInput(), operationNode, style, operationName);
        processOutput(operation.getOutput(), operationNode, style, operationName);
        for (Fault fault : ((Map<String, Fault>)operation.getFaults()).values()) {
            processFault(fault, operationNode);
        }

        if (OperationType.REQUEST_RESPONSE.equals(style) || OperationType.SOLICIT_RESPONSE.equals(style)) {
            operationNode.setProperty(WsdlLexicon.PARAMETER_ORDER, listToStringArray(operation.getParameterOrdering()));
        }

        processDocumentation(operation, operationNode);
        processExtensibilityElements(operation, operationNode);
    }

    protected void processInput( Input input,
                                 Node parentNode,
                                 OperationType style,
                                 String operationName ) throws Exception {
        if (input == null) {
            return;
        }
        String name = input.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) {
                name = operationName;
            } else if (OperationType.NOTIFICATION.equals(style)) {
                name = operationName;
            } else if (OperationType.REQUEST_RESPONSE.equals(style)) {
                name = operationName + "Request";
            } else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Solicit";
        }
        Node inputNode = parentNode.addNode(WsdlLexicon.INPUT, WsdlLexicon.OPERATION_INPUT);
        uuidForComponent.put(input, inputNode.getIdentifier());
        nameForComponent.put(input, name);
        inputNode.setProperty(WsdlLexicon.NC_NAME, name);

        Message message = input.getMessage();
        if (message != null) {
            QName messageName = message.getQName();
            inputNode.setProperty(WsdlLexicon.MESSAGE_NAME, messageName.getLocalPart());
            inputNode.setProperty(WsdlLexicon.MESSAGE_NAMESPACE, messageName.getNamespaceURI());
            setReference(inputNode, WsdlLexicon.MESSAGE_REFERENCE, MESSAGES, messageName);
        }

        processDocumentation(input, inputNode);
        processExtensibilityElements(input, inputNode);
    }

    protected void processOutput( Output out,
                                  Node operationNode,
                                  OperationType style,
                                  String operationName ) throws Exception {
        if (out == null) {
            return;
        }
        String name = out.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) {
                name = operationName;
            } else if (OperationType.NOTIFICATION.equals(style)) {
                name = operationName;
            } else if (OperationType.REQUEST_RESPONSE.equals(style)) {
                name = operationName + "Response";
            } else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Response";
        }
        Node outputNode = operationNode.addNode(WsdlLexicon.OUTPUT, WsdlLexicon.OPERATION_OUTPUT);
        uuidForComponent.put(out, outputNode.getIdentifier());
        nameForComponent.put(out, name);
        outputNode.setProperty(WsdlLexicon.NC_NAME, name);
        Message message = out.getMessage();
        if (message != null) {
            QName messageName = message.getQName();
            outputNode.setProperty(WsdlLexicon.MESSAGE_NAME, messageName.getLocalPart());
            outputNode.setProperty(WsdlLexicon.MESSAGE_NAMESPACE, messageName.getNamespaceURI());
            setReference(outputNode, WsdlLexicon.MESSAGE_REFERENCE, MESSAGES, messageName);
        }

        processDocumentation(out, outputNode);
        processExtensibilityElements(out, outputNode);
    }

    protected void processFault( Fault fault,
                                 Node parentNode ) throws Exception {
        if (fault == null) {
            return;
        }
        Node faultNode = parentNode.addNode(WsdlLexicon.FAULT, WsdlLexicon.FAULT);
        faultNode.setProperty(WsdlLexicon.NC_NAME, fault.getName());
        Message message = fault.getMessage();
        if (message != null) {
            QName messageName = message.getQName();
            faultNode.setProperty(WsdlLexicon.MESSAGE_NAME, messageName.getLocalPart());
            faultNode.setProperty(WsdlLexicon.MESSAGE_NAMESPACE, messageName.getNamespaceURI());
            setReference(faultNode, WsdlLexicon.MESSAGE_REFERENCE, MESSAGES, messageName);
        }

        processDocumentation(fault, faultNode);
        processExtensibilityElements(fault, faultNode);
    }

    protected void processBindings( Map<String, Binding> bindings,
                                    Node parentNode ) throws Exception {
        if (bindings.isEmpty()) {
            return;
        }
        Node bindingNode = parentNode.addNode(WsdlLexicon.BINDINGS, WsdlLexicon.BINDINGS);
        for (Binding binding : bindings.values()) {
            processBinding(binding, bindingNode);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processBinding( Binding binding,
                                   Node bindingsNode ) throws Exception {
        QName qname = binding.getQName();
        Node bindingNode = addChildNode(bindingsNode, qname, WsdlLexicon.BINDING);
        setReferenceable(bindingNode, BINDINGS, qname);
        PortType portType = binding.getPortType();
        if (portType != null) {
            QName typeName = portType.getQName();
            bindingNode.setProperty(WsdlLexicon.TYPE_NAME, typeName.getLocalPart());
            bindingNode.setProperty(WsdlLexicon.TYPE_NAMESPACE, typeName.getNamespaceURI());
            setReference(bindingNode, WsdlLexicon.TYPE_REFERENCE, PORT_TYPES, typeName);
        }

        binding.getBindingOperations();
        for (BindingOperation operation : (List<BindingOperation>)binding.getBindingOperations()) {
            processBindingOperation(operation, bindingNode);
        }

        processDocumentation(binding, bindingNode);
        processExtensibilityElements(binding, bindingNode);
    }

    @SuppressWarnings( "unchecked" )
    protected void processBindingOperation( BindingOperation operation,
                                            Node parentNode ) throws Exception {
        String operationName = operation.getName();
        String name = context.valueFactory().createName(operationName);
        Node bindingOperation = parentNode.addNode(name, WsdlLexicon.BINDING_OPERATION);
        bindingOperation.setProperty(WsdlLexicon.NC_NAME, operationName);

        Operation portTypeOperation = operation.getOperation();
        OperationType type = portTypeOperation.getStyle();
        processBindingInput(operation.getBindingInput(), bindingOperation, type, operationName, portTypeOperation);
        processBindingOutput(operation.getBindingOutput(), bindingOperation, type, operationName, portTypeOperation);
        for (BindingFault fault : ((Map<String, BindingFault>)operation.getBindingFaults()).values()) {
            processBindingFault(fault, bindingOperation);
        }

        processDocumentation(operation, bindingOperation);
        processExtensibilityElements(operation, bindingOperation);
    }

    protected void processBindingInput( BindingInput input,
                                        Node parentNode,
                                        OperationType style,
                                        String operationName,
                                        Operation operation ) throws Exception {
        if (input == null) {
            return;
        }
        String name = input.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) {
                name = operationName;
            } else if (OperationType.NOTIFICATION.equals(style)) {
                name = operationName;
            } else if (OperationType.REQUEST_RESPONSE.equals(style)) {
                name = operationName + "Request";
            } else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Solicit";
        }
        Node bindingInputNode = parentNode.addNode(WsdlLexicon.INPUT, WsdlLexicon.BINDING_OPERATION_INPUT);
        bindingInputNode.setProperty(WsdlLexicon.NC_NAME, name);
        // Find the port type's input ...
        Input opInput = operation.getInput();
        bindingInputNode.setProperty(WsdlLexicon.INPUT_REFERENCE, uuidForComponent(opInput));
        bindingInputNode.setProperty(WsdlLexicon.INPUT_NAME, nameForComponent.get(opInput));

        processDocumentation(input, bindingInputNode);
        processExtensibilityElements(input, bindingInputNode);
    }

    protected void processBindingOutput( BindingOutput out,
                                         Node parentNode,
                                         OperationType style,
                                         String operationName,
                                         Operation operation ) throws Exception {
        if (out == null) {
            return;
        }
        String name = out.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) {
                name = operationName;
            } else if (OperationType.NOTIFICATION.equals(style)) {
                name = operationName;
            } else if (OperationType.REQUEST_RESPONSE.equals(style)) {
                name = operationName + "Response";
            } else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Response";
        }
        Node bindingOutputNode = parentNode.addNode(WsdlLexicon.OUTPUT, WsdlLexicon.BINDING_OPERATION_OUTPUT);
        bindingOutputNode.setProperty(WsdlLexicon.NC_NAME, name);
        // Find the port type's output ...
        Output opOutput = operation.getOutput();
        bindingOutputNode.setProperty(WsdlLexicon.OUTPUT_REFERENCE, uuidForComponent(opOutput));
        bindingOutputNode.setProperty(WsdlLexicon.OUTPUT_NAME, nameForComponent.get(opOutput));

        processDocumentation(out, bindingOutputNode);
        processExtensibilityElements(out, bindingOutputNode);
    }

    protected void processBindingFault( BindingFault fault,
                                        Node parentNode ) throws Exception {
        if (fault == null) {
            return;
        }
        Node bindingFaultNode = parentNode.addNode(WsdlLexicon.FAULT, WsdlLexicon.BINDING_OPERATION_FAULT);
        bindingFaultNode.setProperty(WsdlLexicon.NC_NAME, fault.getName());

        processDocumentation(fault, bindingFaultNode);
        processExtensibilityElements(fault, bindingFaultNode);
    }

    protected void processServices( Map<String, Service> services,
                                    Node parentNode ) throws Exception {
        if (services.isEmpty()) {
            return;
        }
        Node servicesNode = parentNode.addNode(WsdlLexicon.SERVICES, WsdlLexicon.SERVICES);
        for (Service service : services.values()) {
            processService(service, servicesNode);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processService( Service service,
                                   Node parentNode ) throws Exception {
        QName qname = service.getQName();
        Node serviceNode = addChildNode(parentNode, qname, WsdlLexicon.SERVICE);
        setReferenceable(serviceNode, SERVICES, qname);

        for (Port port : ((Map<String, Port>)service.getPorts()).values()) {
            processPort(port, serviceNode);
        }

        processDocumentation(service, serviceNode);
        processExtensibilityElements(service, serviceNode);
    }

    protected void processPort( Port port,
                                Node parentNode ) throws Exception {
        String name = port.getName();
        Node portNode = parentNode.addNode(context.valueFactory().createName(name), WsdlLexicon.PORT);

        QName bindingName = port.getBinding().getQName();
        setReference(portNode, WsdlLexicon.BINDING_REFERENCE, BINDINGS, bindingName);
        portNode.setProperty(WsdlLexicon.NC_NAME, name);

        processDocumentation(port, portNode);
        processExtensibilityElements(port, portNode);
    }

    protected void processDocumentation( WSDLElement wsdlElement,
                                         Node outputNode ) throws Exception {
        if (wsdlElement == null) {
            return;
        }
        Element docElement = wsdlElement.getDocumentationElement();
        if (docElement == null) {
            return;
        }
        String content = null;
        int numChildNodes = docElement.getChildNodes().getLength();
        if (numChildNodes == 0) {
            content = docElement.getTextContent();
        } else if (numChildNodes == 1) {
            // Should be just a text node element ...
            content = docElement.getTextContent();
        } else {
            // There is at least 2 child nodes ...
            content = writeXml(docElement);
        }
        if (content == null) {
            return;
        }
        content = content.trim();
        if (content.length() != 0) {
            outputNode.setProperty(SrampLexicon.DESCRIPTION, cleanDocumentationContent(content));
        }
    }

    /**
     * This method is used by the {@link #processDocumentation(WSDLElement, Node)} to clean up the documentation content string.
     * By default, this method replaces all line feeds, carriage returns, and sequences of multiple whitespace with a single
     * space.
     *
     * @param content the original content as read from the definition; never null
     * @return the cleaned content; may be null if the content contained useless information
     */
    protected String cleanDocumentationContent( String content ) {
        Pattern REMOVE_WHITESPACE_AND_LINE_FEEDS_PATTERN = Pattern.compile("[\\n\\r\\s]+");
        Matcher matcher = REMOVE_WHITESPACE_AND_LINE_FEEDS_PATTERN.matcher(content);
        return matcher.replaceAll(" ");
    }

    @SuppressWarnings( "unchecked" )
    protected void processExtensibilityElements( WSDLElement wsdlElement,
                                                 Node outputNode ) throws Exception {
        if (wsdlElement == null) {
            return;
        }

        // Process the attributes ...
        for (Map.Entry<String, Attr> entry : ((Map<String, Attr>)wsdlElement.getExtensionAttributes()).entrySet()) {
            Attr attribute = entry.getValue();
            // Record any attribute that is not in the XSD namespace ...
            String namespaceUri = attribute.getNamespaceURI();
            if (!WsdlLexicon.Namespace.URI.equals(namespaceUri)) {
                String localName = attribute.getLocalName();
                String value = attribute.getNodeValue();
                if (value == null) continue;
                if (namespaceUri != null) {
                    NamespaceRegistry namespaceRegistry = outputNode.getSession().getWorkspace().getNamespaceRegistry();
                    String prefix = registerNamespace(namespaceRegistry, namespaceUri, attribute.getPrefix());
                    String propertyName = prefix + ":" + localName;
                    outputNode.setProperty(propertyName, value);
                } else {
                    outputNode.setProperty(localName, value);
                }
            }
        }

        // Process the elements ...
        for (ExtensibilityElement element : (List<ExtensibilityElement>)wsdlElement.getExtensibilityElements()) {
            processExtensibilityElement(element, outputNode);
        }
    }

    protected void processExtensibilityElement( ExtensibilityElement element,
                                                Node parentNode ) throws Exception {
        if (element == null) {
            return;
        }
        if (element instanceof SOAP12Address) {
            processSOAP12Address((SOAP12Address)element, parentNode);
        } else if (element instanceof SOAP12Binding) {
            processSOAP12Binding((SOAP12Binding)element, parentNode);
        } else if (element instanceof SOAP12Body) {
            processSOAP12Body((SOAP12Body)element, parentNode);
        } else if (element instanceof SOAP12Fault) {
            processSOAP12Fault((SOAP12Fault)element, parentNode);
        } else if (element instanceof SOAP12Header) {
            processSOAP12Header((SOAP12Header)element, parentNode);
        } else if (element instanceof SOAP12HeaderFault) {
            processSOAP12HeaderFault((SOAP12HeaderFault)element, parentNode);
        } else if (element instanceof SOAP12Operation) {
            processSOAP12Operation((SOAP12Operation)element, parentNode);
        } else if (element instanceof SOAPAddress) {
            processSOAPAddress((SOAPAddress)element, parentNode);
        } else if (element instanceof SOAPBinding) {
            processSOAPBinding((SOAPBinding)element, parentNode);
        } else if (element instanceof SOAPBody) {
            processSOAPBody((SOAPBody)element, parentNode);
        } else if (element instanceof SOAPFault) {
            processSOAPFault((SOAPFault)element, parentNode);
        } else if (element instanceof SOAPHeader) {
            processSOAPHeader((SOAPHeader)element, parentNode);
        } else if (element instanceof SOAPHeaderFault) {
            processSOAPHeaderFault((SOAPHeaderFault)element, parentNode);
        } else if (element instanceof SOAPOperation) {
            processSOAPOperation((SOAPOperation)element, parentNode);
        } else if (element instanceof MIMEMultipartRelated) {
            processMIMEMultipartRelated((MIMEMultipartRelated)element,
                                        parentNode);
        } else if (element instanceof MIMEContent) {
            processMIMEContent((MIMEContent)element, parentNode);
        } else if (element instanceof MIMEPart) {
            processMIMEPart((MIMEPart)element, parentNode);
        } else if (element instanceof MIMEMimeXml) {
            processMIMEMimeXml((MIMEMimeXml)element, parentNode);
        } else if (element instanceof HTTPAddress) {
            processHTTPAddress((HTTPAddress)element, parentNode);
        } else if (element instanceof HTTPBinding) {
            processHTTPBinding((HTTPBinding)element, parentNode);
        } else if (element instanceof HTTPOperation) {
            processHTTPOperation((HTTPOperation)element, parentNode);
        } else if (element instanceof HTTPUrlEncoded) {
            processHTTPUrlEncoded((HTTPUrlEncoded)element, parentNode);
        } else if (element instanceof HTTPUrlReplacement) {
            processHTTPUrlReplacement((HTTPUrlReplacement)element, parentNode);
        } else {
            processUnknownExtensionElement(element, parentNode);
        }
    }

    protected void processHTTPAddress( HTTPAddress element,
                                       Node parentNode ) throws Exception {
        Node addressNode = parentNode.addNode(WsdlLexicon.HTTP_ADDRESS, WsdlLexicon.HTTP_ADDRESS);
        addressNode.setProperty(WsdlLexicon.LOCATION, element.getLocationURI());
    }

    protected void processHTTPBinding( HTTPBinding element,
                                       Node parentNode ) throws Exception {
        Node bindingNode = parentNode.addNode(WsdlLexicon.HTTP_BINDING, WsdlLexicon.HTTP_BINDING);
        bindingNode.setProperty(WsdlLexicon.VERB, element.getVerb());
    }

    protected void processHTTPOperation( HTTPOperation element,
                                         Node parentNode ) throws Exception {
        Node operationNode = parentNode.addNode(WsdlLexicon.HTTP_OPERATION, WsdlLexicon.HTTP_OPERATION);
        operationNode.setProperty(WsdlLexicon.LOCATION, element.getLocationURI());
    }

    protected void processHTTPUrlEncoded( HTTPUrlEncoded element,
                                          Node parentNode ) throws Exception {
        parentNode.addNode(WsdlLexicon.HTTP_URL_ENCODED, WsdlLexicon.HTTP_URL_ENCODED);
    }

    protected void processHTTPUrlReplacement( HTTPUrlReplacement element,
                                              Node parentNode ) throws Exception {
        parentNode.addNode(WsdlLexicon.HTTP_URL_REPLACEMENT, WsdlLexicon.HTTP_URL_REPLACEMENT);
    }

    protected void processSOAP12Address( SOAP12Address element,
                                         Node parentNode ) throws Exception {
        Node addressNode = parentNode.addNode(WsdlLexicon.SOAP_ADDRESS, WsdlLexicon.SOAP_ADDRESS);
        addressNode.setProperty(WsdlLexicon.SOAP_LOCATION, element.getLocationURI());
    }

    protected void processSOAP12Binding( SOAP12Binding element,
                                         Node parentNode ) throws Exception {
        Node bindingNode = parentNode.addNode(WsdlLexicon.SOAP_BINDING, WsdlLexicon.SOAP_BINDING);
        bindingNode.setProperty(WsdlLexicon.STYLE, element.getStyle());
        bindingNode.setProperty(WsdlLexicon.TRANSPORT, element.getTransportURI());
    }

    protected void processSOAP12Operation( SOAP12Operation element,
                                           Node parentNode ) throws Exception {
        Node operationNode = parentNode.addNode(WsdlLexicon.SOAP_OPERATION, WsdlLexicon.SOAP_OPERATION);
        operationNode.setProperty(WsdlLexicon.STYLE, element.getStyle());
        operationNode.setProperty(WsdlLexicon.SOAP_ACTION, element.getSoapActionURI());
    }

    protected void processSOAP12Body( SOAP12Body element,
                                      Node parentNode ) throws Exception {
        Node bodyNode = parentNode.addNode(WsdlLexicon.SOAP_BODY, WsdlLexicon.SOAP_BODY);
        bodyNode.setProperty(WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        bodyNode.setProperty(WsdlLexicon.PARTS, listToStringArray(element.getParts()));
        bodyNode.setProperty(WsdlLexicon.USE, element.getUse());
    }

    protected void processSOAP12Fault( SOAP12Fault element,
                                       Node parentNode ) throws Exception {
        Node faultNode = parentNode.addNode(WsdlLexicon.SOAP_FAULT, WsdlLexicon.SOAP_FAULT);
        faultNode.setProperty(WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        faultNode.setProperty(WsdlLexicon.USE, element.getUse());
    }

    @SuppressWarnings( "unchecked" )
    protected void processSOAP12Header( SOAP12Header element,
                                        Node parentNode ) throws Exception {
        Node headerNode = parentNode.addNode(WsdlLexicon.SOAP_HEADER, WsdlLexicon.SOAP_HEADER);
        headerNode.setProperty(WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        headerNode.setProperty(WsdlLexicon.MESSAGE, element.getMessage().toString());
        headerNode.setProperty(WsdlLexicon.PART, element.getPart());
        headerNode.setProperty(WsdlLexicon.USE, element.getUse());
        for (SOAP12HeaderFault fault : (List<SOAP12HeaderFault>)element.getSOAP12HeaderFaults()) {
            processSOAP12HeaderFault(fault, headerNode);
        }
    }

    protected void processSOAP12HeaderFault( SOAP12HeaderFault element,
                                             Node parentNode ) throws Exception {
        Node faultNode = parentNode.addNode(WsdlLexicon.SOAP_HEADER_FAULT, WsdlLexicon.SOAP_HEADER_FAULT);
        faultNode.setProperty(WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        faultNode.setProperty(WsdlLexicon.USE, element.getUse());
    }

    protected void processSOAPAddress( SOAPAddress element,
                                       Node parentNode ) throws Exception {
        Node addressNode = parentNode.addNode(WsdlLexicon.SOAP_ADDRESS, WsdlLexicon.SOAP_ADDRESS);
        addressNode.setProperty(WsdlLexicon.SOAP_LOCATION, element.getLocationURI());
    }

    protected void processSOAPBinding( SOAPBinding element,
                                       Node parentNode ) throws Exception {
        Node bindingNode = parentNode.addNode(WsdlLexicon.SOAP_BINDING, WsdlLexicon.SOAP_BINDING);
        bindingNode.setProperty(WsdlLexicon.STYLE, element.getStyle());
        bindingNode.setProperty(WsdlLexicon.TRANSPORT, element.getTransportURI());
    }

    protected void processSOAPOperation( SOAPOperation element,
                                         Node parentNode ) throws Exception {
        Node operationNode = parentNode.addNode(WsdlLexicon.SOAP_OPERATION, WsdlLexicon.SOAP_OPERATION);
        operationNode.setProperty(WsdlLexicon.STYLE, element.getStyle());
        operationNode.setProperty(WsdlLexicon.SOAP_ACTION, element.getSoapActionURI());
    }

    protected void processSOAPBody( SOAPBody element,
                                    Node parentNode ) throws Exception {
        Node bodyNode = parentNode.addNode(WsdlLexicon.SOAP_BODY, WsdlLexicon.SOAP_BODY);
        bodyNode.setProperty(WsdlLexicon.ENCODING_STYLE, listToStringArray(element.getEncodingStyles()));
        bodyNode.setProperty(WsdlLexicon.PARTS, listToStringArray(element.getParts()));
        bodyNode.setProperty(WsdlLexicon.USE, element.getUse());
    }

    protected void processSOAPFault( SOAPFault element,
                                     Node parentNode ) throws Exception {
        Node faultNode = parentNode.addNode(WsdlLexicon.SOAP_FAULT, WsdlLexicon.SOAP_FAULT);
        faultNode.setProperty(WsdlLexicon.ENCODING_STYLE, listToStringArray(element.getEncodingStyles()));
        faultNode.setProperty(WsdlLexicon.USE, element.getUse());
    }

    @SuppressWarnings( "unchecked" )
    protected void processSOAPHeader( SOAPHeader element,
                                      Node parentNode ) throws Exception {
        Node headerNode = parentNode.addNode(WsdlLexicon.SOAP_HEADER, WsdlLexicon.SOAP_HEADER);
        headerNode.setProperty(WsdlLexicon.ENCODING_STYLE, listToStringArray(element.getEncodingStyles()));
        headerNode.setProperty(WsdlLexicon.MESSAGE, element.getMessage().toString());
        headerNode.setProperty(WsdlLexicon.PART, element.getPart());
        headerNode.setProperty(WsdlLexicon.USE, element.getUse());
        for (SOAPHeaderFault fault : (List<SOAPHeaderFault>)element.getSOAPHeaderFaults()) {
            processSOAPHeaderFault(fault, headerNode);
        }
    }

    protected void processSOAPHeaderFault( SOAPHeaderFault element,
                                           Node parentNode ) throws Exception {
        Node faultNode = parentNode.addNode(WsdlLexicon.SOAP_HEADER_FAULT, WsdlLexicon.SOAP_HEADER_FAULT);
        faultNode.setProperty(WsdlLexicon.ENCODING_STYLE, listToStringArray(element.getEncodingStyles()));
        faultNode.setProperty(WsdlLexicon.USE, element.getUse());
    }

    @SuppressWarnings( "unchecked" )
    protected void processMIMEMultipartRelated( MIMEMultipartRelated element,
                                                Node parentNode ) throws Exception {
        Node mimeMultipartNode = parentNode.addNode(WsdlLexicon.MIME_MULTIPART_RELATED, WsdlLexicon.MIME_MULTIPART_RELATED);
        for (MIMEPart part : (List<MIMEPart>)element.getMIMEParts()) {
            processMIMEPart(part, mimeMultipartNode);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processMIMEPart( MIMEPart element,
                                    Node parentNode ) throws Exception {
        Node mimePartNode = parentNode.addNode(WsdlLexicon.MIME_PART, WsdlLexicon.MIME_PART);
        for (ExtensibilityElement child : (List<ExtensibilityElement>)element.getExtensibilityElements()) {
            processExtensibilityElement(child, mimePartNode);
        }
    }

    protected void processMIMEContent( MIMEContent element,
                                       Node parentNode ) throws Exception {
        Node mimeContentNode = parentNode.addNode(WsdlLexicon.MIME_CONTENT, WsdlLexicon.MIME_CONTENT);
        mimeContentNode.setProperty(WsdlLexicon.MIME_PART, element.getPart());
        mimeContentNode.setProperty(WsdlLexicon.MIME_TYPE, element.getType());
    }

    protected void processMIMEMimeXml( MIMEMimeXml element,
                                       Node parentNode ) throws Exception {
        Node mimeXmlNode = parentNode.addNode(WsdlLexicon.MIME_XML, WsdlLexicon.MIME_XML);
        mimeXmlNode.setProperty(WsdlLexicon.MIME_PART, element.getPart());
    }

    /**
     * This method is called by {@link #processExtensibilityElement(javax.wsdl.extensions.ExtensibilityElement, javax.jcr.Node)}
     * when the type of extension is not known. By default this method does nothing, but subclasses can override this method
     * to specialize the behavior.
     *
     * @param element the extensibility element
     * @param parentNode the parent node; never null
     * @throws Exception if there is a problem processing the element
     */
    protected void processUnknownExtensionElement( ExtensibilityElement element,
                                                   Node parentNode ) throws Exception {
        // ignore for now
        logger.warn("Unknown extension element {}", element);
    }

    protected void setReferenceable( Node node,
                                     SymbolSpace space,
                                     QName qname ) throws RepositoryException {
        setReferenceable(node, space, qname.getNamespaceURI(), qname.getLocalPart());
    }

    protected void setReferenceable( Node node,
                                     SymbolSpace space,
                                     String namespace,
                                     String name ) throws RepositoryException {
        registerForSymbolSpace(space, namespace, name, node.getIdentifier());
    }

    protected void setReference( Node node,
                                 String propertyName,
                                 SymbolSpace space,
                                 QName name ) throws RepositoryException {
        setReference(node, propertyName, space, name.getNamespaceURI(), name.getLocalPart());
    }

    protected String writeXml( org.w3c.dom.Node xmlNode ) throws TransformerFactoryConfigurationError, TransformerException {
        // Prepare the DOM document for writing
        Source source = new DOMSource(xmlNode);

        // Prepare the output file
        StringWriter writer = new StringWriter();
        Result result = new StreamResult(writer);

        // Write the DOM document to the file
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(source, result);
        return writer.toString();
    }

    protected Node addChildNode( Node parentNode,
                                 QName qname,
                                 String primaryType ) throws RepositoryException {
        String localName = qname.getLocalPart();
        String namespace = qname.getNamespaceURI();
        Node childNode = parentNode.addNode(context.valueFactory().createName(localName), primaryType);
        childNode.setProperty(WsdlLexicon.NC_NAME, localName);
        childNode.setProperty(WsdlLexicon.NAMESPACE, namespace);
        return childNode;
    }

    protected String uuidForComponent( WSDLElement component ) {
        String uuid = uuidForComponent.get(component);
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            uuidForComponent.put(component, uuid);
        }
        return uuid;
    }

    private String[] listToStringArray( List<?> list ) {
        if (list == null) {
            return null;
        }
        String[] result = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i).toString();
        }
        return result;
    }

    protected class PortTypeResolvers {

        private final Map<QName, NamespaceEntityResolver> resolversByKind = new HashMap<QName, NamespaceEntityResolver>();

        public NamespaceEntityResolver get( QName portTypeName ) {
            NamespaceEntityResolver resolver = resolversByKind.get(portTypeName);
            if (resolver == null) {
                resolver = new NamespaceEntityResolver();
                resolversByKind.put(portTypeName, resolver);
            }
            return resolver;
        }
    }

    protected class CustomWSDLLocator implements WSDLLocator {

        protected final InputSource baseInputSource;
        protected final InputSource emptyInputSource;
        protected final String baseUri;

        protected CustomWSDLLocator( InputSource baseInputSource,
                                     String baseUri ) {
            this.baseInputSource = baseInputSource;
            this.emptyInputSource = new InputSource(new StringReader(""));
            this.baseUri = baseUri;
        }

        @Override
        public String getLatestImportURI() {
            return baseUri;
        }

        @Override
        public InputSource getImportInputSource( String parentLocation,
                                                 String importLocation ) {
            return emptyInputSource;
        }

        @Override
        public String getBaseURI() {
            return baseUri;
        }

        @Override
        public InputSource getBaseInputSource() {
            return baseInputSource;
        }

        @Override
        public void close() {
        }
    }
}
