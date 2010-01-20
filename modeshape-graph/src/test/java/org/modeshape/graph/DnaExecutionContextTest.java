/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.DnaExecutionContext.LegacyNamespacePrefixes;
import org.modeshape.graph.DnaExecutionContext.LegacyNamespaceUris;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.NamespaceRegistry.Namespace;
import org.modeshape.graph.property.basic.BasicNamespace;

/**
 * 
 */
public class DnaExecutionContextTest {

    private static final Namespace DNA = new BasicNamespace(LegacyNamespacePrefixes.DNA, LegacyNamespaceUris.DNA);
    private static final Namespace DNAINT = new BasicNamespace(LegacyNamespacePrefixes.DNAINT, LegacyNamespaceUris.DNAINT);
    private static final Namespace MODE = new BasicNamespace(ModeShapeLexicon.Namespace.PREFIX, ModeShapeLexicon.Namespace.URI);
    private static final Namespace MODEINT = new BasicNamespace(ModeShapeIntLexicon.Namespace.PREFIX,
                                                                ModeShapeIntLexicon.Namespace.URI);

    private ExecutionContext context;
    private DnaExecutionContext dnaContext;
    private NamespaceRegistry registry;
    private NamespaceRegistry dnaRegistry;
    private ValueFactory<String> stringFactory;
    private NameFactory nameFactory;
    private PathFactory pathFactory;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        dnaContext = new DnaExecutionContext(context);
        registry = context.getNamespaceRegistry();
        dnaRegistry = dnaContext.getNamespaceRegistry();

        stringFactory = dnaContext.getValueFactories().getStringFactory();
        nameFactory = dnaContext.getValueFactories().getNameFactory();
        pathFactory = dnaContext.getValueFactories().getPathFactory();
    }

    protected Name name( String name ) {
        return nameFactory.create(name);
    }

    protected Path path( String path ) {
        return pathFactory.create(path);
    }

    @Test
    public void shouldNotFindDnaNamespacesInOriginalExecutionContext() {
        assertThat(registry.isRegisteredNamespaceUri(DNA.getNamespaceUri()), is(false));
        assertThat(registry.getPrefixForNamespaceUri(DNA.getPrefix(), false), is(nullValue()));
        assertThat(registry.isRegisteredNamespaceUri(DNAINT.getNamespaceUri()), is(false));
        assertThat(registry.getPrefixForNamespaceUri(DNAINT.getPrefix(), false), is(nullValue()));
    }

    @Test
    public void shouldFindDnaNamespacesInDnaExecutionContext() {
        assertThat(dnaRegistry.isRegisteredNamespaceUri(DNA.getNamespaceUri()), is(true));
        assertThat(dnaRegistry.isRegisteredNamespaceUri(DNAINT.getNamespaceUri()), is(true));
        assertThat(dnaRegistry.getNamespaces().contains(DNA), is(true));
        assertThat(dnaRegistry.getNamespaces().contains(DNAINT), is(true));
    }

    @Test
    public void shouldAliasDnaNamespaceToModeShapeNamespace() {
        assertThat(dnaRegistry.isRegisteredNamespaceUri(DNA.getNamespaceUri()), is(true));
        assertThat(dnaRegistry.getPrefixForNamespaceUri(DNA.getNamespaceUri(), false), is(MODE.getPrefix()));
        assertThat(dnaRegistry.getNamespaceForPrefix(DNA.getPrefix()), is(MODE.getNamespaceUri()));
        assertThat(dnaRegistry.isRegisteredNamespaceUri(DNAINT.getNamespaceUri()), is(true));
        assertThat(dnaRegistry.getPrefixForNamespaceUri(DNAINT.getNamespaceUri(), false),
                   is(ModeShapeIntLexicon.Namespace.PREFIX));
        assertThat(dnaRegistry.getNamespaceForPrefix(DNAINT.getPrefix()), is(MODEINT.getNamespaceUri()));
    }

    @Test
    public void shouldConstructNameWithModeShapeNamespaceFromStringContainingDnaPrefix() {
        Name name = name("dna:something");
        assertThat(name.getNamespaceUri(), is(MODE.getNamespaceUri()));
        assertThat(stringFactory.create(name), is("mode:something"));
    }

    @Test
    public void shouldConstructNameWithModeShapeNamespaceFromStringContainingModeShapePrefix() {
        Name name = name("mode:something");
        assertThat(name.getNamespaceUri(), is(MODE.getNamespaceUri()));
        assertThat(stringFactory.create(name), is("mode:something"));
    }

    @Test
    public void shouldConstructNameWithModeShapeNamespaceFromStringContainingDnaUri() {
        Name name = name("{" + DNA.getNamespaceUri() + "}something");
        assertThat(name.getNamespaceUri(), is(MODE.getNamespaceUri()));
        assertThat(stringFactory.create(name), is("mode:something"));
    }

    @Test
    public void shouldConstructNameWithModeShapeNamespaceFromStringContainingModeShapeUri() {
        Name name = name("{" + MODE.getNamespaceUri() + "}something");
        assertThat(name.getNamespaceUri(), is(MODE.getNamespaceUri()));
        assertThat(stringFactory.create(name), is("mode:something"));
    }

    @Test
    public void shouldConstructRelativePathWithModeShapeNamespaceFromStringContainingDnaPrefix() {
        Path path = path("dna:something/dna:else");
        assertThat(stringFactory.create(path), is("mode:something/mode:else"));
    }

    @Test
    public void shouldConstructRelativePathWithModeShapeNamespaceFromStringContainingModeShapePrefix() {
        Path path = path("mode:something/mode:else");
        assertThat(stringFactory.create(path), is("mode:something/mode:else"));
    }

    @Test
    public void shouldConstructAbsolutePathWithModeShapeNamespaceFromStringContainingDnaPrefix() {
        Path path = path("/dna:something/dna:else");
        assertThat(stringFactory.create(path), is("/mode:something/mode:else"));
    }

    @Test
    public void shouldConstructAbsolutePathWithModeShapeNamespaceFromStringContainingModeShapePrefix() {
        Path path = path("/mode:something/mode:else");
        assertThat(stringFactory.create(path), is("/mode:something/mode:else"));
    }
}
