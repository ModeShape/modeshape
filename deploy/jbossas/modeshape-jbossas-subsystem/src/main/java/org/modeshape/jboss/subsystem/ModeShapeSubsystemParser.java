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
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
			ArrayList<String> repositories = new ArrayList<String>(node.get(
					Element.REPOSITORY_ELEMENT.getLocalName()).keys());
			Collections.sort(repositories);
			if (!repositories.isEmpty()) {
				for (String repository : repositories) {
					writer.writeStartElement(Element.REPOSITORY_ELEMENT
							.getLocalName());
					writeRepositoryConfiguration(writer, node.get(
							Element.REPOSITORY_ELEMENT.getLocalName(),
							repository), repository);
				}
			}
		}

		writer.writeEndElement(); // End of subsystem element

	}

	// write the elements according to the schema defined.
	private void writeRepositoryConfiguration(XMLExtendedStreamWriter writer,
			ModelNode node, String repositoryName) throws XMLStreamException {

		writeAttribute(writer, Element.REPOSITORY_NAME_ATTRIBUTE, node);
		writeAttribute(writer, Element.REPOSITORY_JNDI_NAME_ATTRIBUTE, node);

		if (like(node, Element.SEQUENCER_ELEMENT)) {
			writer.writeStartElement(Element.SEQUENCING_ELEMENT.getLocalName());
			List<Property> sequencerList = node.get(
					Element.SEQUENCER_ELEMENT.getLocalName()).asPropertyList(); // sequencers
			for (Property sequencer : sequencerList) {
				writer.writeStartElement(Element.SEQUENCER_ELEMENT
						.getLocalName());
				writer.writeAttribute(
						Element.SEQUENCER_NAME_ATTRIBUTE.getLocalName(),
						sequencer.getName());
				writePropertyAttribute(
						writer,
						sequencer
								.getValue()
								.get(Element.SEQUENCER_DESCRIPTION_ATTRIBUTE
										.getModelName()).asString(),
										Element.SEQUENCER_DESCRIPTION_ATTRIBUTE);
				writePropertyAttribute(
						writer,
						sequencer
								.getValue()
								.get(Element.SEQUENCER_TYPE_ATTRIBUTE
										.getModelName()).asString(),
										Element.SEQUENCER_TYPE_ATTRIBUTE);
				writePropertyAttribute(
						writer,
						sequencer
								.getValue()
								.get(Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE
										.getModelName()).asString(),
										Element.SEQUENCER_EXPRESSIONS_ATTRIBUTE);
				writer.writeEndElement();
			}
			writer.writeEndElement();
		}
		writer.writeEndElement();
	}

	@Override
	public void readElement(final XMLExtendedStreamReader reader,
			final List<ModelNode> list) throws XMLStreamException {

		final ModelNode subsystem = new ModelNode();
		subsystem.add(SUBSYSTEM, ModeShapeExtension.MODESHAPE_SUBSYSTEM);
		subsystem.protect();

		final ModelNode bootServices = new ModelNode();
		bootServices.get(OP).set(ADD);
		bootServices.get(OP_ADDR).set(subsystem);
		list.add(bootServices);

		// no attributes
		requireNoAttributes(reader);

		final List<ModelNode> repositories = new ArrayList<ModelNode>();

		while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
			if (reader.isStartElement()) {
				// elements
				switch (Namespace.forUri(reader.getNamespaceURI())) {
				case MODESHAPE_3_0: {
					Element element = Element.forName(reader.getLocalName());
					switch (element) {
					case REPOSITORY_ELEMENT:
						parseRepository(reader, subsystem, repositories);
						break;
					default:
						throw ParseUtils.unexpectedElement(reader);
					}
				}

				}
			}
		}

		list.addAll(repositories);

	}

	private void parseRepository(final XMLExtendedStreamReader reader,
			final ModelNode address, final List<ModelNode> repositories)
			throws XMLStreamException {

		final ModelNode repository = new ModelNode();
		final ModelNode repositoryAddress = address.clone();

		repository.get(OP).set(ADD);
		String repositoryName = null;
		List<ModelNode> sequencers = new ArrayList<ModelNode>();
		if (reader.getAttributeCount() > 0) {
			for (int i = 0; i < reader.getAttributeCount(); i++) {
				String attrName = reader.getAttributeLocalName(i);
				String attrValue = reader.getAttributeValue(i);
				Element element = Element.forName(attrName);
				switch (element) {
				case REPOSITORY_NAME_ATTRIBUTE:
					repositoryName = attrValue;
					repositoryAddress.add("repository", attrValue); //$NON-NLS-1$
					repositoryAddress.protect();
					repository.get(OP).set(ADD);
					repository.get(OP_ADDR).set(repositoryAddress);
					repository.get(element.getModelName()).set(attrValue);
					repositories.add(repository);
					break;
				case REPOSITORY_JNDI_NAME_ATTRIBUTE:
					repository.get(element.getModelName()).set(attrValue);
					break;
				default:
					throw ParseUtils.unexpectedAttribute(reader, i);
				}
			}
		}

		while (reader.hasNext()
				&& (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
			Element element = Element.forName(reader.getLocalName());
			switch (element) {
			case SEQUENCING_ELEMENT:
				sequencers = parseSequencers(reader, address, repositoryName);
				break;
			default:
				throw ParseUtils.unexpectedElement(reader);
			}
		}

		// while (reader.hasNext()
		// && (reader.nextTag() != XMLStreamConstants.END_ELEMENT))
		// ;

		repositories.addAll(sequencers);

	}

	private List<ModelNode> parseSequencers(
			final XMLExtendedStreamReader reader,
			final ModelNode parentAddress, final String repositoryName)
			throws XMLStreamException {
		requireNoAttributes(reader);

		List<ModelNode> sequencers = new ArrayList<ModelNode>();
		while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
			final Element element = Element.forName(reader.getLocalName());
			switch (element) {
			case SEQUENCER_ELEMENT: {
				parseSequencer(reader, repositoryName, sequencers);
				break;
			}
			default: {
				throw ParseUtils.unexpectedElement(reader);
			}
			}
		}

		return sequencers;
	}

	private void parseSequencer(XMLExtendedStreamReader reader,
			String repositoryName, final List<ModelNode> sequencers)
			throws XMLStreamException {

		final ModelNode sequencer = new ModelNode();
		sequencer.get(OP).set(ADD);
		String name = null;

		sequencers.add(sequencer);

		if (reader.getAttributeCount() > 0) {
			for (int i = 0; i < reader.getAttributeCount(); i++) {
				String attrName = reader.getAttributeLocalName(i);
				String attrValue = reader.getAttributeValue(i);
				Element element = Element.forName(attrName,
						Element.SEQUENCER_ELEMENT);

				switch (element) {
				case SEQUENCER_NAME_ATTRIBUTE:
					name = attrValue;
					sequencer.get(element.getModelName()).set(attrValue);
					break;

				case SEQUENCER_DESCRIPTION_ATTRIBUTE:
					sequencer.get(element.getModelName()).set(attrValue);
					break;

				case SEQUENCER_EXPRESSIONS_ATTRIBUTE:
					sequencer.get(element.getModelName()).set(attrValue);
					break;

				case SEQUENCER_TYPE_ATTRIBUTE:
					sequencer.get(element.getModelName()).set(attrValue);
					break;

				default:
					throw ParseUtils.unexpectedAttribute(reader, i);
				}
			}
		}

		while (reader.hasNext()
				&& (reader.nextTag() != XMLStreamConstants.END_ELEMENT))
			;

		sequencer.get(OP_ADDR)
				.add(SUBSYSTEM, ModeShapeExtension.MODESHAPE_SUBSYSTEM)
				.add(Element.REPOSITORY_ELEMENT.getLocalName(), repositoryName)
				.add(Element.SEQUENCER_ELEMENT.getLocalName(), name);

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

	private void writePropertyAttribute(final XMLExtendedStreamWriter writer,
			final String value, final Element element) throws XMLStreamException {
		if (value != null &! value.equals("undefined")) { //$NON-NLS-1$
			if (!element.sameAsDefault(value)) {
				writer.writeAttribute(element.getLocalName(), value);
			}
		}
	}

	private boolean like(ModelNode node, Element element) {
		if (node.isDefined()) {
			Set<String> keys = node.keys();
			for (String key : keys) {
				if (key.startsWith(element.getLocalName())) {
					return true;
				}
			}
		}
		return false;
	}
}
