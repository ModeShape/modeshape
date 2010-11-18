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
package org.modeshape.sequencer.teiid;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.SubgraphNode;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.basic.LocalNamespaceRegistry;
import org.xml.sax.SAXException;

/**
 * 
 */
public class VdbManifest {

    private String name = "";
    private String description = "";
    private boolean preview = false;
    private int version = 1;
    private final List<VdbModel> models = new ArrayList<VdbModel>();

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( String name ) {
        this.name = name != null ? name : "";
    }

    /**
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description Sets description to the specified value.
     */
    public void setDescription( String description ) {
        this.description = description != null ? description : "";
    }

    /**
     * @return preview
     */
    public boolean isPreview() {
        return preview;
    }

    /**
     * @param preview Sets preview to the specified value.
     */
    public void setPreview( boolean preview ) {
        this.preview = preview;
    }

    /**
     * @return version
     */
    public int getVersion() {
        return version;
    }

    /**
     * @param version Sets version to the specified value.
     */
    public void setVersion( int version ) {
        this.version = version;
    }

    /**
     * @return models
     */
    public List<VdbModel> getModels() {
        return models;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return name + " v" + version + " (\"" + description + "\")";
    }

    public Iterable<VdbModel> modelsInDependencyOrder() {
        if (!models.isEmpty()) {
            Collections.sort(models);
        }
        return models;
    }

    public static VdbManifest read( InputStream stream,
                                    ExecutionContext context ) throws IOException, SAXException {
        return new Reader(context).read(stream);
    }

    protected static class Reader {
        private final ExecutionContext context;
        private final ValueFactory<String> stringFactory;

        protected Reader( ExecutionContext context ) {
            // Use a local namespace registry (we don't need any of the namespaces in these XML files) ...
            NamespaceRegistry registry = context.getNamespaceRegistry();
            LocalNamespaceRegistry localRegistry = new LocalNamespaceRegistry(registry);
            this.context = context.with(localRegistry);
            this.stringFactory = context.getValueFactories().getStringFactory();
        }

        public VdbManifest read( InputStream stream ) throws IOException, SAXException {
            VdbManifest manifest = new VdbManifest();
            Graph graph = Graph.create(context);
            boolean error = false;
            try {
                // Load the input into the transient graph ...
                graph.importXmlFrom(stream).into("/");

                // Now read the graph ...
                Subgraph subgraph = graph.getSubgraphOfDepth(100).at("/vdb");

                // Read the 'vdb' root node ...
                SubgraphNode vdb = subgraph.getRoot();
                manifest.setName(firstValue(vdb, "name"));
                manifest.setDescription(firstValue(vdb, "description"));
                manifest.setVersion((int)firstValue(vdb, "version", 1L));

                // Read the children of 'vdb' ...
                for (Location childLocation : vdb) {
                    SubgraphNode model = subgraph.getNode(childLocation);
                    String name = nameOf(childLocation);
                    if ("property".equals(name)) {
                        String propertyName = firstValue(model, "name");
                        if ("preview".equals(propertyName)) {
                            manifest.setPreview(firstValue(model, "value", false));
                        }
                    } else if ("model".equals(name)) {
                        String modelName = firstValue(model, "name");
                        String modelType = firstValue(model, "type");
                        String modelPath = firstValue(model, "path");
                        modelPath = modelPath.replaceFirst("^/", "");
                        VdbModel vdbModel = new VdbModel(modelName, modelType, modelPath);
                        vdbModel.setBuiltIn(firstValue(model, "builtIn", false));
                        vdbModel.setVisible(firstValue(model, "visible", true));
                        vdbModel.setBuiltIn(property(model, "builtIn", vdbModel.isBuiltIn()));
                        vdbModel.setChecksum(property(model, "checksum", 0L));
                        vdbModel.getImports().addAll(properties(model, "imports"));

                        // Load the source information ...
                        SubgraphNode source = model.getNode("source");
                        if (source != null) {
                            vdbModel.setSourceName(firstValue(source, "name"));
                            vdbModel.setSourceTranslator(firstValue(source, "translator-name"));
                            vdbModel.setSourceJndiName(firstValue(source, "connection-jndi-name"));
                        }

                        // Load the problem markers ...
                        for (Location errorLocation : model) {
                            SubgraphNode marker = model.getNode(errorLocation.getPath().getLastSegment());
                            String path = firstValue(marker, "path");
                            String severity = firstValue(marker, "severity");
                            String message = firstValue(marker, JcrLexicon.XMLCHARACTERS, "");
                            vdbModel.addProblem(severity, path, message);
                        }
                        manifest.getModels().add(vdbModel);
                    }
                }

            } catch (IOException e) {
                error = true;
                throw e;
            } catch (RuntimeException e) {
                error = true;
                throw e;
            } finally {
                try {
                    if (stream != null) stream.close();
                } catch (IOException e) {
                    if (!error) throw e;
                }
            }

            return manifest;
        }

