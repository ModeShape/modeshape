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
package org.modeshape.jcr.api.observation;

import java.util.List;

/**
 * Extension of the {@link javax.jcr.observation.Event} interface allowing clients to retrieve extra information from events
 * generated around properties.
 * 
 * @author Horia Chiorean
 * @see javax.jcr.observation.Event#PROPERTY_CHANGED
 * @see javax.jcr.observation.Event#PROPERTY_ADDED
 * @see javax.jcr.observation.Event#PROPERTY_REMOVED
 */
public interface PropertyEvent extends Event {

    /**
     * Returns {@code true} if this property is multi-valued and {@code false} if this property is single-valued.
     * 
     * @return whether this property has multiple values or a single value.
     */
    public boolean isMultiValue();

    /**
     * Returns the single value of the property if it's not a multi-value property, or the first value from the list of values if
     * the property is multi-valued.
     * 
     * @return a {@code Object} corresponding to the value of the property or {@code null} if the property has no value
     */
    public Object getCurrentValue();

    /**
     * Returns all the values of the property. If the property is single-valued, it will return a list with 1 element.
     * 
     * @return a {@code List} with all the values of the property, never {@code null}
     */
    public List<?> getCurrentValues();

    /**
     * In case of a {@link javax.jcr.observation.Event#PROPERTY_CHANGED} event, returns {@code true} if the old property was
     * multi-valued and {@code false} if the old property was single-valued.
     * <p/>
     * For all other property events, this will return {@code false}.
     * 
     * @return whether the old property had multiple values or a single value, or {@code false} if the event type is not
     *         {@link javax.jcr.observation.Event#PROPERTY_CHANGED}
     */
    public boolean wasMultiValue();

    /**
     * In case of a {@link javax.jcr.observation.Event#PROPERTY_CHANGED} event, returns the single value of the old property if it
     * wasn't a multi-value property, or the first value from the list of values if the property was multi-valued.
     * 
     * @return a {@code Object} corresponding to the value of the old property or {@code null} if the old property had no value or
     *         if the event is not a{@link javax.jcr.observation.Event#PROPERTY_CHANGED} event
     */
    public Object getPreviousValue();

    /**
     * In case of a {@link javax.jcr.observation.Event#PROPERTY_CHANGED} event, returns all the values of the old property. If the
     * property was single-valued, it will return a list with 1 element.
     * 
     * @return a {@code List} with all the values of the old property or {@code null} if the event is not a
     *         {@link javax.jcr.observation.Event#PROPERTY_CHANGED} event.
     */
    public List<?> getPreviousValues();
}
