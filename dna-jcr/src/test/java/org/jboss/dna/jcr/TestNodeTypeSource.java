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
package org.jboss.dna.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.jcr.PropertyType;
import org.jboss.dna.graph.ExecutionContext;

/**
 * Node type source with additional node types that can be used for testing. This class defines its own namespace for its types.
 */
public class TestNodeTypeSource extends AbstractJcrNodeTypeSource {

    /** The list of primary node types. */
    private final List<JcrNodeType> primaryNodeTypes;
    /** The list of mixin node types. */
    private final List<JcrNodeType> mixinNodeTypes;

    TestNodeTypeSource( ExecutionContext context,
                        JcrNodeTypeSource predecessor ) {
        super(predecessor);

        primaryNodeTypes = new ArrayList<JcrNodeType>();
        mixinNodeTypes = new ArrayList<JcrNodeType>();

        JcrNodeType base = findType(JcrNtLexicon.BASE);

        if (base == null) {
            String baseTypeName = JcrNtLexicon.BASE.getString(context.getNamespaceRegistry());
            String namespaceTypeName = DnaLexicon.NAMESPACE.getString(context.getNamespaceRegistry());
            throw new IllegalStateException(JcrI18n.supertypeNotFound.text(baseTypeName, namespaceTypeName));
        }

        // Stubbing in child node and property definitions for now
        JcrNodeType constrainedType = new JcrNodeType(context, NO_NODE_TYPE_MANAGER, TestLexicon.CONSTRAINED_TYPE,
                                                      Arrays.asList(new JcrNodeType[] {base}), NO_PRIMARY_ITEM_NAME,
                                                      NO_CHILD_NODES, Arrays.asList(new JcrPropertyDefinition[] {
                                                          new JcrPropertyDefinition(context, null,
                                                                                    TestLexicon.CONSTRAINED_BINARY,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.BINARY, new String[] {"[,5)",
                                                                                        "[10, 20)", "(30,40]", "[50,]"}, false),
                                                          new JcrPropertyDefinition(context, null, TestLexicon.CONSTRAINED_DATE,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.DATE, new String[] {
                                                                                        "[,+1945-08-01T01:30:00.000Z]",
                                                                                        "[+1975-08-01T01:30:00.000Z,)"}, false),
                                                          new JcrPropertyDefinition(context, null,
                                                                                    TestLexicon.CONSTRAINED_DOUBLE,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.DOUBLE,
                                                                                    new String[] {"[,5.0)", "[10.1, 20.2)",
                                                                                        "(30.3,40.4]", "[50.5,]"}, false),

                                                          new JcrPropertyDefinition(context, null, TestLexicon.CONSTRAINED_LONG,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.LONG, new String[] {"[,5)",
                                                                                        "[10, 20)", "(30,40]", "[50,]"}, false),

                                                          new JcrPropertyDefinition(context, null, TestLexicon.CONSTRAINED_NAME,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.NAME, new String[] {
                                                                                        "jcr:system", "dnatest:constrainedType"},
                                                                                    false),

                                                          new JcrPropertyDefinition(context, null, TestLexicon.CONSTRAINED_PATH,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.PATH, new String[] {
                                                                                    // "/" + JcrLexicon.Namespace.URI +
                                                                                    // ":system/*", "b", "/a/b/c"}, false),
                                                                                        "/jcr:system/*", "b", "/a/b/c"}, false),
                                                          new JcrPropertyDefinition(context, null,
                                                                                    TestLexicon.CONSTRAINED_REFERENCE,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.REFERENCE,
                                                                                    new String[] {"dna:root",}, false),

                                                          new JcrPropertyDefinition(context, null,
                                                                                    TestLexicon.CONSTRAINED_STRING,
                                                                                    OnParentVersionBehavior.IGNORE.getJcrValue(),
                                                                                    false, false, false, NO_DEFAULT_VALUES,
                                                                                    PropertyType.STRING, new String[] {"foo",
                                                                                        "bar*", ".*baz",}, false),

                                                      }), NOT_MIXIN, UNORDERABLE_CHILD_NODES);

        primaryNodeTypes.addAll(Arrays.asList(new JcrNodeType[] {constrainedType}));
        mixinNodeTypes.addAll(Arrays.asList(new JcrNodeType[] {}));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.AbstractJcrNodeTypeSource#getDeclaredMixinNodeTypes()
     */
    @Override
    public Collection<JcrNodeType> getDeclaredMixinNodeTypes() {
        return primaryNodeTypes;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.AbstractJcrNodeTypeSource#getDeclaredPrimaryNodeTypes()
     */
    @Override
    public Collection<JcrNodeType> getDeclaredPrimaryNodeTypes() {
        return mixinNodeTypes;
    }

}
