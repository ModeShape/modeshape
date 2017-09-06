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
package org.modeshape.jcr.api;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.InvalidSerializedDataException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;

import org.xml.sax.ContentHandler;

/**
 * A specialization of the standard JCR {@link javax.jcr.Session} interface that returns the ModeShape-specific extension
 * interfaces from {@link #getWorkspace()}, {@link #getRepository()}, and {@link #getValueFactory()}.
 */
@SuppressWarnings("deprectation")
public interface Session extends javax.jcr.Session {

    @Override
    public Workspace getWorkspace();

    @Override
    public Repository getRepository();

    @Override
    public ValueFactory getValueFactory() throws RepositoryException;

    public ValueFactory getValueFactory( String binaryStoreHint ) throws RepositoryException;

    /**
     * Sequence the specified property using the named sequencer, and place the generated output at the specified location using
     * this session. The output nodes will be transient within the current session, so this session will need to be saved to
     * persist the output.
     * <p>
     * It is suggested that this method be used on inputs that the sequencer is not configured to process automatically, otherwise
     * ModeShape will also sequence the same property. If your application is to only manually sequence, simply configure each
     * sequencer without a path expression or with a path expression that will never apply to the manually-sequenced nodes.
     * </p>
     * 
     * @param sequencerName the name of the configured sequencer that should be executed
     * @param inputProperty the property that was changed and that should be used as the input; never null
     * @param outputNode the node that represents the output for the derived information; never null, and will either be
     *        {@link Node#isNew() new} if the output is being placed outside of the selected node, or will not be new when the
     *        output is to be placed on the selected input node
     * @return true if the sequencer did generate output; false if the sequencer could not process the specified content or if
     *         {@code sequencerName} is null or does not match a configured sequencer
     * @throws RepositoryException if there was a problem with the sequencer
     */
    boolean sequence( String sequencerName,
                      Property inputProperty,
                      Node outputNode ) throws RepositoryException;

    /**
     * Evaluate a local name and replace any characters that are not allowed within the local names of nodes and properties. Such
     * characters include '/', ':', '[', ']', '*', and '|', since these are all important to the rules for qualified names and
     * paths. When such characters are to be used within a <i>local name</i>, the application must escape them using this method
     * before the local name is used.
     * 
     * @param localName the local name to be encoded; can be <code>null</code> or empty
     * @return the supplied local name if it contains no illegal characters, or the encoded form of the supplied local name with
     *         all illegal characters replaced, or <code>null</code> if the input was <code>null</code>
     * @see #move(String, String)
     * @see javax.jcr.Node#addNode(String)
     * @see javax.jcr.Node#addNode(String, String)
     */
    String encode( final String localName );

    /**
     * Evaluate a local name and replace any characters that were previously {@link #encode(String) encoded}.
     * 
     * @param localName the local name to be decoded; can be <code>null</code> or empty
     * @return the supplied local name if it contains no encoded characters, or the decoded form of the supplied local name with
     *         all encoded characters replaced, or <code>null</code> if the input was <code>null</code>
     * @see #encode(String)
     */
    String decode( final String localName );

