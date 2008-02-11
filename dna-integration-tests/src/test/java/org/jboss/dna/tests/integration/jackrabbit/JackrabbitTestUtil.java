package org.jboss.dna.tests.integration.jackrabbit;

import java.io.PrintStream;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class JackrabbitTestUtil {

    /**
     * Recursively outputs the contents of the given node.
     * @param node
     * @param stream
     * @param recursive
     * @throws RepositoryException
     */
    public static void dumpNode( Node node, PrintStream stream, boolean recursive ) throws RepositoryException {
        // First output the node path
        System.out.println(node.getPath());
        // Skip the virtual (and large!) jcr:system subtree
        if (node.getName().equals("jcr:system")) {
            return;
        }

        // Then output the properties
        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (property.getDefinition().isMultiple()) {
                // A multi-valued property, print all values
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    stream.println(property.getPath() + " = " + values[i].getString());
                }
            } else {
                // A single-valued property
                stream.println(property.getPath() + " = " + property.getString());
            }
        }

        if (recursive) {
            // Finally output all the child nodes recursively
            NodeIterator nodes = node.getNodes();
            while (nodes.hasNext()) {
                dumpNode(nodes.nextNode(), stream, true);
            }
        }
    }

}
