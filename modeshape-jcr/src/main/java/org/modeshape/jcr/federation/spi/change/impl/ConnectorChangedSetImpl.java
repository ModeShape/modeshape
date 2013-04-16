package org.modeshape.jcr.federation.spi.change.impl;

import java.util.Map;

import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.federation.spi.change.ConnectorChangedSet;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactories;

public class ConnectorChangedSetImpl implements ConnectorChangedSet {

	private ValueFactories factories;
	private String processKey;
	private String repositoryKey;
	private ChangeBus bus;

	private RecordingChanges events;

	public ConnectorChangedSetImpl(final String processKey,
			final String repositoryKey, final ValueFactories factories,
			final ChangeBus bus) {
		this.bus = bus;
		this.factories = factories;
		this.processKey = processKey;
		this.repositoryKey = repositoryKey;
		this.events = new RecordingChanges(processKey, repositoryKey);
	}

	@Override
	public void nodeCreated(String key, String parentKey, String path,
			Map<Name, Property> properties) {
		events.nodeCreated(nodeKey(key), nodeKey(parentKey), path(path),
				properties);
	}

	@Override
	public void nodeRemoved(String key, String parentKey, String path) {
		events.nodeRemoved(nodeKey(key), nodeKey(parentKey), path(path));
	}

	@Override
	public void nodeMoved(String key, String newParent, String oldParent,
			String newPath, String oldPath) {
		// TODO Auto-generated method stub

	}

	@Override
	public void nodeReordered(String key, String parent,
			String newRelativePath, String oldRelativePath,
			String reorderedBeforeRelativePath) {
		// TODO Auto-generated method stub

	}

	@Override
	public void propertyAdded(String key, String nodePath, Property property) {
		// TODO Auto-generated method stub

	}

	@Override
	public void propertyRemoved(String key, String nodePath, Property property) {
		// TODO Auto-generated method stub

	}

	@Override
	public void propertyChanged(String key, String nodePath,
			Property newProperty, Property oldProperty) {
		// TODO Auto-generated method stub

	}

	@Override
	public void publish() {
		bus.notify(events);
		events = new RecordingChanges(processKey, repositoryKey);
	}

	@Override
	public void reset() {
		events = new RecordingChanges(processKey, repositoryKey);
	}

	private Path path(final String p) {
		return factories.getPathFactory().create(p);
	}

	private NodeKey nodeKey(final String k) {
		return new NodeKey(k);
	}

}
