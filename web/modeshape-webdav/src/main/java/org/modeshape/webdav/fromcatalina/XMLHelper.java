package org.modeshape.webdav.fromcatalina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLHelper {

    public static Node findSubElement( Node parent,
                                       String localName ) {
        if (parent == null) {
            return null;
        }
        Node child = parent.getFirstChild();
        while (child != null) {
            if ((child.getNodeType() == Node.ELEMENT_NODE) && (child.getLocalName().equals(localName))) {
                return child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

    public static Vector<String> getPropertiesFromXML( Node propNode ) {
        Vector<String> properties;
        properties = new Vector<String>();
        NodeList childList = propNode.getChildNodes();

        for (int i = 0; i < childList.getLength(); i++) {
            Node currentNode = childList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = currentNode.getLocalName();
                String namespace = currentNode.getNamespaceURI();
                // href is a live property which is handled differently
                properties.addElement(namespace + ":" + nodeName);
            }
        }
        return properties;
    }

    public static Map<String, Object> getPropertiesWithValuesFromXML( Node propNode ) {
        Map<String, Object> propertiesWithValues = new HashMap<String, Object>();

        NodeList childList = propNode.getChildNodes();
        for (int i = 0; i < childList.getLength(); i++) {
            Node currentNode = childList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = currentNode.getLocalName();
                String namespace = currentNode.getNamespaceURI();
                // href is a live property which is handled differently
                String fqn = namespace + ":" + nodeName;
                propertiesWithValues.put(fqn, nodeValue(currentNode));

            }
        }
        return propertiesWithValues;
    }

    private static Object nodeValue(Node node) {
        NodeList childList = node.getChildNodes();
        if (childList.getLength() == 0) {
            return "";
        } else if (childList.getLength() == 1 && childList.item(0).getNodeType() == Node.TEXT_NODE) {
            return node.getTextContent().trim();
        } else {
            List<Object> value = new ArrayList<Object>();
            for (int i = 0; i < childList.getLength(); i++) {
                value.add(nodeValue(childList.item(i)));
            }
            return value;
        }
    }
}
