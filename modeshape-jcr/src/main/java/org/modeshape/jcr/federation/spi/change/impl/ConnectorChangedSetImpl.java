package org.modeshape.jcr.federation.spi.change.impl;

import java.util.Map;

import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.federation.spi.change.ConnectorChangedSet;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.ValueFactories;

public class ConnectorChangedSetImpl implements ConnectorChangedSet {

	private PathFactory pathFactory;
	private String processKey;
	private String repositoryKey;
	private ChangeBus bus;

	private RecordingChanges events;

	public ConnectorChangedSetImpl(final String processKey,
			final String repositoryKey, final ValueFactories factories,
			final ChangeBus bus) {
		this.bus = bus;
		this.pathFactory = factories.getPathFactory();
		this.processKey = processKey;
		this.repositoryKey = repositoryKey;
		this.events = new RecordingChanges(processKey, repositoryKey);
	}

	@Override
	public void nodeCreated(final String key, final String parentKey,
			final String path, final Map<Name, Property> properties) {
		events.nodeCreated(nodeKey(key), nodeKey(parentKey), path(path),
				properties);
	}

	@Override
	public void nodeRemoved(final String key, final String parentKey,
			final String path) {
		events.nodeRemoved(nodeKey(key), nodeKey(parentKey), path(path));
	}

	@Override
	public void nodeMoved(final String key, final String newParent,
			final String oldParent, final String newPath, final String oldPath) {
		events.nodeMoved(nodeKey(key), nodeKey(newParent), nodeKey(oldParent),
				path(newPath), path(oldPath));
	}

	@Override
	public void nodeReordered(final String key, final String parent,
			final String newRelativePath, final String oldRelativePath,
			final String reorderedBeforeRelativePath) {
		events.nodeReordered(nodeKey(key), nodeKey(parent),
				path(newRelativePath), path(oldRelativePath),
				path(reorderedBeforeRelativePath));
	}

	@Override
	public void propertyAdded(final String key, final String nodePath,
			final Property property) {
		events.propertyAdded(nodeKey(key), path(nodePath), property);
	}

	@Override
	public void propertyRemoved(final String key, final String nodePath,
			final Property property) {
		events.propertyRemoved(nodeKey(key), path(nodePath), property);
	}

	@Override
	public void propertyChanged(final String key, final String nodePath,
			Property newProperty, Property oldProperty) {
		events.propertyChanged(nodeKey(key), path(nodePath), newProperty,
				oldProperty);
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
		return pathFactory.create(p);
	}

	private NodeKey nodeKey(final String k) {
		return new NodeKey(k);
	}

}
