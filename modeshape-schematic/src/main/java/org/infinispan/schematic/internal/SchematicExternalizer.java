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
package org.infinispan.schematic.internal;

import java.util.Set;

/**
 * An abstract base class for all Schematic {@link org.infinispan.commons.marshall.Externalizer<T> Externalizer} implementations.
 * <p>
 * There are two primary advantages of implementing {@link org.infinispan.commons.marshall.AdvancedExternalizer} versus implementing
 * {@link org.infinispan.commons.marshall.Externalizer<T>}:
 * <ol>
 * <li>The class to be externalized must be modified with a {@code @SerializeWith} annotation. Since {@link SchematicEntryLiteral}
 * is our class, this is possible.</li>
 * <li>The generated payloads (shipped between Infinispan processes) are slightly smaller, since it includes only the
 * {@link org.infinispan.commons.marshall.AdvancedExternalizer}'s {@link #getId() identifier} instead of the class information and/or the serialized
 * {@link org.infinispan.commons.marshall.Externalizer<T>}. But for this to work, the advanced externalizers need to be registered with the
 * cache container at startup.</li>
 * </ol>
 * Unfortunately, Infinispan in AS7.1 does not allow defining {@link org.infinispan.commons.marshall.AdvancedExternalizer} classes for the cache container, and
 * thus they cannot be used (see <a href="https://issues.jboss.org/browse/MODE-1524">MODE-1524</a> for details). Therefore, we
 * have to rely only upon implementing {@link org.infinispan.commons.marshall.Externalizer<T>}.
 * </p>
 * <p>
 * This abstract class was created so that the Schematic externalizers can easily extend it, but so that we can encapsulate in
 * this abstract class the use of {@link org.infinispan.commons.marshall.Externalizer} or {@link org.infinispan.commons.marshall.AdvancedExternalizer}.
 * Once Infinispan in AS7 supports user-defined advanced externalizers, then we can simply change this class to
 * implement {@link org.infinispan.commons.marshall.AdvancedExternalizer} rather than {@link org.infinispan.commons.marshall.Externalizer}.
 * 
 * @param <T> the type of class
 */
public abstract class SchematicExternalizer<T> implements org.infinispan.commons.marshall.Externalizer<T>
/*implements AdvancedExternalizer<T>*/{

    private static final long serialVersionUID = 1L;

    public abstract Integer getId();

    public abstract Set<Class<? extends T>> getTypeClasses();

}
