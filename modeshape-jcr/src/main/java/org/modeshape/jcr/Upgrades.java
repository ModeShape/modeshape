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
package org.modeshape.jcr;

import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrRepository.RunningState;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class Upgrades {

    protected static final Logger LOGGER = Logger.getLogger(Upgrades.class);

    public static interface Context {
        /**
         * Get the repository's running state.
         * 
         * @return the repository state
         */
        RunningState getRepository();
    }

    /**
     * The standard upgrades for the built-in components and content of ModeShape.
     */
    public static final Upgrades STANDARD_UPGRADES;

    static {
        STANDARD_UPGRADES = new Upgrades(/*new ModeShape_3_6_0()*/);
    }

    private final List<UpgradeOperation> operations = new ArrayList<UpgradeOperation>();

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
        return operations.isEmpty() ? 0 : operations.get(operations.size() - 1).getId();
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

    // protected static class ModeShape_3_6_0 extends UpgradeOperation {
    // protected ModeShape_3_6_0() {
    // super(1);
    // }
    //
    // @Override
    // public void apply( Context resources ) {
    // // TODO
    // }
    // }
}
