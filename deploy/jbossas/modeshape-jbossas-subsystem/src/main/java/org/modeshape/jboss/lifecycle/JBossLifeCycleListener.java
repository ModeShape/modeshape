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
package org.modeshape.jboss.lifecycle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.msc.service.ServiceContainer.TerminateListener;

public class JBossLifeCycleListener implements TerminateListener, ContainerLifeCycleListener {

    private boolean shutdownInProgress = false;
    private List<ContainerLifeCycleListener.LifeCycleEventListener> listeners = Collections.synchronizedList(new ArrayList<ContainerLifeCycleListener.LifeCycleEventListener>());

    @Override
    public boolean isShutdownInProgress() {
        return shutdownInProgress;
    }

    @Override
    public void handleTermination( Info info ) {
        if (info.getShutdownInitiated() > 0) {
            this.shutdownInProgress = true;
        }
    }

    @Override
    public void addListener( LifeCycleEventListener listener ) {
        listeners.add(listener);
    }
}
