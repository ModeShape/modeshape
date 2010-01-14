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
package org.modeshape.graph.property.basic;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.Set;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.junit.Test;

/**
 * @author Randall Hauch
 */
public class GraphNamespaceRegistryTest extends AbstractNamespaceRegistryTest<GraphNamespaceRegistry> {

    protected ExecutionContext context;
    protected InMemoryRepositorySource source;
    protected Graph graph;
    private Path pathToParentOfNamespaceNodes;
    private Name uriPropertyName;
    private Property[] additionalNamespaceProperties;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractNamespaceRegistryTest#setUp()
     */
    @Override
    public void setUp() {
        super.setUp();

        // Set up the context and register any namespaces that we'll be using to manage the namespaces ...
        context = new ExecutionContext();
        NameFactory nameFactory = context.getValueFactories().getNameFactory();
        PropertyFactory propertyFactory = context.getPropertyFactory();
        context.getNamespaceRegistry().register("nsx", "http://www.example.com/namespaces");
        context.getNamespaceRegistry().register("other", "http://www.example.com/other");
        uriPropertyName = context.getValueFactories().getNameFactory().create("nsx:uri");
        additionalNamespaceProperties = new Property[] {
            propertyFactory.create(nameFactory.create("nsx:something"), "Some value"),
            propertyFactory.create(nameFactory.create("nsx:something2"), "Some value2"),
            propertyFactory.create(nameFactory.create("other:something2"), "Some other value2")};

        // Set up the repository that we'll be using ...
        source = new InMemoryRepositorySource();
        source.setName("namespace repository");
        graph = Graph.create(source, context);

        // Create the path to the where the namespaces will be managed ...
        pathToParentOfNamespaceNodes = graph.create("/a").and().create("/a/b").and().createAt("/a/b/c").getLocation().getPath();

        // Now set up the graph-based namespace registry ...
        namespaceRegistry = new GraphNamespaceRegistry(graph, pathToParentOfNamespaceNodes, uriPropertyName,
                                                       additionalNamespaceProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.property.basic.AbstractNamespaceRegistryTest#createNamespaceRegistry()
     */
    @Override
    protected GraphNamespaceRegistry createNamespaceRegistry() {
        return null;
    }

    @Test
    public void shouldInitializeFromPersistedContent() {
        // Add some namespaces ...
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix1), is(validNamespaceUri1));
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix2), is(validNamespaceUri2));

        // Now set up the graph-based namespace registry ...
        GraphNamespaceRegistry registry2 = new GraphNamespaceRegistry(graph, pathToParentOfNamespaceNodes, uriPropertyName,
                                                                      additionalNamespaceProperties);
        // All namespaces should match ...
        Set<NamespaceRegistry.Namespace> all = namespaceRegistry.getNamespaces();
        Set<NamespaceRegistry.Namespace> all2 = registry2.getNamespaces();
        assertThat(all, is(all2));
        assertThat(registry2.getNamespaceForPrefix(validPrefix1), is(validNamespaceUri1));
        assertThat(registry2.getNamespaceForPrefix(validPrefix2), is(validNamespaceUri2));

        registry2.refresh();
        all2 = registry2.getNamespaces();
        assertThat(all, is(all2));
        assertThat(registry2.getNamespaceForPrefix(validPrefix1), is(validNamespaceUri1));
        assertThat(registry2.getNamespaceForPrefix(validPrefix2), is(validNamespaceUri2));
    }

    @Test
    public void shouldRefreshFromPersistedContent() {
        // Add some namespaces ...
        namespaceRegistry.register(validPrefix1, validNamespaceUri1);
        namespaceRegistry.register(validPrefix2, validNamespaceUri2);
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix1), is(validNamespaceUri1));
        assertThat(namespaceRegistry.getNamespaceForPrefix(validPrefix2), is(validNamespaceUri2));

        // Get the namespaces, refresh, then get all the namespaces again
        Set<NamespaceRegistry.Namespace> allBefore = namespaceRegistry.getNamespaces();
        namespaceRegistry.refresh();
        Set<NamespaceRegistry.Namespace> allAfter = namespaceRegistry.getNamespaces();

        assertThat(allBefore, is(allAfter));
        for (NamespaceRegistry.Namespace namespace : allBefore) {
            assertThat(namespaceRegistry.getNamespaceForPrefix(namespace.getPrefix()), is(namespace.getNamespaceUri()));
            assertThat(namespaceRegistry.getPrefixForNamespaceUri(namespace.getNamespaceUri(), false), is(namespace.getPrefix()));
        }
    }

}
