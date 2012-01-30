/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.parsing.ParseUtils.requireNoAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

public class ModeShapeSubsystemParser implements XMLStreamConstants,
		XMLElementReader<List<ModelNode>>,
		XMLElementWriter<SubsystemMarshallingContext> {

	@Override
	public void writeContent(XMLExtendedStreamWriter writer,
			SubsystemMarshallingContext context) throws XMLStreamException {
		context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
		ModelNode node = context.getModelNode();
		if (!node.isDefined()) {
			return;
		}

		if (has(node, Element.REPOSITORY_ELEMENT.getLocalName())) {
	    	ArrayList<String> repositories = new ArrayList<String>(node.get(Element.REPOSITORY_ELEMENT.getLocalName()).keys());
	    	Collections.sort(repositories);
	    	if (!repositories.isEmpty()) {
	    		for (String repository:repositories) {
	    	        writer.writeStartElement(Element.REPOSITORY_ELEMENT.getLocalName());
	    	        writeRepositoryConfiguration(writer, node.get(Element.REPOSITORY_ELEMENT.getLocalName(), repository), repository);
	    	        writer.writeEndElement();    			
	    		}
	    	}        
    	}   
		
		writer.writeEndElement(); // End of subsystem element
		
	}
	
	 // write the elements according to the schema defined.
    private void writeRepositoryConfiguration( XMLExtendedStreamWriter writer, ModelNode node, String repositoryName) throws XMLStreamException {
    	
    	writeAttribute(writer, Element.REPOSITORY_NAME_ATTRIBUTE, node);
    	writeAttribute(writer, Element.REPOSITORY_JNDI_NAME_ATTRIBUTE, node);
    	writeAttribute(writer, Element.REPOSITORY_ROOT_NODE_ID_ATTRIBUTE, node);
    	writeAttribute(writer, Element.REPOSITORY_LARGE_VALUE_SIZE_ID_ATTRIBUTE, node);;
    	
    }

	@Override
	public void readElement(final XMLExtendedStreamReader reader,
			final List<ModelNode> list) throws XMLStreamException {
		final ModelNode address = new ModelNode();
		address.add(SUBSYSTEM, ModeShapeExtension.MODESHAPE_SUBSYSTEM);
		address.protect();

		final ModelNode bootServices = new ModelNode();
		bootServices.get(OP).set(ADD);
		bootServices.get(OP_ADDR).set(address);
		list.add(bootServices);

		// no attributes
		requireNoAttributes(reader);

		while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
			if (reader.isStartElement()) {
				// elements
					switch (Namespace.forUri(reader.getNamespaceURI())) {
					case MODESHAPE_3_0: {
						Element element = Element
								.forName(reader.getLocalName());
						switch (element) {
						case REPOSITORY_ELEMENT:
							ModelNode repository = new ModelNode();
							String name = parseRepository(reader, repository);
							if (name != null) {
								final ModelNode repositoryName = address.clone();
								repositoryName.add("repository", name); //$NON-NLS-1$
								repositoryName.protect();
		                        repository.get(OP).set(ADD);
		                        repository.get(OP_ADDR).set(repositoryName);
		                        list.add(repository);  
		                        ParseUtils.requireNoContent(reader);
			                }
	                        else {
	                        	throw new XMLStreamException();
	                        }
	                        break;

						default:
							throw ParseUtils.unexpectedElement(reader);
						}
					}

					}
				}
			}
		 
		
	}

	private String parseRepository(XMLExtendedStreamReader reader,
			ModelNode node) throws XMLStreamException {
		String repositoryName = null;
		if (reader.getAttributeCount() > 0) {
			for (int i = 0; i < reader.getAttributeCount(); i++) {
				String attrName = reader.getAttributeLocalName(i);
				String attrValue = reader.getAttributeValue(i);
				Element element = Element.forName(attrName);
				switch (element) {
				case REPOSITORY_NAME_ATTRIBUTE:
					repositoryName = attrValue;
					node.get(element.getModelName()).set(attrValue);
					break;
				case REPOSITORY_JNDI_NAME_ATTRIBUTE:
    				node.get(element.getModelName()).set(attrValue);
    				break;
				case REPOSITORY_ROOT_NODE_ID_ATTRIBUTE:
    				node.get(element.getModelName()).set(attrValue);
    				break;
				case REPOSITORY_LARGE_VALUE_SIZE_ID_ATTRIBUTE:
    				node.get(element.getModelName()).set(attrValue);
    				break;	
				default:
					throw ParseUtils.unexpectedAttribute(reader, i);
				}
			}
		}

 	    return repositoryName;
	}

		private boolean has(ModelNode node, String name) {
		return node.has(name) && node.get(name).isDefined();
	}

	private void writeAttribute(final XMLExtendedStreamWriter writer,
			final Element element, final ModelNode node)
			throws XMLStreamException {
		if (has(node, element.getModelName())) {
			String value = node.get(element.getModelName()).asString();
			if (!element.sameAsDefault(value)) {
				writer.writeAttribute(element.getLocalName(), value);
			}
		}
	}
}
