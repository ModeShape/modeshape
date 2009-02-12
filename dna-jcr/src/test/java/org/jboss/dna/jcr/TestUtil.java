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

import javax.security.auth.login.LoginContext;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.JcrNtLexicon;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.mockito.Mockito;

/**
 * @author jverhaeg
 */
@NotThreadSafe
public class TestUtil {

    public static RepositoryConnectionFactory createJackRabbitConnectionFactory( final RepositorySource source,
                                                                                 final ExecutionContext context ) {
        Graph repository = Graph.create(source, context);
        Graph.Batch batch = repository.batch();
        batch.set(JcrLexicon.PRIMARY_TYPE).on("/").to(JcrNtLexicon.UNSTRUCTURED).and();
        batch.create("/dna:system").with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.UNSTRUCTURED).and();
        batch.create("/dna:system/jcr:versionStorage").with(JcrLexicon.PRIMARY_TYPE, "rep:versionStorage").and();
        batch.create("/dna:system/jcr:nodeTypes").with(JcrLexicon.PRIMARY_TYPE, "rep:nodeTypes").and();
        batch.execute();

        createNodeType(repository, context, "rep:nodeTypes", false, false);
        createChildDefinition(repository, context, "rep:nodeTypes", false, "nt:nodeType", false, "ABORT", true, false);
        createNodeType(repository, context, "mix:versionable", false, true);
        createPropertyDefinition(repository,
                                 context,
                                 "mix:versionable",
                                 1,
                                 false,
                                 false,
                                 true,
                                 "jcr:mergeFailed",
                                 "ABORT",
                                 true,
                                 "REFERENCE");
        createPropertyDefinition(repository,
                                 context,
                                 "mix:versionable",
                                 2,
                                 false,
                                 true,
                                 true,
                                 "jcr:predecessors",
                                 "COPY",
                                 true,
                                 "REFERENCE");
        createPropertyDefinition(repository,
                                 context,
                                 "mix:versionable",
                                 3,
                                 true,
                                 true,
                                 false,
                                 "jcr:isCheckedOut",
                                 "IGNORE",
                                 true,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "mix:versionable",
                                 4,
                                 false,
                                 true,
                                 false,
                                 "jcr:baseVersion",
                                 "IGNORE",
                                 true,
                                 "REFERENCE");
        createPropertyDefinition(repository,
                                 context,
                                 "mix:versionable",
                                 5,
                                 false,
                                 true,
                                 false,
                                 "jcr:versionHistory",
                                 "COPY",
                                 true,
                                 "REFERENCE");
        createNodeType(repository, context, "nt:file", false, false);
        createProperty(repository, context, "nt:file", "jcr:primaryItemName", "jcr:content");
        createChildDefinition(repository, context, "nt:file", false, true, "jcr:content", "COPY", false, false);
        createNodeType(repository, context, "nt:hierarchyNode", false, false);
        createPropertyDefinition(repository,
                                 context,
                                 "nt:hierarchyNode",
                                 true,
                                 false,
                                 false,
                                 "jcr:created",
                                 "INITIALIZE",
                                 true,
                                 "DATE");
        createNodeType(repository, context, "nt:versionedChild", false, false);
        createPropertyDefinition(repository,
                                 context,
                                 "nt:versionedChild",
                                 true,
                                 true,
                                 false,
                                 "jcr:childVersionHistory",
                                 "ABORT",
                                 true,
                                 "REFERENCE");
        createNodeType(repository, context, "nt:version", false, false);
        createPropertyDefinition(repository,
                                 context,
                                 "nt:version",
                                 1,
                                 false,
                                 false,
                                 true,
                                 "jcr:successors",
                                 "ABORT",
                                 true,
                                 "REFERENCE");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:version",
                                 2,
                                 false,
                                 false,
                                 true,
                                 "jcr:predecessors",
                                 "ABORT",
                                 true,
                                 "REFERENCE");
        createPropertyDefinition(repository, context, "nt:version", 3, true, true, false, "jcr:created", "ABORT", true, "DATE");
        createChildDefinition(repository, context, "nt:version", false, false, "jcr:frozenNode", "ABORT", true, false);
        /*
            <nt:versionLabels jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="false" jcr:nodeTypeName="nt:versionLabels">
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="false" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:requiredType="REFERENCE"/>
            <nt:folder jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="false" jcr:nodeTypeName="nt:folder">
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:onParentVersion="VERSION" jcr:protected="false" jcr:sameNameSiblings="false"/>
        */
        createNodeType(repository, context, "nt:nodeType", false, false);
        createPropertyDefinition(repository,
                                 context,
                                 "nt:nodeType",
                                 1,
                                 false,
                                 false,
                                 false,
                                 "jcr:primaryItemName",
                                 "COPY",
                                 false,
                                 "NAME");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:nodeType",
                                 2,
                                 false,
                                 true,
                                 false,
                                 "jcr:hasOrderableChildNodes",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:nodeType",
                                 3,
                                 false,
                                 true,
                                 false,
                                 "jcr:isMixin",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:nodeType",
                                 4,
                                 false,
                                 false,
                                 true,
                                 "jcr:supertypes",
                                 "COPY",
                                 false,
                                 "NAME");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:nodeType",
                                 5,
                                 false,
                                 true,
                                 false,
                                 "jcr:nodeTypeName",
                                 "COPY",
                                 false,
                                 "NAME");
        createChildDefinition(repository,
                              context,
                              "nt:nodeType",
                              1,
                              false,
                              "nt:childNodeDefinition",
                              false,
                              "jcr:childNodeDefinition",
                              "VERSION",
                              false,
                              true);
        createChildDefinition(repository,
                              context,
                              "nt:nodeType",
                              2,
                              false,
                              "nt:propertyDefinition",
                              false,
                              "jcr:propertyDefinition",
                              "VERSION",
                              false,
                              true);
        createNodeType(repository, context, "nt:propertyDefinition", false, false);
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 1,
                                 false,
                                 true,
                                 false,
                                 "jcr:multiple",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 2,
                                 false,
                                 false,
                                 true,
                                 "jcr:defaultValues",
                                 "COPY",
                                 false,
                                 "UNDEFINED");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 3,
                                 false,
                                 false,
                                 true,
                                 "jcr:valueConstraints",
                                 "COPY",
                                 false,
                                 "STRING");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 4,
                                 false,
                                 true,
                                 false,
                                 "jcr:requiredType",
                                 "COPY",
                                 false,
                                 "STRING");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 5,
                                 false,
                                 true,
                                 false,
                                 "jcr:protected",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 6,
                                 false,
                                 true,
                                 false,
                                 "jcr:onParentVersion",
                                 "COPY",
                                 false,
                                 "STRING");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 7,
                                 false,
                                 true,
                                 false,
                                 "jcr:mandatory",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 8,
                                 false,
                                 true,
                                 false,
                                 "jcr:autoCreated",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:propertyDefinition",
                                 9,
                                 false,
                                 false,
                                 false,
                                 "jcr:name",
                                 "COPY",
                                 false,
                                 "NAME");
        /*
            <rep:versionStorage jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="false" jcr:nodeTypeName="rep:versionStorage">
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:defaultPrimaryType="rep:versionStorage" jcr:mandatory="false" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="true"/>
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:defaultPrimaryType="nt:versionHistory" jcr:mandatory="false" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="true"/>
        */
        createNodeType(repository, context, "nt:base", false, false);
        createPropertyDefinition(repository, context, "nt:base", false, false, true, "jcr:mixinTypes", "COMPUTE", true, "NAME");
        createPropertyDefinition(repository, context, "nt:base", true, true, false, "jcr:primaryType", "COMPUTE", true, "NAME");
        /*
            <nt:resource jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="false" jcr:nodeTypeName="nt:resource" jcr:primaryItemName="jcr:data">
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="true" jcr:multiple="false" jcr:name="jcr:lastModified" jcr:onParentVersion="IGNORE" jcr:protected="false" jcr:requiredType="DATE"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="true" jcr:multiple="false" jcr:name="jcr:data" jcr:onParentVersion="COPY" jcr:protected="false" jcr:requiredType="BINARY"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="true" jcr:multiple="false" jcr:name="jcr:mimeType" jcr:onParentVersion="COPY" jcr:protected="false" jcr:requiredType="STRING"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="false" jcr:name="jcr:encoding" jcr:onParentVersion="COPY" jcr:protected="false" jcr:requiredType="STRING"/>
        */
        createNodeType(repository, context, "nt:childNodeDefinition", false, false);
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 1,
                                 false,
                                 true,
                                 false,
                                 "jcr:sameNameSiblings",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 2,
                                 false,
                                 false,
                                 false,
                                 "jcr:defaultPrimaryType",
                                 "COPY",
                                 false,
                                 "NAME");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 3,
                                 false,
                                 true,
                                 true,
                                 "jcr:requiredPrimaryTypes",
                                 "COPY",
                                 false,
                                 "NAME");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 4,
                                 false,
                                 true,
                                 false,
                                 "jcr:protected",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 5,
                                 false,
                                 true,
                                 false,
                                 "jcr:onParentVersion",
                                 "COPY",
                                 false,
                                 "STRING");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 6,
                                 false,
                                 true,
                                 false,
                                 "jcr:mandatory",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 7,
                                 false,
                                 true,
                                 false,
                                 "jcr:autoCreated",
                                 "COPY",
                                 false,
                                 "BOOLEAN");
        createPropertyDefinition(repository,
                                 context,
                                 "nt:childNodeDefinition",
                                 8,
                                 false,
                                 false,
                                 false,
                                 "jcr:name",
                                 "COPY",
                                 false,
                                 "NAME");
        createNodeType(repository, context, "mix:referenceable", false, true);
        createPropertyDefinition(repository,
                                 context,
                                 "mix:referenceable",
                                 true,
                                 true,
                                 false,
                                 "jcr:uuid",
                                 "INITIALIZE",
                                 true,
                                 "STRING");
        createNodeType(repository, context, "nt:unstructured", true, false);
        createPropertyDefinition(repository, context, "nt:unstructured", false, false, false, "COPY", false, "UNDEFINED");
        createPropertyDefinition(repository, context, "nt:unstructured", false, false, true, "COPY", false, "UNDEFINED");
        createChildDefinition(repository, context, "nt:unstructured", false, "nt:unstructured", false, "VERSION", false, true);
        /*
            <nt:versionHistory jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="false" jcr:nodeTypeName="nt:versionHistory">
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="true" jcr:mandatory="true" jcr:multiple="false" jcr:name="jcr:versionableUuid" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:requiredType="STRING"/>
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:defaultPrimaryType="nt:version" jcr:mandatory="false" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="false"/>
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="true" jcr:defaultPrimaryType="nt:versionLabels" jcr:mandatory="true" jcr:name="jcr:versionLabels" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="false"/>
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="true" jcr:defaultPrimaryType="nt:version" jcr:mandatory="true" jcr:name="jcr:rootVersion" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="false"/>
            <mix:lockable jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="true" jcr:nodeTypeName="mix:lockable">
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="false" jcr:name="jcr:lockIsDeep" jcr:onParentVersion="IGNORE" jcr:protected="true" jcr:requiredType="BOOLEAN"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="false" jcr:name="jcr:lockOwner" jcr:onParentVersion="IGNORE" jcr:protected="true" jcr:requiredType="STRING"/>
            <nt:frozenNode jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="true" jcr:isMixin="false" jcr:nodeTypeName="nt:frozenNode">
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="true" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:requiredType="UNDEFINED"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="false" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:requiredType="UNDEFINED"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="true" jcr:mandatory="true" jcr:multiple="false" jcr:name="jcr:frozenUuid" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:requiredType="STRING"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="true" jcr:name="jcr:frozenMixinTypes" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:requiredType="NAME"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="true" jcr:mandatory="true" jcr:multiple="false" jcr:name="jcr:frozenPrimaryType" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:requiredType="NAME"/>
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="true"/>
            <rep:system jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="true" jcr:isMixin="false" jcr:nodeTypeName="rep:system">
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:defaultPrimaryType="nt:unstructured" jcr:mandatory="false" jcr:onParentVersion="IGNORE" jcr:protected="false" jcr:sameNameSiblings="true"/>
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:defaultPrimaryType="rep:nodeTypes" jcr:mandatory="true" jcr:name="jcr:nodeTypes" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="false"/>
                <jcr:childNodeDefinition jcr:primaryType="nt:childNodeDefinition" jcr:autoCreated="false" jcr:defaultPrimaryType="rep:versionStorage" jcr:mandatory="true" jcr:name="jcr:versionStorage" jcr:onParentVersion="ABORT" jcr:protected="true" jcr:sameNameSiblings="false"/>
        */
        createNodeType(repository, context, "rep:root", true, false);
        createChildDefinition(repository, context, "rep:root", false, true, "jcr:system", false, false);
        /*
            <nt:query jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="false" jcr:nodeTypeName="nt:query">
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="false" jcr:name="jcr:language" jcr:onParentVersion="COPY" jcr:protected="false" jcr:requiredType="STRING"/>
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="false" jcr:multiple="false" jcr:name="jcr:statement" jcr:onParentVersion="COPY" jcr:protected="false" jcr:requiredType="STRING"/>
            </nt:query>
            <nt:linkedFile jcr:primaryType="nt:nodeType" jcr:hasOrderableChildNodes="false" jcr:isMixin="false" jcr:nodeTypeName="nt:linkedFile" jcr:primaryItemName="jcr:content">
                <jcr:propertyDefinition jcr:primaryType="nt:propertyDefinition" jcr:autoCreated="false" jcr:mandatory="true" jcr:multiple="false" jcr:name="jcr:content" jcr:onParentVersion="COPY" jcr:protected="false" jcr:requiredType="REFERENCE"/>
            </nt:linkedFile>
         */

        return new RepositoryConnectionFactory() {

            public RepositoryConnection createConnection( String sourceName ) {
                return source.getConnection();
            }
        };
    }

    public static ExecutionContext getExecutionContext() {
        final ExecutionContext context = new ExecutionContext().create(Mockito.mock(LoginContext.class));
        NamespaceRegistry registry = context.getNamespaceRegistry();
        registry.register("dna", "http://www.jboss.org/dna/1.0");
        registry.register("fn", "http://www.w3.org/2005/xpath-functions");
        registry.register("fn_old", "http://www.w3.org/2004/10/xpath-functions");
        registry.register("jcr", "http://www.jcp.org/jcr/1.0");
        registry.register("mix", "http://www.jcp.org/jcr/mix/1.0");
        registry.register("nt", "http://www.jcp.org/jcr/nt/1.0");
        registry.register("rep", "internal");
        registry.register("sv", "http://www.jcp.org/jcr/sv/1.0");
        registry.register("xs", "http://www.w3.org/2001/XMLSchema");
        return context;
    }

    private static void createChildDefinition( Graph repository,
                                               ExecutionContext context,
                                               String node,
                                               Boolean autoCreated,
                                               Boolean mandatory,
                                               String onParentVersion,
                                               Boolean isProtected,
                                               Boolean sameNameSiblings ) {
        createChildDefinition(repository,
                              context,
                              node,
                              0,
                              autoCreated,
                              null,
                              mandatory,
                              null,
                              onParentVersion,
                              isProtected,
                              sameNameSiblings);
    }

    private static void createChildDefinition( Graph repository,
                                               ExecutionContext context,
                                               String node,
                                               Boolean autoCreated,
                                               Boolean mandatory,
                                               String name,
                                               String onParentVersion,
                                               Boolean isProtected,
                                               Boolean sameNameSiblings ) {
        createChildDefinition(repository,
                              context,
                              node,
                              0,
                              autoCreated,
                              null,
                              mandatory,
                              name,
                              onParentVersion,
                              isProtected,
                              sameNameSiblings);
    }

    private static void createChildDefinition( Graph repository,
                                               ExecutionContext context,
                                               String node,
                                               Boolean autoCreated,
                                               String defaultPrimaryType,
                                               Boolean mandatory,
                                               String onParentVersion,
                                               Boolean isProtected,
                                               Boolean sameNameSiblings ) {
        createChildDefinition(repository,
                              context,
                              node,
                              0,
                              autoCreated,
                              defaultPrimaryType,
                              mandatory,
                              null,
                              onParentVersion,
                              isProtected,
                              sameNameSiblings);
    }

    private static void createChildDefinition( Graph repository,
                                               ExecutionContext context,
                                               String node,
                                               int index,
                                               Boolean autoCreated,
                                               String defaultPrimaryType,
                                               Boolean mandatory,
                                               String name,
                                               String onParentVersion,
                                               Boolean isProtected,
                                               Boolean sameNameSiblings ) {
        String defNode = "/dna:system/jcr:nodeTypes/" + node + "/jcr:childNodeDefinition";
        if (index > 0) {
            defNode += '[' + index + ']';
        }
        createProperty(repository, context, defNode, "jcr:primaryType", "nt:childNodeDefinition");
        createProperty(repository, context, defNode, "jcr:autoCreated", autoCreated.toString());
        if (defaultPrimaryType != null) {
            createProperty(repository, context, defNode, "jcr:defaultPrimaryType", defaultPrimaryType);
        }
        createProperty(repository, context, defNode, "jcr:mandatory", mandatory.toString());
        if (name != null) {
            createProperty(repository, context, defNode, "jcr:name", name);
        }
        createProperty(repository, context, defNode, "jcr:onParentVersion", onParentVersion);
        createProperty(repository, context, defNode, "jcr:protected", isProtected.toString());
        createProperty(repository, context, defNode, "jcr:sameNameSiblings", sameNameSiblings.toString());
    }

    //
    // private static void createChildDefinitionProperty( SimpleRepository repository,
    // ExecutionContext context,
    // String node,
    // String property,
    // String value ) {
    // createProperty(repository, context, node + "/jcr:childNodeDefinition", property, value);
    // }

    private static void createNodeType( Graph repository,
                                        ExecutionContext context,
                                        String node,
                                        Boolean hasOrderableChildNodes,
                                        Boolean isMixin ) {
        node = "/dna:system/jcr:nodeTypes/" + node;
        repository.create(node);
        createProperty(repository, context, node, "jcr:primaryType", "nt:nodeType");
        createProperty(repository, context, node, "jcr:hasOrderableChildNodes", hasOrderableChildNodes.toString());
        createProperty(repository, context, node, "jcr:isMixin", isMixin.toString());
        createProperty(repository, context, node, "jcr:nodeTypeName", node);
    }

    private static void createProperty( Graph repository,
                                        ExecutionContext context,
                                        String node,
                                        String property,
                                        String value ) {
        repository.set(property).on(node).to(value);
    }

    private static void createPropertyDefinition( Graph repository,
                                                  ExecutionContext context,
                                                  String node,
                                                  Boolean autoCreated,
                                                  Boolean mandatory,
                                                  Boolean multiple,
                                                  String onParentVersion,
                                                  Boolean isProtected,
                                                  String requiredType ) {
        createPropertyDefinition(repository,
                                 context,
                                 node,
                                 0,
                                 autoCreated,
                                 mandatory,
                                 multiple,
                                 null,
                                 onParentVersion,
                                 isProtected,
                                 requiredType);
    }

    private static void createPropertyDefinition( Graph repository,
                                                  ExecutionContext context,
                                                  String node,
                                                  Boolean autoCreated,
                                                  Boolean mandatory,
                                                  Boolean multiple,
                                                  String name,
                                                  String onParentVersion,
                                                  Boolean isProtected,
                                                  String requiredType ) {
        createPropertyDefinition(repository,
                                 context,
                                 node,
                                 0,
                                 autoCreated,
                                 mandatory,
                                 multiple,
                                 name,
                                 onParentVersion,
                                 isProtected,
                                 requiredType);
    }

    private static void createPropertyDefinition( Graph repository,
                                                  ExecutionContext context,
                                                  String node,
                                                  int index,
                                                  Boolean autoCreated,
                                                  Boolean mandatory,
                                                  Boolean multiple,
                                                  String name,
                                                  String onParentVersion,
                                                  Boolean isProtected,
                                                  String requiredType ) {
        String defNode = "/dna:system/jcr:nodeTypes/" + node + "/nt:propertyDefinition";
        if (index > 0) {
            defNode += '[' + index + ']';
        }
        repository.create(defNode);
        createProperty(repository, context, defNode, "jcr:primaryType", "nt:propertyDefinition");
        createProperty(repository, context, defNode, "jcr:autoCreated", autoCreated.toString());
        createProperty(repository, context, defNode, "jcr:mandatory", mandatory.toString());
        createProperty(repository, context, defNode, "jcr:multiple", multiple.toString());
        if (name != null) {
            createProperty(repository, context, defNode, "jcr:name", name);
        }
        createProperty(repository, context, defNode, "jcr:onParentVersion", onParentVersion);
        createProperty(repository, context, defNode, "jcr:protected", isProtected.toString());
        createProperty(repository, context, defNode, "jcr:requiredType", requiredType);
    }
}
