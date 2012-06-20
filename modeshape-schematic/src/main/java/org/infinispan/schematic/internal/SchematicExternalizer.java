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
package org.infinispan.schematic.internal;

import java.util.Set;
import org.infinispan.marshall.AdvancedExternalizer;
import org.infinispan.marshall.Externalizer;

/**
 * An abstract base class for all Schematic {@link org.infinispan.marshall.Externalizer Externalizer} implementations.
 * <p>
 * There are two primary advantages of implementing {@link AdvancedExternalizer} versus implementing
 * {@link org.infinispan.marshall.Externalizer}:
 * <ol>
 * <li>The class to be externalized must be modified with a {@code @SerializeWith} annotation. Since {@link SchematicEntryLiteral}
 * is our class, this is possible.</li>
 * <li>The generated payloads (shipped between Infinispan processes) are slightly smaller, since it includes only the
 * {@link AdvancedExternalizer}'s {@link #getId() identifier} instead of the class information and/or the serialized
 * {@link org.infinispan.marshall.Externalizer}. But for this to work, the advanced externalizers need to be registered with the
 * cache container at startup.</li>
 * </ol>
 * Unfortunately, Infinispan in AS7.1 does not allow defining {@link AdvancedExternalizer} classes for the cache container, and
 * thus they cannot be used (see <a href="https://issues.jboss.org/browse/MODE-1524">MODE-1524</a> for details). Therefore, we
 * have to rely only upon implementing {@link org.infinispan.marshall.Externalizer}.
 * </p>
 * <p>
 * This abstract class was created so that the Schematic externalizers can easily extend it, but so that we can encapsulate in
 * this abstract class the use of {@link Externalizer} or {@link AdvancedExternalizer}. Once Infinispan in AS7 supports
 * user-defined advanced externalizers, then we can simply change this class to implement {@link AdvancedExternalizer} rather than
 * {@link Externalizer}.
 * 
 * @param <T> the type of class
 */
public abstract class SchematicExternalizer<T> implements org.infinispan.marshall.Externalizer<T>
/*implements AdvancedExternalizer<T>*/{

    private static final long serialVersionUID = 1L;

    public abstract Integer getId();

    public abstract Set<Class<? extends T>> getTypeClasses();

}
