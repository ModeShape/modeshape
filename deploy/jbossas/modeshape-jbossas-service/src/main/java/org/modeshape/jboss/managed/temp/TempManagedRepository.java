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
package org.modeshape.jboss.managed.temp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.Repository;
import org.joda.time.DateTime;
import org.modeshape.jboss.managed.ManagedLock;
import org.modeshape.jboss.managed.ManagedRepository;
import org.modeshape.jboss.managed.ManagedSession;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrRepository.DefaultOption;
import org.modeshape.jcr.JcrRepository.Option;

/**
 * 
 */
public class TempManagedRepository extends ManagedRepository {

    private final int id;
    private final Map<String, String> descriptors;
    private List<ManagedLock> locks;
    private final Map<String, String> options;
    private List<ManagedSession> sessions;

    public TempManagedRepository( int id ) {
        this.id = id;

        // options
        Map<String, String> tempOptions = new HashMap<String, String>();
        tempOptions.put(Option.PROJECT_NODE_TYPES.toString(), DefaultOption.PROJECT_NODE_TYPES);
        tempOptions.put(Option.JAAS_LOGIN_CONFIG_NAME.toString(), DefaultOption.JAAS_LOGIN_CONFIG_NAME);
        tempOptions.put(Option.READ_DEPTH.toString(), DefaultOption.READ_DEPTH);
        tempOptions.put(Option.ANONYMOUS_USER_ROLES.toString(), DefaultOption.ANONYMOUS_USER_ROLES);
        tempOptions.put(Option.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES.toString(),
                        DefaultOption.TABLES_INCLUDE_COLUMNS_FOR_INHERITED_PROPERTIES);
        tempOptions.put(Option.QUERY_EXECUTION_ENABLED.toString(), DefaultOption.QUERY_EXECUTION_ENABLED);
        tempOptions.put(Option.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY.toString(), DefaultOption.QUERY_INDEXES_UPDATED_SYNCHRONOUSLY);
        tempOptions.put(Option.QUERY_INDEX_DIRECTORY.toString(), DefaultOption.QUERY_INDEX_DIRECTORY);
        tempOptions.put(Option.PERFORM_REFERENTIAL_INTEGRITY_CHECKS.toString(),
                        DefaultOption.PERFORM_REFERENTIAL_INTEGRITY_CHECKS);
        this.options = Collections.<String, String>unmodifiableMap(tempOptions);

        // descriptors
        boolean value = (id % 2 == 0);
        Map<String, String> tempDescriptors = new HashMap<String, String>();
        tempDescriptors.put(Repository.LEVEL_1_SUPPORTED, Boolean.toString(value));
        tempDescriptors.put(Repository.LEVEL_2_SUPPORTED, Boolean.toString(value));
        tempDescriptors.put(Repository.OPTION_LOCKING_SUPPORTED, Boolean.toString(value));
        tempDescriptors.put(Repository.OPTION_OBSERVATION_SUPPORTED, Boolean.toString(value));
        tempDescriptors.put(Repository.OPTION_QUERY_SQL_SUPPORTED, "false"); // not JCR 1.0 SQL
        tempDescriptors.put(Repository.OPTION_TRANSACTIONS_SUPPORTED, Boolean.toString(!value));
        tempDescriptors.put(Repository.OPTION_VERSIONING_SUPPORTED, Boolean.toString(value));
        tempDescriptors.put(Repository.QUERY_XPATH_DOC_ORDER, Boolean.toString(!value));
        tempDescriptors.put(Repository.QUERY_XPATH_POS_INDEX, Boolean.toString(value));
        tempDescriptors.put(Repository.REP_NAME_DESC, Boolean.toString(value));
        tempDescriptors.put(Repository.REP_VENDOR_DESC, Boolean.toString(value));
        tempDescriptors.put(Repository.REP_VENDOR_URL_DESC, Boolean.toString(value));
        tempDescriptors.put(Repository.REP_VERSION_DESC, "true");
        tempDescriptors.put(Repository.SPEC_NAME_DESC, JcrI18n.SPEC_NAME_DESC.text());
        tempDescriptors.put(Repository.SPEC_VERSION_DESC, "1.0");
        this.descriptors = Collections.unmodifiableMap(tempDescriptors);

        // sessions
        this.sessions = new ArrayList<ManagedSession>();

        for (int i = 0; i < 5; ++i) {
            this.sessions.add(new ManagedSession("workspace-" + this.id, "userName-" + this.id + '.' + i, "session-" + this.id
                                                                                                          + '.' + i,
                                                 new DateTime()));
        }

        // locks
        this.locks = new ArrayList<ManagedLock>();
        boolean sessionBased = true;
        boolean deep = !sessionBased;

        for (int i = 0; i < 5; ++i) {
            this.locks.add(new ManagedLock("workspace-" + this.id, sessionBased, "session-" + this.id + '.' + i, new DateTime(),
                                           "lock-" + this.id + '.' + i, "owner-" + this.id + '.' + i, deep));
            sessionBased = !sessionBased;
        }
    }

    @Override
    public Map<String, String> getDescriptors() {
        return this.descriptors;
    }

    @Override
    public Map<String, String> getOptions() {
        return this.options;
    }

    @Override
    public int getQueryActivity() {
        // TODO getQueryActivity()
        return super.getQueryActivity();
    }

    @Override
    public int getSaveActivity() {
        // TODO getSaveActivity()
        return super.getSaveActivity();
    }

    @Override
    public Object getSessionActivity() {
        // TODO getSessionActivity()
        return super.getSessionActivity();
    }

    @Override
    public List<ManagedLock> listLocks() {
        return listLocks(ManagedLock.SORT_BY_OWNER);
    }

    @Override
    public List<ManagedLock> listLocks( Comparator<ManagedLock> lockSorter ) {
        Collections.sort(this.locks, lockSorter);
        return this.locks;
    }

    @Override
    public List<ManagedSession> listSessions() {
        return listSessions(ManagedSession.SORT_BY_USER);
    }

    @Override
    public List<ManagedSession> listSessions( Comparator<ManagedSession> sessionSorter ) {
        Collections.sort(this.sessions, sessionSorter);
        return this.sessions;
    }

    @Override
    public boolean removeLock( String lockId ) {
        return this.locks.remove(lockId);
    }

    @Override
    public boolean terminateSession( String sessionId ) {
        return this.sessions.remove(sessionId);
    }

}
