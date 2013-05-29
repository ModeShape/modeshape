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
