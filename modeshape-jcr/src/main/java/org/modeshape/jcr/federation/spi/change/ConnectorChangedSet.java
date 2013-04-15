package org.modeshape.jcr.federation.spi.change;

import org.modeshape.jcr.value.Property;

/**
 * This interface represents an atomic set of changes that have occurred to
 * resources in the remit of a Connector. After construction, methods should be
 * called to record events of interest, which will be recorded as having
 * occurred in the order in which the methods were called.
 * 
 * When all events have been recorded, the publish() method should be called to
 * initiate the publication of the events in an atomic fashion into the
 * repository's change bus.
 * 
 * 
 * @author ajs6f
 * 
 */
public interface ConnectorChangedSet {

	/**
	 * Signal that a new node resource was created.
	 * 
	 * @param key
	 *            the key for the new node; may not be null
	 * @param parentKey
	 *            the key (node ID) for the parent of the new node; may not be null
	 */
	void nodeCreated(String key, String parentKey);

	/**
	 * Signal that a node resource was removed.
	 * 
	 * @param key
	 *            the key for the removed node; may not be null
	 */
	void nodeRemoved(String key);

	/**
	 * Signal that a node resource was moved from one parent to another.
	 * 
	 * @param key
	 *            the key for the node; may not be null
	 * @param newParent
	 *            the node ID of the new parent for the node; may not be null
	 * @param oldParent
	 *            the node ID of the old parent for the node; may not be null
	 */
	void nodeMoved(String key, String newParent, String oldParent);

	/**
	 * Signal that a node resource was placed into a new location within the
	 * same parent.
	 * 
	 * @param key
	 *            the key for the node; may not be null
	 * @param parent
	 *            the key (node ID) for the parent of the node; may not be null
	 * @param newPath
	 *            the new relative path for the node after it has been
	 *            reordered; may not be null
	 * @param oldPath
	 *            the old relative path for the node before it was reordered;
	 *            may not be null
	 * @param reorderedBeforePath
	 *            the relative path of the node before which the node was moved;
	 *            or null if the node was reordered to the end of the list of
	 *            children of the parent node
	 */
	void nodeReordered(String key, String parent, String newRelativePath,
			String oldRelativePath, String reorderedBeforeRelativePath);

	/**
	 * Create an event signifying that something about the node resource (other
	 * than the properties or location) changed.
	 * 
	 * @param key
	 *            the node key; may not be null
	 */
	void nodeChanged(String key);

	/**
	 * Signal that a property was added to a node resource.
	 * 
	 * @param key
	 *            the key of the node that was changed; may not be null
	 * @param property
	 *            the new property, with name and value(s); may not be null
	 */
	void propertyAdded(String key, Property property);

	/**
	 * Signal that a property was removed from a node resource.
	 * 
	 * @param key
	 *            the key of the node that was changed; may not be null
	 * @param property
	 *            the property that was removed, with name and value(s); may not
	 *            be null
	 */
	void propertyRemoved(String key, Property property);

	/**
	 * Signal that a property resource was changed on a node resource.
	 * 
	 * @param key
	 *            the key of the node that was changed; may not be null
	 * @param newProperty
	 *            the new property, with name and value(s); may not be null
	 * @param oldProperty
	 *            the old property, with name and value(s); may not be null
	 */
	void propertyChanged(String key, Property newProperty, Property oldProperty);

	/**
	 * Finish the construction of this change-set and make it available for
	 * publication into the repository.
	 */
	void publish();

	/**
	 * Removes any recorded change events.
	 */
	void reset();

}
