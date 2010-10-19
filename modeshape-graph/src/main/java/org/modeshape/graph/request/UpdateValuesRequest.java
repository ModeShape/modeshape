package org.modeshape.graph.request;

import java.util.Collections;
import java.util.List;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * Instruction to update the values for a certain property on the node at the specified location.
 * <p>
 * This request is capable of specifying specific values for the property that will be added or removed. Other values for the
 * property not be affected by this request. The request contains a workspace name and a location that uniquely identify a node in
 * the workspace as well as the name of property (that may or may not previously exist) on the node. The request also contains
 * zero or more values to add and zero or more values to remove from the property. All values will be appended to the list of
 * values. Removals are processed before additions.
 * </p>
 * <p>
 * Even if the property has no values after this call, the property itself will not be removed by this request.
 * </p>
 * <p>
 * Note that the number of values in a property (e.g., {@link Property#size()}, {@link Property#isEmpty()},
 * {@link Property#isSingle()}, and {@link Property#isMultiple()}) has no influence on whether the property should be removed. It
 * is possible for a property to have no values.
 * </p>
 */
public class UpdateValuesRequest extends ChangeRequest implements PropertyChangeRequest {

    private static final long serialVersionUID = 1L;

    private final String workspaceName;
    private final Location on;
    private final Name propertyName;
    private final List<Object> addedValues;
    private final List<Object> removedValues;

    private Location actualLocation;
    private List<Object> actualAddedValues;
    private List<Object> actualRemovedValues;
    private boolean actualCreation;
    private Property actualProperty;

    public UpdateValuesRequest( String workspaceName,
                                Location on,
                                Name propertyName,
                                List<Object> addedValues,
                                List<Object> removedValues ) {
        super();

        assert workspaceName != null;
        assert on != null;
        assert propertyName != null;

        this.workspaceName = workspaceName;
        this.on = on;
        this.propertyName = propertyName;
        this.addedValues = addedValues == null ? Collections.emptyList() : addedValues;
        this.removedValues = removedValues == null ? Collections.emptyList() : removedValues;
    }

    /**
     * Get the location defining the node that is to be updated.
     * 
     * @return the location of the node; never null
     */
    public Location on() {
        return on;
    }

    /**
     * Get the name of the property that is to be updated.
     * 
     * @return the name of the property; never null
     */
    public Name property() {
        return propertyName;
    }

    /**
     * Get the name of the workspace in which the node exists.
     * 
     * @return the name of the workspace; never null
     */
    public String inWorkspace() {
        return workspaceName;
    }

    /**
     * Get the list of values to be added.
     * 
     * @return the values (if any) to be added; never null
     */
    public List<Object> addedValues() {
        return addedValues;
    }

    /**
     * Get the list of values to be removed.
     * 
     * @return the values (if any) to be removed; never null
     */
    public List<Object> removedValues() {
        return removedValues;
    }

    @Override
    public Location changedLocation() {
        return on;
    }

    @Override
    public String changedWorkspace() {
        return workspaceName;
    }

    @Override
    public boolean changes( String workspace,
                            Path path ) {
        return workspaceName.equals(workspace) && on.hasPath() && on.getPath().equals(path);
    }

    @Override
    public boolean isReadOnly() {
        return addedValues.isEmpty() && removedValues.isEmpty();
    }

    public void setActualLocation( Location actual,
                                   List<Object> actualAddedValues,
                                   List<Object> actualRemovedValues ) {
        checkNotFrozen();
        CheckArg.isNotNull(actual, "actual");
        if (!actual.hasPath()) {
            throw new IllegalArgumentException(GraphI18n.actualLocationMustHavePath.text(actual));
        }
        this.actualLocation = actual;
        assert actualLocation != null;

        assert actualAddedValues != null;
        assert actualAddedValues.size() <= addedValues.size();
        assert actualRemovedValues != null;
        assert actualRemovedValues.size() <= actualRemovedValues.size();

        this.actualAddedValues = actualAddedValues;
        this.actualRemovedValues = actualRemovedValues;
    }

    /**
     * Record that the property did not exist prior to the processing of this request and was actually created by this request.
     * This method must be called when processing the request, and the actual location must have a {@link Location#getPath() path}
     * .
     * 
     * @param property the property being created or updated (may not be <code>null</code>)
     * @param created true if the property was created by this request, or false if this request updated an existing property
     * @throws IllegalStateException if the request is frozen
     * @throws IllegalArgumentException if the property is <code>null</code>
     */
    public void setActualProperty( Property property,
                                   boolean created ) {
        CheckArg.isNotNull(property, "property");
        checkNotFrozen();
        this.actualProperty = property;
        this.actualCreation = created;
    }

    /**
     * Get the actual node property that was created or updated.
     * 
     * @return the actual property or <code>null</code> if the actual property was not set
     */
    public Property getActualProperty() {
        return this.actualProperty;
    }

    /**
     * Get the actual location of the node that was updated.
     * 
     * @return the actual location, or null if the actual location was not set
     */
    public Location getActualLocationOfNode() {
        return actualLocation;
    }

    /**
     * Get the actual added values. This should always be identical to the list of values that were requested to be added.
     * 
     * @return the values that were added to the node when this request was processed; never null
     */
    public List<Object> getActualAddedValues() {
        return actualAddedValues;
    }

    /**
     * Get the actual removed values. This will differ from the values that were requested to be removed if some of the values
     * that were requested to be removed were not already values for the property.
     * 
     * @return the values that were removed from the node when this request was processed; never null
     */
    public List<Object> getActualRemovedValues() {
        return actualRemovedValues;
    }

    /**
     * Get whether the {@link #property() property} was created.
     * 
     * @return true if this request created the property, or false if this request changed an existing property
     */
    public boolean isNewProperty() {
        return actualCreation;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method does not clone the results.
     * </p>
     * 
     * @see org.modeshape.graph.request.ChangeRequest#clone()
     */
    @Override
    public UpdateValuesRequest clone() {
        UpdateValuesRequest request = new UpdateValuesRequest(workspaceName, actualLocation != null ? actualLocation : on,
                                                              propertyName, addedValues, removedValues);
        request.setActualLocation(actualLocation, actualAddedValues, actualRemovedValues);

        // don't call request.setActualProperty(Property, boolean) here as the actual property may have not been set
        request.actualProperty = actualProperty;
        request.actualCreation = actualCreation;

        return request;
    }

    @Override
    public RequestType getType() {
        return RequestType.UPDATE_VALUES;
    }
}
