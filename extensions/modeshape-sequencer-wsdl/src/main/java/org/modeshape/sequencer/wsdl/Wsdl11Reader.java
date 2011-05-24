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

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.modeshape.common.collection.Collections;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.sramp.SrampLexicon;
import org.modeshape.sequencer.xsd.NamespaceEntityResolver;
import org.modeshape.sequencer.xsd.XsdLexicon;
import org.modeshape.sequencer.xsd.XsdReader;
import org.modeshape.sequencer.xsd.XsdResolvers;
import org.modeshape.sequencer.xsd.XsdResolvers.SymbolSpace;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import com.ibm.wsdl.Constants;

/**
 * A class that can parse WSDL 1.1 definitions, derive a graph structure from the content, and output that graph structure to a
 * supplied {@link SequencerOutput}.
 * <p>
 * This class can be subclassed and any of the 'process' methods overridden to customize the dervied graph structure.
 * </p>
 */
@NotThreadSafe
public class Wsdl11Reader extends WsdlReader<javax.wsdl.Definition> {

    protected static final SymbolSpace MESSAGES = new SymbolSpace("Messages");
    protected static final SymbolSpace PORT_TYPES = new SymbolSpace("PortTypes");
    protected static final SymbolSpace BINDINGS = new SymbolSpace("Bindings");
    protected static final SymbolSpace PORTS = new SymbolSpace("Ports");
    protected static final SymbolSpace SERVICES = new SymbolSpace("Services");

    protected XsdResolvers typeResolvers;
    protected Map<WSDLElement, UUID> uuidForComponent;
    protected Map<WSDLElement, String> nameForComponent;

