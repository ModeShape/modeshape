/**
 * Graph content can often be represented in XML, so this part of the JBoss DNA Graph API defines the classes
 * that provide the binding between graph and XML content.  The {@link XmlHandler} is an implementation of
 * several SAX interfaces (including {@link org.xml.sax.ext.LexicalHandler}, {@link org.xml.sax.ext.DeclHandler}, 
 * and {@link org.xml.sax.ext.EntityResolver2}) that responds to XML content events by creating the corresponding 
 * content in a supplied graph. 
 */

package org.jboss.dna.graph.xml;