    /**
     * Deserializes an XML document and adds the resulting item subgraph as a
     * child of the node at <code>parentAbsPath</code>.
     * <p>
     * If the incoming XML stream does not appear to be a JCR <i>system view</i>
     * XML document then it is interpreted as a <i>document view</i> XML
     * document.
     * <p>
     * The passed <code>InputStream</code> is closed before this method returns
     * either normally or because of an exception.
     * <p>
     * The tree of new items is built in the transient storage of the
     * <code>Session</code>. In order to persist the new content,
     * <code>save</code> must be called. The advantage of this
     * through-the-session method is that (depending on what constraint checks
     * the implementation leaves until <code>save</code>) structures that
     * violate node type constraints can be imported, fixed and then saved. The
     * disadvantage is that a large import will result in a large cache of
     * pending nodes in the session. See {@link Workspace#importXML} for a
     * version of this method that does not go through the
     * <code>Session</code>.
     * <p>
     * The flag <code>uuidBehavior</code> governs how the identifiers of
     * incoming nodes are handled. There are four options: <ul> <li> {@link
     * ImportUUIDBehavior#IMPORT_UUID_CREATE_NEW}: Incoming nodes are added in
     * the same way that new node is added with <code>Node.addNode</code>. That
     * is, they are either assigned newly created identifiers upon addition or
     * upon <code>save</code> (depending on the implementation, see <i>4.9.1.1
     * When Identifiers are Assigned</i> in the specification). In either case,
     * identifier collisions will not occur. </li> <li> {@link
     * ImportUUIDBehavior#IMPORT_UUID_COLLISION_REMOVE_EXISTING}: If an incoming
     * node has the same identifier as a node already existing in the workspace
     * then the already existing node (and its subgraph) is removed from
     * wherever it may be in the workspace before the incoming node is added.
     * Note that this can result in nodes "disappearing" from locations in the
     * workspace that are remote from the location to which the incoming
     * subgraph is being written. Both the removal and the new addition will be
     * dispatched on <code>save</code>. </li> <li> {@link
     * ImportUUIDBehavior#IMPORT_UUID_COLLISION_REPLACE_EXISTING}: If an
     * incoming node has the same identifier as a node already existing in the
     * workspace, then the already-existing node is replaced by the incoming
     * node in the same position as the existing node. Note that this may result
     * in the incoming subgraph being disaggregated and "spread around" to
     * different locations in the workspace. In the most extreme case this
     * behavior may result in no node at all being added as child of
     * <code>parentAbsPath</code>. This will occur if the topmost element of the
     * incoming XML has the same identifier as an existing node elsewhere in the
     * workspace. The change will be dispatched on <code>save</code>. </li> <li>
     * {@link ImportUUIDBehavior#IMPORT_UUID_COLLISION_THROW}: If an incoming
     * node has the same identifier as a node already existing in the workspace
     * then an <code>ItemExistsException</code> is thrown. </li> </ul> Unlike
     * {@link Workspace#importXML}, this method does not necessarily enforce all
     * node type constraints during deserialization. Those that would be
     * immediately enforced in a normal write method (<code>Node.addNode</code>,
     * <code>Node.setProperty</code> etc.) of this implementation cause an
     * immediate <code>ConstraintViolationException</code> during
     * deserialization. All other constraints are checked on <code>save</code>,
     * just as they are in normal write operations. However, which node type
     * constraints are enforced depends upon whether node type information in
     * the imported data is respected, and this is an implementation-specific
     * issue.
     * <p>
     * A <code>ConstraintViolationException</code> will also be thrown
     * immediately if <code>uuidBehavior</code> is set to
     * <code>IMPORT_UUID_COLLISION_REMOVE_EXISTING</code> and an incoming node
     * has the same identifier as the node at <code>parentAbsPath</code> or one
     * of its ancestors.
     * <p>
     * A <code>PathNotFoundException</code> is thrown either immediately, on
     * dispatch or on persist, if no node exists at <code>parentAbsPath</code>.
     * Implementations may differ on when this validation is performed
     * <p>
     * A <code>ConstraintViolationException</code> is thrown either immediately,
     * on dispatch or on persist, if the new subgraph cannot be added to the
     * node at <code>parentAbsPath</code> due to node-type or other
     * implementation-specific constraints. Implementations may differ on when
     * this validation is performed.
     * <p>
     * A <code>VersionException</code> is thrown either immediately, on dispatch
     * or on persist, if the node at <code>parentAbsPath</code> is read-only due
     * to a check-in. Implementations may differ on when this validation is
     * performed.
     * <p>
     * A <code>LockException</code> is thrown either immediately, on dispatch or
     * on persist, if a lock prevents the addition of the subgraph.
     * Implementations may differ on when this validation is performed.
     *
     * @param parentAbsPath the absolute path of the node below which the
     *                      deserialized subgraph is added.
     * @param in            The <code>Inputstream</code> from which the XML to be
     *                      deserialized is read.
     * @param uuidBehavior  a four-value flag that governs how incoming
     *                      identifiers are handled.\
     * @param binaryStoreHint the hint used for storing binaries.
     * @throws IOException            if an error during an I/O operation occurs.
     * @throws PathNotFoundException          if no node exists at
     *                                        <code>parentAbsPath</code> and this implementation performs this
     *                                        validation immediately.
     * @throws ItemExistsException            if deserialization would overwrite an
     *                                        existing item and this implementation performs this validation
     *                                        immediately.
     * @throws ConstraintViolationException   if a node type or other
     *                                        implementation-specific constraint is violated that would be checked on a
     *                                        session-write method or if <code>uuidBehavior</code> is set to
     *                                        <code>IMPORT_UUID_COLLISION_REMOVE_EXISTING</code> and an incoming node
     *                                        has the same UUID as the node at <code>parentAbsPath</code> or one of its
     *                                        ancestors.
     * @throws VersionException               if the node at <code>parentAbsPath</code> is
     *                                        read-only due to a checked-in node and this implementation performs this
     *                                        validation immediately.
     * @throws InvalidSerializedDataException if incoming stream is not a valid
     *                                        XML document.
     * @throws LockException                  if a lock prevents the addition of the subgraph and
     *                                        this implementation performs this validation immediately.
     * @throws RepositoryException            if another error occurs.
     */
    void importXML(String parentAbsPath, InputStream in, int uuidBehavior, String binaryStoreHint) throws IOException, PathNotFoundException, ItemExistsException, ConstraintViolationException, VersionException, InvalidSerializedDataException, LockException, RepositoryException;