    public Wsdl11Reader( SequencerOutput output,
                         StreamSequencerContext context ) {
        super(output, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.wsdl.WsdlReader#parse(org.xml.sax.InputSource, java.lang.String)
     */
    @Override
    protected Definition parse( InputSource source,
                                String baseUri ) throws Exception {
        WSDLReader wsdlReader = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader();
        wsdlReader.setFeature(Constants.FEATURE_VERBOSE, false);
        wsdlReader.setFeature(Constants.FEATURE_IMPORT_DOCUMENTS, false);
        // We use a custom WSDLLocator that never tries to resolve the import locations for WSDLs or XSDs.
        WSDLLocator locator = new CustomWSDLLocator(source, baseUri);
        Definition def = wsdlReader.readWSDL(locator);
        return def;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.wsdl.WsdlReader#process(java.lang.Object, org.modeshape.graph.property.Path, long)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    protected void process( Definition definition,
                            Path docPath,
                            long sizeOfFile ) throws Exception {
        uuidForComponent = new HashMap<WSDLElement, UUID>();
        nameForComponent = new HashMap<WSDLElement, String>();

        output.setProperty(docPath, SrampLexicon.CONTENT_TYPE, "application/wsdl");
        output.setProperty(docPath, SrampLexicon.CONTENT_SIZE, sizeOfFile);
        output.setProperty(docPath, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.WSDL_DOCUMENT);
        output.setProperty(docPath, JcrLexicon.UUID, UUID.randomUUID());

        processDocumentation(definition, docPath);
        processExtensibilityElements(definition, docPath);

        processTypes(definition.getTypes(), docPath);
        processMessages(definition.getMessages(), docPath);
        processPortTypes(definition.getPortTypes(), docPath);
        processBindings(definition.getBindings(), docPath);
        processServices(definition.getServices(), docPath);
    }

    protected void processTypes( Types types,
                                 Path parentPath ) throws Exception {
        if (types == null) return;
        XsdReader xsdReader = new XsdReader(output, context);
        typeResolvers = xsdReader.getResolvers();
        for (Object obj : types.getExtensibilityElements()) {
            if (obj instanceof Schema) {
                process((Schema)obj, parentPath, xsdReader);
            }
        }
    }

    protected void process( Schema schema,
                            Path parentPath,
                            XsdReader xsdReader ) throws Exception {
        Element schemaElement = schema.getElement();

        // We need to see what this schema contains. If it contains only references to other schemas,
        // we can parse the elements ourselves. Otherwise, we'll need to parse it and embed it within our output.
        NodeList childNodes = schemaElement.getChildNodes();
        boolean parseSchemaRequired = false;
        for (int i = 0, len = childNodes.getLength(); i != len; ++i) {
            Node child = childNodes.item(i);
            switch (child.getNodeType()) {
                case Node.ELEMENT_NODE:
                    Element element = (Element)child;
                    if (processXsdReference(element, parentPath)) {
                        // then this is an 'import', 'include', or 'reference' ...
                    } else if (processXsdAnnotation(element, parentPath)) {
                        // then this is an 'annotation' ...
                    } else {
                        parseSchemaRequired = true;
                    }
                    break;
                default:
                    break;
            }
        }

        if (parseSchemaRequired) {
            // If there is more than just 'import', 'include', 'redefine', or 'annotation' elements ...
            String schemaContent = writeXml(schemaElement);
            InputSource source = new InputSource(new StringReader(schemaContent));

            // Process the XSD and write the derived content to the WSDL output ...
            Path path = nextPath(parentPath, WsdlLexicon.SCHEMA);
            xsdReader.read(source, path);
        }
    }

    protected boolean processXsdReference( Element element,
                                           Path parentPath ) {
        String localName = element.getLocalName();
        // Figure out the type of element ...
        Name type = null;
        if (XsdLexicon.IMPORT.getLocalName().equals(localName)) type = WsdlLexicon.IMPORTED_XSD;
        else if (XsdLexicon.INCLUDE.getLocalName().equals(localName)) type = WsdlLexicon.INCLUDED_XSD;
        else if (XsdLexicon.REDEFINE.getLocalName().equals(localName)) type = WsdlLexicon.REDEFINED_XSD;
        else return false;

        Path path = nextPath(parentPath, type);
        String namespace = getAttributeValue(element, XsdLexicon.Namespace.URI, XsdLexicon.NAMESPACE.getLocalName());
        String location = getAttributeValue(element, XsdLexicon.Namespace.URI, XsdLexicon.SCHEMA_LOCATION.getLocalName());
        String id = getAttributeValue(element, XsdLexicon.Namespace.URI, XsdLexicon.ID.getLocalName());
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, type);
        output.setProperty(path, XsdLexicon.SCHEMA_LOCATION, location);
        if (namespace != null) output.setProperty(path, XsdLexicon.NAMESPACE, namespace);
        if (id != null) output.setProperty(path, XsdLexicon.ID, id);
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
                                            Path parentPath ) {
        String localName = element.getLocalName();
        // Currently ignoring the annotation
        return XsdLexicon.ANNOTATION.getLocalName().equals(localName);
    }

    protected static final Set<String> SCHEMA_REFERENCE_ELEMENTS = Collections.unmodifiableSet("import",
                                                                                               "include",
                                                                                               "redefine",
                                                                                               "annotation");

    protected void processMessages( Map<String, Message> messages,
                                    Path parentPath ) throws Exception {
        Path messagesPath = null;
        for (Map.Entry<String, Message> entry : messages.entrySet()) {
            Message message = entry.getValue();
            if (messagesPath == null) {
                messagesPath = nextPath(parentPath, WsdlLexicon.MESSAGES);
                output.setProperty(messagesPath, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.MESSAGES);
            }
            processMessage(message, messagesPath);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processMessage( Message message,
                                   Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, message.getQName(), WsdlLexicon.MESSAGE);
        setReferenceable(path, MESSAGES, message.getQName());
        processDocumentation(message, path);
        processExtensibilityElements(message, path);
        for (Map.Entry<String, Part> entry : ((Map<String, Part>)message.getParts()).entrySet()) {
            processPart(entry.getValue(), path);
        }
    }

    protected void processPart( Part part,
                                Path parentPath ) throws Exception {
        String ncName = part.getName();
        Path path = nextPath(parentPath, name(ncName));
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.PART);
        output.setProperty(path, WsdlLexicon.NC_NAME, ncName);

        QName elementName = part.getElementName();
        if (elementName != null) {
            output.setProperty(path, WsdlLexicon.ELEMENT_NAME, elementName.getLocalPart());
            output.setProperty(path, WsdlLexicon.ELEMENT_NAMESPACE, elementName.getNamespaceURI());
            setReference(path, WsdlLexicon.ELEMENT_REFERENCE, SymbolSpace.ELEMENT_DECLARATION, elementName);
        }

        QName typeName = part.getTypeName();
        if (typeName != null) {
            output.setProperty(path, WsdlLexicon.TYPE_NAME, typeName.getLocalPart());
            output.setProperty(path, WsdlLexicon.TYPE_NAMESPACE, typeName.getNamespaceURI());
            setReference(path, WsdlLexicon.TYPE_REFERENCE, SymbolSpace.TYPE_DEFINITIONS, typeName);
        }

        processDocumentation(part, path);
        processExtensibilityElements(part, path);
    }

    protected void processPortTypes( Map<String, PortType> portTypes,
                                     Path parentPath ) throws Exception {
        Path portTypesPath = null;
        for (PortType portType : portTypes.values()) {
            if (portTypesPath == null) {
                portTypesPath = nextPath(parentPath, WsdlLexicon.PORT_TYPES);
                output.setProperty(portTypesPath, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.PORT_TYPES);
            }
            processPortType(portType, portTypesPath);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processPortType( PortType portType,
                                    Path parentPath ) throws Exception {
        QName qname = portType.getQName();
        Path path = nextPath(parentPath, qname, WsdlLexicon.PORT_TYPE);
        setReferenceable(path, PORT_TYPES, qname);

        for (Operation operation : (List<Operation>)portType.getOperations()) {
            process(operation, path, qname);
        }

        processDocumentation(portType, path);
        processExtensibilityElements(portType, path);
    }

    @SuppressWarnings( "unchecked" )
    protected void process( Operation operation,
                            Path parentPath,
                            QName portTypeName ) throws Exception {
        String operationName = operation.getName();
        Path path = nextPath(parentPath, name(operationName));
        output.setProperty(path, WsdlLexicon.NC_NAME, operationName);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.OPERATION);

        OperationType style = operation.getStyle();
        process(operation.getInput(), path, style, operationName, portTypeName);
        process(operation.getOutput(), path, style, operationName, portTypeName);
        for (Fault fault : ((Map<String, Fault>)operation.getFaults()).values()) {
            process(fault, path);
        }

        if (OperationType.REQUEST_RESPONSE.equals(style) || OperationType.SOLICIT_RESPONSE.equals(style)) {
            @SuppressWarnings( "cast" )
            List<String> partNames = (List<String>)operation.getParameterOrdering();
            output.setProperty(path, WsdlLexicon.PARAMETER_ORDER, partNames);
        }

        processDocumentation(operation, path);
        processExtensibilityElements(operation, path);
    }

    protected void process( Input input,
                            Path parentPath,
                            OperationType style,
                            String operationName,
                            QName portTypeName ) throws Exception {
        if (input == null) return;
        String name = input.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) name = operationName;
            else if (OperationType.NOTIFICATION.equals(style)) name = operationName;
            else if (OperationType.REQUEST_RESPONSE.equals(style)) name = operationName + "Request";
            else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Solicit";
        }
        Path path = nextPath(parentPath, WsdlLexicon.INPUT);
        UUID uuid = uuidForComponent(input);
        output.setProperty(path, JcrLexicon.UUID, uuid);
        uuidForComponent.put(input, uuid);
        nameForComponent.put(input, name);
        output.setProperty(path, WsdlLexicon.NC_NAME, name);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.OPERATION_INPUT);
        Message message = input.getMessage();
        if (message != null) {
            QName messageName = message.getQName();
            output.setProperty(path, WsdlLexicon.MESSAGE_NAME, messageName.getLocalPart());
            output.setProperty(path, WsdlLexicon.MESSAGE_NAMESPACE, messageName.getNamespaceURI());
            setReference(path, WsdlLexicon.MESSAGE_REFERENCE, MESSAGES, messageName);
        }

