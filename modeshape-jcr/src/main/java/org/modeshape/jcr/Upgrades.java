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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.query.Query;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.query.JcrQuery;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;
import org.modeshape.jcr.value.binary.FileSystemBinaryStore;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Upgrades {

    protected static final Logger LOGGER = Logger.getLogger(Upgrades.class);

    protected static final int EMPTY_UPGRADES_ID = 0;

    public static interface Context {
        /**
         * Get the repository's running state.
         * 
         * @return the repository state
         */
        RunningState getRepository();

        /**
         * Get a problems instance which can be used to record failures/warnings/information messages.
         * 
         * @return a {@link Problems} instance, never null.
         */
        Problems getProblems();
    }

    /**
     * The standard upgrades for the built-in components and content of ModeShape.
     */
    public static final Upgrades STANDARD_UPGRADES;

    static {
        STANDARD_UPGRADES = new Upgrades(ModeShape_3_6_0.INSTANCE, ModeShape_4_0_0_Alpha1.INSTANCE, ModeShape_4_0_0_Beta3.INSTANCE);
    }

    private final List<UpgradeOperation> operations = new ArrayList<>();

    protected Upgrades( UpgradeOperation... operations ) {
        int maxId = 0;
        for (UpgradeOperation op : operations) {
            assert op.getId() > maxId : "Upgrade operation '" + op + "' has an out-of-order ID ('" + op.getId()
                                        + "') that must be greater than all prior upgrades";
            maxId = op.getId();
            this.operations.add(op);
        }
    }

    /**
     * Apply any upgrades that are more recent than identified by the last upgraded identifier.
     * 
     * @param lastId the identifier of the last upgrade that was successfully run against the repository
     * @param resources the resources for the repository
     * @return the identifier of the last upgrade applied to the repository; may be the same or greater than {@code lastId}
     */
    public final int applyUpgradesSince( int lastId,
                                         Context resources ) {
        int lastUpgradeId = lastId;
        for (UpgradeOperation op : operations) {
            if (op.getId() <= lastId) continue;
            LOGGER.debug("Upgrade {0}: starting", op);
            op.apply(resources);
            LOGGER.debug("Upgrade {0}: complete", op);
            lastUpgradeId = op.getId();
        }
        return lastUpgradeId;
    }

    /**
     * Determine if an {@link #applyUpgradesSince(int, Context) upgrade} is required given the identifier of the last known
     * upgrade, which is compared to the identifiers of the registered upgrades.
     * 
     * @param lastId the identifier of the last known/successful upgrade previously applied to the repository
     * @return true if this contains at least one upgrade that should be applied to the repository, or false otherwise
     */
    public final boolean isUpgradeRequired( int lastId ) {
        return getLatestAvailableUpgradeId() > lastId;
    }

    /**
     * Get the identifier of the latest upgrade known to this object.
     * 
     * @return the latest identifier; 0 if there are no upgrades in this object, or positive number
     */
    public final int getLatestAvailableUpgradeId() {
        return operations.isEmpty() ? EMPTY_UPGRADES_ID : operations.get(operations.size() - 1).getId();
    }

    protected static abstract class UpgradeOperation {
        private final int id;

        protected UpgradeOperation( int id ) {
            assert id > 0 : "An upgrade operation's identifier must be positive";
            this.id = id;
        }

        /**
         * Get the identifier for this upgrade. This should be unique and sortable with respect to all other identifiers.
         * 
         * @return this upgrade's identifier; always positive
         */
        public int getId() {
            return id;
        }

        /**
         * Apply this upgrade operation to the supplied running repository.
         * 
         * @param resources the resources for the repository; never null
         */
        public abstract void apply( Context resources );

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(step " + id + ")";
        }
    }

    /**
     * Upgrade operation handling moving to ModeShape 3.6.0.Final. This consists of:making sure the internal node types are
     * <ul>
     * <li>updated to reflect the ACL and mode:lock changes</li>
     * <li>updating any potential existing lock to the updated mode:lock node type</li>
     * </ul>
     */
    protected static class ModeShape_3_6_0 extends UpgradeOperation {
        protected static final UpgradeOperation INSTANCE = new ModeShape_3_6_0();

        protected ModeShape_3_6_0() {
            super(1);
        }

        @Override
        public void apply( Context resources ) {
            LOGGER.info(JcrI18n.upgrade3_6_0Running);
            RunningState repository = resources.getRepository();
            if (updateInternalNodeTypes(repository)) {
                updateLocks(repository);
            }
        }

        @SuppressWarnings( "deprecation" )
        private void updateLocks( RunningState repository ) {
            try {
                SessionCache systemSession = repository.createSystemSession(repository.context(), false);
                SystemContent systemContent = new SystemContent(systemSession);
                CachedNode locksNode = systemContent.locksNode();
                if (locksNode == null) {
                    return;
                }
                ChildReferences childReferences = locksNode.getChildReferences(systemSession);
                if (childReferences.isEmpty()) {
                    return;
                }
                for (ChildReference ref : childReferences) {
                    MutableCachedNode lockNode = systemSession.mutable(ref.getKey());

                    // remove properties that belong to the old (invalid) node type
                    lockNode.removeProperty(systemSession, ModeShapeLexicon.LOCKED_KEY);
                    lockNode.removeProperty(systemSession, ModeShapeLexicon.SESSION_SCOPE);
                    lockNode.removeProperty(systemSession, ModeShapeLexicon.IS_DEEP);
                }
                systemContent.save();
            } catch (Exception e) {
                LOGGER.error(e, JcrI18n.upgrade3_6_0CannotUpdateLocks, e.getMessage());
            }
        }

        private boolean updateInternalNodeTypes( RunningState repository ) {
            CndImporter importer = new CndImporter(repository.context());
            SimpleProblems problems = new SimpleProblems();
            try {
                importer.importFrom(getClass().getClassLoader().getResourceAsStream(CndImporter.MODESHAPE_BUILT_INS),
                                    problems,
                                    null);
                if (!problems.isEmpty()) {
                    LOGGER.error(JcrI18n.upgrade3_6_0CannotUpdateNodeTypes, problems.toString());
                    return false;
                }
                List<NodeTypeDefinition> nodeTypeDefinitions = new ArrayList<NodeTypeDefinition>(
                                                                                                 importer.getNodeTypeDefinitions());
                for (Iterator<NodeTypeDefinition> nodeTypeDefinitionIterator = nodeTypeDefinitions.iterator(); nodeTypeDefinitionIterator.hasNext();) {
                    NodeTypeDefinition nodeTypeDefinition = nodeTypeDefinitionIterator.next();
                    String name = nodeTypeDefinition.getName();
                    // keep only the exact types that we know have changed to keep the overhead to a minimum
                    if (ModeShapeLexicon.ACCESS_CONTROLLABLE_STRING.equalsIgnoreCase(name)
                        || ModeShapeLexicon.ACCESS_LIST_NODE_TYPE_STRING.equalsIgnoreCase(name)
                        || ModeShapeLexicon.PERMISSION.getString().equalsIgnoreCase(name)
                        || ModeShapeLexicon.LOCK.getString().equalsIgnoreCase(name)) {
                        continue;
                    }
                    nodeTypeDefinitionIterator.remove();
                }
                repository.nodeTypeManager().registerNodeTypes(nodeTypeDefinitions, false, false, true);
            } catch (Exception e) {
                LOGGER.error(e, JcrI18n.upgrade3_6_0CannotUpdateNodeTypes, e.getMessage());
                return false;
            }
            return true;
        }
    }

    /**
     * Upgrade operation handling moving to ModeShape 4.0.0.Alpha1. This consists of making sure that the access control metadata
     * information is correctly stored in the repository metadata and that the {@link ModeShapeLexicon#ACL_COUNT} property
     * correctly reflects this.
     */
    protected static class ModeShape_4_0_0_Alpha1 extends UpgradeOperation {
        protected static final UpgradeOperation INSTANCE = new ModeShape_4_0_0_Alpha1();

        protected ModeShape_4_0_0_Alpha1() {
            super(401);
        }

        @Override
        public void apply( Context resources ) {
            LOGGER.info(JcrI18n.upgrade4_0_0_Alpha1_Running);
            RunningState runningState = resources.getRepository();
            RepositoryCache repositoryCache = runningState.repositoryCache();

            try {
                long nodesWithAccessControl = 0;
                for (String workspaceName : repositoryCache.getWorkspaceNames()) {
                    JcrSession session = runningState.loginInternalSession(workspaceName);
                    try {
                        JcrQueryManager queryManager = session.getWorkspace().getQueryManager();
                        Query query = queryManager.createQuery(
                                "select [jcr:name] from [" + ModeShapeLexicon.ACCESS_CONTROLLABLE_STRING + "]",
                                JcrQuery.JCR_SQL2);
                        nodesWithAccessControl = query.execute().getRows().getSize();
                    } finally {
                        session.logout();
                    }
                }
                if (nodesWithAccessControl == 0) {
                    repositoryCache.setAccessControlEnabled(false);
                }

                ExecutionContext context = runningState.context();
                SessionCache systemSession = runningState.createSystemSession(context, false);
                SystemContent systemContent = new SystemContent(systemSession);
                MutableCachedNode systemNode = systemContent.mutableSystemNode();
                systemNode.setProperty(systemSession, context.getPropertyFactory().create(ModeShapeLexicon.ACL_COUNT, nodesWithAccessControl));
                systemSession.save();
            } catch (RepositoryException e) {
               LOGGER.error(e, JcrI18n.upgrade4_0_0_Alpha1_Failed, e.getMessage());
            }
        }
    }

    /**
     * Upgrade operation handling moving to ModeShape 4.0.0.Beta3. This consists of upgrading the {@link org.modeshape.jcr.value.binary.FileSystemBinaryStore}
     * (if one is used) and changing the storage mechanism of the unused files (see MODE-2302).
     */
    protected static class ModeShape_4_0_0_Beta3 extends UpgradeOperation {
        protected static final UpgradeOperation INSTANCE = new ModeShape_4_0_0_Beta3();

        protected ModeShape_4_0_0_Beta3() {
            super(403);
        }

        @Override
        public void apply( Context resources ) {
            LOGGER.info(JcrI18n.upgrade4_0_0_Beta3_Running);
            RunningState runningState = resources.getRepository();
            BinaryStore binaryStore = runningState.binaryStore();
            try {
                if (binaryStore instanceof FileSystemBinaryStore) {
                    ((FileSystemBinaryStore)binaryStore).upgradeTrashContentFormat();
                }
            } catch (BinaryStoreException e) {
                LOGGER.error(e, JcrI18n.upgrade4_0_0_Beta3_Failed, e.getMessage());
            }
        }
    }
}
