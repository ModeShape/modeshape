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

import java.util.Random;
import org.joda.time.DateTime;
import org.modeshape.jboss.managed.ManagedSequencingService;

/**
 * 
 */
public class TempManagedSequencingService extends ManagedSequencingService {

    private final Random numberGenerator;

    public TempManagedSequencingService() {
        this.numberGenerator = new Random();
    }

    @Override
    public int getJobActivity() {
        // TODO implement getJobActivity()
        return super.getJobActivity();
    }

    @Override
    public long getNodesSequencedCount() {
        return this.numberGenerator.nextLong();
    }

    @Override
    public long getNodesSkippedCount() {
        return this.numberGenerator.nextLong();
    }

    @Override
    public int getQueuedJobCount() {
        // TODO implement getQueuedJobCount()
        return super.getQueuedJobCount();
    }

    @Override
    public long getStartTime() {
        return new DateTime().getMillis();
    }

    @Override
    public Object listQueuedJobs() {
        // TODO implement listQueuedJobs()
        return super.listQueuedJobs();
    }

}