        protected String property( SubgraphNode node,
                                   String propertyName ) {
            for (Location childLocation : node) {
                SubgraphNode child = node.getNode(childLocation.getPath().getLastSegment());
                String name = nameOf(childLocation);
                if ("property".equals(name) && propertyName.equals(firstValue(child, "name"))) {
                    return firstValue(child, "value");
                }
            }
            return null;
        }

        protected List<String> properties( SubgraphNode node,
                                           String propertyName ) {
            List<String> values = new ArrayList<String>();
            for (Location childLocation : node) {
                SubgraphNode child = node.getNode(childLocation.getPath().getLastSegment());
                String name = nameOf(childLocation);
                if ("property".equals(name) && propertyName.equals(firstValue(child, "name"))) {
                    String value = firstValue(child, "value");
                    values.add(value);
                }
            }
            return values;
        }

        protected long property( SubgraphNode node,
                                 String propertyName,
                                 long defaultValue ) {
            for (Location childLocation : node) {
                SubgraphNode child = node.getNode(childLocation.getPath().getLastSegment());
                String name = nameOf(childLocation);
                if ("property".equals(name) && propertyName.equals(firstValue(child, "name"))) {
                    return firstValue(child, "value", defaultValue);
                }
            }
            return defaultValue;
        }

        protected boolean property( SubgraphNode node,
                                    String propertyName,
                                    boolean defaultValue ) {
            for (Location childLocation : node) {
                SubgraphNode child = node.getNode(childLocation.getPath().getLastSegment());
                String name = nameOf(childLocation);
                if ("property".equals(name) && propertyName.equals(firstValue(child, "name"))) {
                    return firstValue(child, "value", defaultValue);
                }
            }
            return defaultValue;
        }

        protected String firstValue( Node node,
                                     String propertyName ) {
            return firstValue(node, propertyName, null);
        }

        protected String firstValue( Node node,
                                     String propertyName,
                                     String defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return defaultValue;
            }
            return stringFactory.create(property.getFirstValue());
        }

        protected String firstValue( Node node,
                                     Name propertyName,
                                     String defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return defaultValue;
            }
            return stringFactory.create(property.getFirstValue());
        }

        protected boolean firstValue( Node node,
                                      String propertyName,
                                      boolean defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return defaultValue;
            }
            return context.getValueFactories().getBooleanFactory().create(property.getFirstValue());
        }

        protected long firstValue( Node node,
                                   String propertyName,
                                   long defaultValue ) {
            Property property = node.getProperty(propertyName);
            if (property == null || property.isEmpty()) {
                return defaultValue;
            }
            return context.getValueFactories().getLongFactory().create(property.getFirstValue());
        }

        protected String nameOf( Location location ) {
            Path path = location.getPath();
            if (path.isRoot()) return "";
            return path.getLastSegment().getName().getLocalName();
        }
    }

}