        processDocumentation(input, path);
        processExtensibilityElements(input, path);
    }

    protected void process( Output out,
                            Path parentPath,
                            OperationType style,
                            String operationName,
                            QName portTypeName ) throws Exception {
        if (out == null) return;
        String name = out.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) name = operationName;
            else if (OperationType.NOTIFICATION.equals(style)) name = operationName;
            else if (OperationType.REQUEST_RESPONSE.equals(style)) name = operationName + "Response";
            else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Response";
        }
        Path path = nextPath(parentPath, WsdlLexicon.OUTPUT);
        UUID uuid = uuidForComponent(out);
        output.setProperty(path, JcrLexicon.UUID, uuid);
        uuidForComponent.put(out, uuid);
        nameForComponent.put(out, name);
        output.setProperty(path, WsdlLexicon.NC_NAME, name);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.OPERATION_OUTPUT);
        Message message = out.getMessage();
        if (message != null) {
            QName messageName = message.getQName();
            output.setProperty(path, WsdlLexicon.MESSAGE_NAME, messageName.getLocalPart());
            output.setProperty(path, WsdlLexicon.MESSAGE_NAMESPACE, messageName.getNamespaceURI());
            setReference(path, WsdlLexicon.MESSAGE_REFERENCE, MESSAGES, messageName);
        }

        processDocumentation(out, path);
        processExtensibilityElements(out, path);
    }

    protected void process( Fault fault,
                            Path parentPath ) throws Exception {
        if (fault == null) return;
        Path path = nextPath(parentPath, WsdlLexicon.FAULT);
        output.setProperty(path, WsdlLexicon.NC_NAME, fault.getName());
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.FAULT);
        Message message = fault.getMessage();
        if (message != null) {
            QName messageName = message.getQName();
            output.setProperty(path, WsdlLexicon.MESSAGE_NAME, messageName.getLocalPart());
            output.setProperty(path, WsdlLexicon.MESSAGE_NAMESPACE, messageName.getNamespaceURI());
            setReference(path, WsdlLexicon.MESSAGE_REFERENCE, MESSAGES, messageName);
        }

        processDocumentation(fault, path);
        processExtensibilityElements(fault, path);
    }

    protected void processBindings( Map<String, Binding> bindings,
                                    Path parentPath ) throws Exception {
        Path bindingsPath = null;
        for (Binding binding : bindings.values()) {
            if (bindingsPath == null) {
                bindingsPath = nextPath(parentPath, WsdlLexicon.BINDINGS);
                output.setProperty(bindingsPath, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.BINDINGS);
            }
            processBinding(binding, bindingsPath);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processBinding( Binding binding,
                                   Path parentPath ) throws Exception {
        QName qname = binding.getQName();
        Path path = nextPath(parentPath, qname, WsdlLexicon.BINDING);
        setReferenceable(path, BINDINGS, qname);
        PortType portType = binding.getPortType();
        QName typeName = null;
        if (portType != null) {
            typeName = portType.getQName();
            output.setProperty(path, WsdlLexicon.TYPE_NAME, typeName.getLocalPart());
            output.setProperty(path, WsdlLexicon.TYPE_NAMESPACE, typeName.getNamespaceURI());
            setReference(path, WsdlLexicon.TYPE_REFERENCE, PORT_TYPES, typeName);
        }

        binding.getBindingOperations();
        for (BindingOperation operation : (List<BindingOperation>)binding.getBindingOperations()) {
            process(operation, path);
        }

        processDocumentation(binding, path);
        processExtensibilityElements(binding, path);
    }

    @SuppressWarnings( "unchecked" )
    protected void process( BindingOperation operation,
                            Path parentPath ) throws Exception {
        String operationName = operation.getName();
        Path path = nextPath(parentPath, name(operationName));
        output.setProperty(path, WsdlLexicon.NC_NAME, operationName);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.BINDING_OPERATION);

        Operation portTypeOperation = operation.getOperation();
        OperationType type = portTypeOperation.getStyle();
        process(operation.getBindingInput(), path, type, operationName, portTypeOperation);
        process(operation.getBindingOutput(), path, type, operationName, portTypeOperation);
        for (BindingFault fault : ((Map<String, BindingFault>)operation.getBindingFaults()).values()) {
            process(fault, path);
        }

        processDocumentation(operation, path);
        processExtensibilityElements(operation, path);
    }

    protected void process( BindingInput input,
                            Path parentPath,
                            OperationType style,
                            String operationName,
                            Operation operation ) throws Exception {
        if (input == null) return;
        String name = input.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) name = operationName;
            else if (OperationType.NOTIFICATION.equals(style)) name = operationName;
            else if (OperationType.REQUEST_RESPONSE.equals(style)) name = operationName + "Request";
            else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Solicit";
        }
        Path path = nextPath(parentPath, WsdlLexicon.INPUT);
        output.setProperty(path, WsdlLexicon.NC_NAME, name);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.BINDING_OPERATION_INPUT);
        // Find the port type's input ...
        Input opInput = operation.getInput();
        output.setProperty(path, WsdlLexicon.INPUT_REFERENCE, uuidForComponent(opInput));
        output.setProperty(path, WsdlLexicon.INPUT_NAME, nameForComponent.get(opInput));

        processDocumentation(input, path);
        processExtensibilityElements(input, path);
    }

    protected void process( BindingOutput out,
                            Path parentPath,
                            OperationType style,
                            String operationName,
                            Operation operation ) throws Exception {
        if (out == null) return;
        String name = out.getName();
        if (name == null) {
            if (OperationType.ONE_WAY.equals(style)) name = operationName;
            else if (OperationType.NOTIFICATION.equals(style)) name = operationName;
            else if (OperationType.REQUEST_RESPONSE.equals(style)) name = operationName + "Response";
            else if (OperationType.SOLICIT_RESPONSE.equals(style)) name = operationName + "Response";
        }
        Path path = nextPath(parentPath, WsdlLexicon.OUTPUT);
        output.setProperty(path, WsdlLexicon.NC_NAME, name);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.BINDING_OPERATION_OUTPUT);
        // Find the port type's output ...
        Output opOutput = operation.getOutput();
        output.setProperty(path, WsdlLexicon.OUTPUT_REFERENCE, uuidForComponent(opOutput));
        output.setProperty(path, WsdlLexicon.OUTPUT_NAME, nameForComponent.get(opOutput));

        processDocumentation(out, path);
        processExtensibilityElements(out, path);
    }

    protected void process( BindingFault fault,
                            Path parentPath ) throws Exception {
        if (fault == null) return;
        Path path = nextPath(parentPath, WsdlLexicon.FAULT);
        output.setProperty(path, WsdlLexicon.NC_NAME, fault.getName());
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.BINDING_OPERATION_FAULT);

        processDocumentation(fault, path);
        processExtensibilityElements(fault, path);
    }

    protected void processServices( Map<String, Service> services,
                                    Path parentPath ) throws Exception {
        Path servicePath = null;
        for (Service service : services.values()) {
            if (servicePath == null) {
                servicePath = nextPath(parentPath, WsdlLexicon.SERVICES);
                output.setProperty(servicePath, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SERVICES);
            }
            processService(service, servicePath);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void processService( Service service,
                                   Path parentPath ) throws Exception {
        QName qname = service.getQName();
        Path path = nextPath(parentPath, qname, WsdlLexicon.SERVICE);
        setReferenceable(path, SERVICES, qname);

        for (Port port : ((Map<String, Port>)service.getPorts()).values()) {
            process(port, path);
        }

        processDocumentation(service, path);
        processExtensibilityElements(service, path);
    }

    protected void process( Port port,
                            Path parentPath ) throws Exception {
        String name = port.getName();
        Path path = nextPath(parentPath, name(name));
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.PORT);
        QName bindingName = port.getBinding().getQName();
        setReference(path, WsdlLexicon.BINDING_REFERENCE, BINDINGS, bindingName);
        output.setProperty(path, WsdlLexicon.NC_NAME, name);

        processDocumentation(port, path);
        processExtensibilityElements(port, path);
    }

    protected void processDocumentation( WSDLElement wsdlElement,
                                         Path path ) throws Exception {
        if (wsdlElement == null) return;
        Element docElement = wsdlElement.getDocumentationElement();
        if (docElement == null) return;
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
        if (content == null) return;
        content = content.trim();
        if (content.length() != 0) output.setProperty(path, SrampLexicon.DESCRIPTION, cleanDocumentationContent(content));
    }

    /**
     * This method is used by the {@link #processDocumentation(WSDLElement, Path)} to clean up the documentation content string.
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
                                                 Path path ) throws Exception {
        if (wsdlElement == null) return;

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
                    prefixForNamespace(namespaceUri, attribute.getPrefix());
                    output.setProperty(path, name(namespaceUri, localName), value);
                } else {
                    output.setProperty(path, name(localName), value);
                }
            }
        }

        // Process the elements ...
        for (ExtensibilityElement element : (List<ExtensibilityElement>)wsdlElement.getExtensibilityElements()) {
            process(element, path);
        }
    }

    protected void process( ExtensibilityElement element,
                            Path parentPath ) throws Exception {
        if (element == null) return;
        if (element instanceof SOAP12Address) process((SOAP12Address)element, parentPath);
        else if (element instanceof SOAP12Binding) process((SOAP12Binding)element, parentPath);
        else if (element instanceof SOAP12Body) process((SOAP12Body)element, parentPath);
        else if (element instanceof SOAP12Fault) process((SOAP12Fault)element, parentPath);
        else if (element instanceof SOAP12Header) process((SOAP12Header)element, parentPath);
        else if (element instanceof SOAP12HeaderFault) process((SOAP12HeaderFault)element, parentPath);
        else if (element instanceof SOAP12Operation) process((SOAP12Operation)element, parentPath);
        else if (element instanceof SOAPAddress) process((SOAPAddress)element, parentPath);
        else if (element instanceof SOAPBinding) process((SOAPBinding)element, parentPath);
        else if (element instanceof SOAPBody) process((SOAPBody)element, parentPath);
        else if (element instanceof SOAPFault) process((SOAPFault)element, parentPath);
        else if (element instanceof SOAPHeader) process((SOAPHeader)element, parentPath);
        else if (element instanceof SOAPHeaderFault) process((SOAPHeaderFault)element, parentPath);
        else if (element instanceof SOAPOperation) process((SOAPOperation)element, parentPath);
        else if (element instanceof MIMEMultipartRelated) process((MIMEMultipartRelated)element, parentPath);
        else if (element instanceof MIMEContent) process((MIMEContent)element, parentPath);
        else if (element instanceof MIMEPart) process((MIMEPart)element, parentPath);
        else if (element instanceof MIMEMimeXml) process((MIMEMimeXml)element, parentPath);
        else if (element instanceof HTTPAddress) process((HTTPAddress)element, parentPath);
        else if (element instanceof HTTPBinding) process((HTTPBinding)element, parentPath);
        else if (element instanceof HTTPOperation) process((HTTPOperation)element, parentPath);
        else if (element instanceof HTTPUrlEncoded) process((HTTPUrlEncoded)element, parentPath);
        else if (element instanceof HTTPUrlReplacement) process((HTTPUrlReplacement)element, parentPath);
        else processUnknownExtensionElement(element, parentPath);
    }

    protected void process( HTTPAddress element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.HTTP_ADDRESS);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.HTTP_ADDRESS);
        output.setProperty(path, WsdlLexicon.LOCATION, element.getLocationURI());
    }

    protected void process( HTTPBinding element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.HTTP_BINDING);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.HTTP_BINDING);
        output.setProperty(path, WsdlLexicon.VERB, element.getVerb());
    }

    protected void process( HTTPOperation element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.HTTP_OPERATION);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.HTTP_OPERATION);
        output.setProperty(path, WsdlLexicon.LOCATION, element.getLocationURI());
    }

    protected void process( HTTPUrlEncoded element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.HTTP_URL_ENCODED);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.HTTP_URL_ENCODED);
    }

    protected void process( HTTPUrlReplacement element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.HTTP_URL_REPLACEMENT);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.HTTP_URL_REPLACEMENT);
    }

    protected void process( SOAP12Address element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_ADDRESS);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_ADDRESS);
        output.setProperty(path, WsdlLexicon.SOAP_LOCATION, element.getLocationURI());
    }

    protected void process( SOAP12Binding element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_BINDING);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_BINDING);
        output.setProperty(path, WsdlLexicon.STYLE, element.getStyle());
        output.setProperty(path, WsdlLexicon.TRANSPORT, element.getTransportURI());
    }

    protected void process( SOAP12Operation element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_OPERATION);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_OPERATION);
        output.setProperty(path, WsdlLexicon.STYLE, element.getStyle());
        output.setProperty(path, WsdlLexicon.SOAP_ACTION, element.getSoapActionURI());
    }

    protected void process( SOAP12Body element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_BODY);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_BODY);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        output.setProperty(path, WsdlLexicon.PARTS, element.getParts());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
    }

    protected void process( SOAP12Fault element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_FAULT);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_FAULT);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
    }

    @SuppressWarnings( "unchecked" )
    protected void process( SOAP12Header element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_HEADER);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_HEADER);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        output.setProperty(path, WsdlLexicon.MESSAGE, element.getMessage());
        output.setProperty(path, WsdlLexicon.PART, element.getPart());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
        for (SOAP12HeaderFault fault : (List<SOAP12HeaderFault>)element.getSOAP12HeaderFaults()) {
            process(fault, path);
        }
    }

    protected void process( SOAP12HeaderFault element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_HEADER_FAULT);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_HEADER_FAULT);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyle());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
    }

    protected void process( SOAPAddress element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_ADDRESS);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_ADDRESS);
        output.setProperty(path, WsdlLexicon.SOAP_LOCATION, element.getLocationURI());
    }

    protected void process( SOAPBinding element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_BINDING);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_BINDING);
        output.setProperty(path, WsdlLexicon.STYLE, element.getStyle());
        output.setProperty(path, WsdlLexicon.TRANSPORT, element.getTransportURI());
    }

    protected void process( SOAPOperation element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_OPERATION);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_OPERATION);
        output.setProperty(path, WsdlLexicon.STYLE, element.getStyle());
        output.setProperty(path, WsdlLexicon.SOAP_ACTION, element.getSoapActionURI());
    }

    protected void process( SOAPBody element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_BODY);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_BODY);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyles());
        output.setProperty(path, WsdlLexicon.PARTS, element.getParts());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
    }

    protected void process( SOAPFault element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_FAULT);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_FAULT);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyles());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
    }

    @SuppressWarnings( "unchecked" )
    protected void process( SOAPHeader element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_HEADER);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_HEADER);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyles());
        output.setProperty(path, WsdlLexicon.MESSAGE, element.getMessage());
        output.setProperty(path, WsdlLexicon.PART, element.getPart());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
        for (SOAP12HeaderFault fault : (List<SOAP12HeaderFault>)element.getSOAPHeaderFaults()) {
            process(fault, path);
        }
    }

    protected void process( SOAPHeaderFault element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.SOAP_HEADER_FAULT);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.SOAP_HEADER_FAULT);
        output.setProperty(path, WsdlLexicon.ENCODING_STYLE, element.getEncodingStyles());
        output.setProperty(path, WsdlLexicon.USE, element.getUse());
    }

    @SuppressWarnings( "unchecked" )
    protected void process( MIMEMultipartRelated element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.MIME_MULTIPART_RELATED);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.MIME_MULTIPART_RELATED);
        for (MIMEPart part : (List<MIMEPart>)element.getMIMEParts()) {
            process(part, path);
        }
    }

    @SuppressWarnings( "unchecked" )
    protected void process( MIMEPart element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.MIME_PART);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.MIME_PART);
        for (ExtensibilityElement child : (List<ExtensibilityElement>)element.getExtensibilityElements()) {
            process(child, path);
        }
    }

    protected void process( MIMEContent element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.MIME_CONTENT);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.MIME_CONTENT);
        output.setProperty(path, WsdlLexicon.MIME_PART, element.getPart());
        output.setProperty(path, WsdlLexicon.MIME_TYPE, element.getType());
    }

    protected void process( MIMEMimeXml element,
                            Path parentPath ) throws Exception {
        Path path = nextPath(parentPath, WsdlLexicon.MIME_XML);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, WsdlLexicon.MIME_XML);
        output.setProperty(path, WsdlLexicon.MIME_PART, element.getPart());
    }

    /**
     * This method is called by {@link #process(ExtensibilityElement, Path)} when the type of extension is not known. By default
     * this method does nothing, but subclasses can override this method to specialize the behavior.
     * 
     * @param element the extensibility element
     * @param parentPath the path of the parent node; never null
     * @throws Exception if there is a problem processing the element
     */
    protected void processUnknownExtensionElement( ExtensibilityElement element,
                                                   Path parentPath ) throws Exception {
        // ignore for now
    }

    protected boolean is( QName qname,
                          Name name ) {
        return qname != null && name.getLocalName().equals(qname.getLocalPart())
               && name.getNamespaceUri().equals(qname.getNamespaceURI());
    }

    protected UUID setReferenceable( Path path,
                                     SymbolSpace space,
                                     QName qname ) {
        return setReferenceable(path, space, qname.getNamespaceURI(), qname.getLocalPart());
    }

    protected UUID setReferenceable( Path path,
                                     SymbolSpace space,
                                     String namespace,
                                     String name ) {
        UUID uuid = setUuid(path);
        resolvers.get(space).register(namespace, name, path, uuid);
        return uuid;
    }

    protected UUID setReference( Path path,
                                 Name propertyName,
                                 SymbolSpace space,
                                 QName name ) {
        return setReference(path, propertyName, space, name.getNamespaceURI(), name.getLocalPart());
    }

    protected String writeXml( Node xmlNode ) throws TransformerFactoryConfigurationError, TransformerException {
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

    protected Path nextPath( Path parentPath,
                             QName qname,
                             Name primaryType ) {
        String localName = qname.getLocalPart();
        String namespace = qname.getNamespaceURI();
        Path path = nextPath(parentPath, name(localName));
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, primaryType);
        output.setProperty(path, WsdlLexicon.NC_NAME, localName);
        output.setProperty(path, WsdlLexicon.NAMESPACE, namespace);
        return path;
    }

    protected UUID uuidForComponent( WSDLElement component ) {
        UUID uuid = uuidForComponent.get(component);
        if (uuid == null) {
            uuid = context.getValueFactories().getUuidFactory().create();
            uuidForComponent.put(component, uuid);
        }
        return uuid;
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