    /**
     * Returns an <code>org.xml.sax.ContentHandler</code> which is used to push
     * SAX events to the repository. If the incoming XML (in the form of SAX
     * events) does not appear to be a JCR <i>system view</i> XML document then
     * it is interpreted as a JCR <i>document view</i> XML document.
     * <p>
     * The incoming XML is deserialized into a subgraph of items immediately
     * below the node at <code>parentAbsPath</code>.
     * <p>
     * This method simply returns the <code>ContentHandler</code> without
     * altering the state of the session; the actual deserialization to the
     * session transient space is done through the methods of the
     * <code>ContentHandler</code>. Invalid XML data will cause the
     * <code>ContentHandler</code> to throw a <code>SAXException</code>.
     * <p>
     * As SAX events are fed into the <code>ContentHandler</code>, the tree of
     * new items is built in the transient storage of the session. In order to
     * dispatch the new content, <code>save</code> must be called. See {@link
     * Workspace#getImportContentHandler} for a workspace-write version of this
     * method.
     * <p>
     * The flag <code>uuidBehavior</code> governs how the identifiers of
     * incoming nodes are handled: <ul> <li> {@link ImportUUIDBehavior#IMPORT_UUID_CREATE_NEW}:
     * Incoming identifiers nodes are added in the same way that new node is
     * added with <code>Node.addNode</code>. That is, they are either assigned
     * newly created identifiers upon addition or upon <code>save</code>
     * (depending on the implementation). In either case, identifier collisions
     * will not occur. </li> <li> {@link ImportUUIDBehavior#IMPORT_UUID_COLLISION_REMOVE_EXISTING}:
     * If an incoming node has the same identifier as a node already existing in
     * the workspace then the already existing node (and its subgraph) is
     * removed from wherever it may be in the workspace before the incoming node
     * is added. Note that this can result in nodes "disappearing" from
     * locations in the workspace that are remote from the location to which the
     * incoming subgraph is being written. Both the removal and the new addition
     * will be persisted on <code>save</code>. </li> <li> {@link
     * ImportUUIDBehavior#IMPORT_UUID_COLLISION_REPLACE_EXISTING}: If an
     * incoming node has the same identifier as a node already existing in the
     * workspace, then the already-existing node is replaced by the incoming
     * node in the same position as the existing node. Note that this may result
     * in the incoming subgraph being disaggregated and "spread around" to
     * different locations in the workspace. In the most extreme case this
     * behavior may result in no node at all being added as child of
     * <code>parentAbsPath</code>. This will occur if the topmost element of the
     * incoming XML has the same identifier as an existing node elsewhere in the
     * workspace. The change will be persisted on <code>save</code>. </li> <li>
     * {@link ImportUUIDBehavior#IMPORT_UUID_COLLISION_THROW}: If an incoming
     * node has the same identifier as a node already existing in the workspace
     * then a <code>SAXException</code> is thrown by the
     * <code>ContentHandler</code> during deserialization. </li> </ul> Unlike
     * <code>Workspace.getImportContentHandler</code>, this method does not
     * necessarily enforce all node type constraints during deserialization.
     * Those that would be immediately enforced in a session-write method
     * (<code>Node.addNode</code>, <code>Node.setProperty</code> etc.) of this
     * implementation cause the returned <code>ContentHandler</code> to throw an
     * immediate <code>SAXException</code> during deserialization. All other
     * constraints are checked on save, just as they are in normal write
     * operations. However, which node type constraints are enforced depends
     * upon whether node type information in the imported data is respected, and
     * this is an implementation-specific issue.
     * <p>
     * A <code>SAXException</code> will also be thrown by the returned
     * <code>ContentHandler</code> during deserialization if
     * <code>uuidBehavior</code> is set to <code>IMPORT_UUID_COLLISION_REMOVE_EXISTING</code>
     * and an incoming node has the same identifier as the node at
     * <code>parentAbsPath</code> or one of its ancestors.
     * <p>
     * A <code>PathNotFoundException</code> is thrown either immediately, on
     * dispatch or on persist, if no node exists at <code>parentAbsPath</code>.
     * Implementations may differ on when this validation is performed
     * <p>
     * A <code>ConstraintViolationException</code> is thrown either immediately,
     * on dispatch or on persist, if the new subgraph cannot be added to the
     * node at <code>parentAbsPath</code> due to node-type or other
     * implementation-specific constraints, and this can be determined before
     * the first SAX event is sent. Implementations may differ on when this
     * validation is performed.
     * <p>
     * A <code>VersionException</code> is thrown either immediately, on dispatch
     * or on persist, if the node at <code>parentAbsPath</code> is read-only due
     * to a check-in. Implementations may differ on when this validation is
     * performed.
     * <p>
     * A <code>LockException</code> is thrown either immediately, on dispatch or
     * on persist, if a lock prevents the addition of the subgraph.
     * Implementations may differ on when this validation is performed.
     *
     * @param parentAbsPath the absolute path of a node under which (as child)
     *                      the imported subgraph will be built.
     * @param uuidBehavior  a four-value flag that governs how incoming
     *                      identifiers are handled.
     * @param binaryStoreHint the binary store hint to use.
     * @return an org.xml.sax.ContentHandler whose methods may be called to feed
     *         SAX events into the deserializer.
     * @throws PathNotFoundException        if no node exists at
     *                                      <code>parentAbsPath</code> and this implementation performs this
     *                                      validation immediately.
     * @throws ConstraintViolationException if the new subgraph cannot be added
     *                                      to the node at <code>parentAbsPath</code> due to node-type or other
     *                                      implementation-specific constraints, and this implementation performs
     *                                      this validation immediately.
     * @throws VersionException             if the node at <code>parentAbsPath</code> is
     *                                      read-only due to a checked-in node and this implementation performs this
     *                                      validation immediately.
     * @throws LockException                if a lock prevents the addition of the subgraph and
     *                                      this implementation performs this validation immediately.
     * @throws RepositoryException          if another error occurs.
     */
    ContentHandler getImportContentHandler( String parentAbsPath, int uuidBehavior, String binaryStoreHint )
    throws PathNotFoundException, ConstraintViolationException, VersionException, LockException, RepositoryException;
}
