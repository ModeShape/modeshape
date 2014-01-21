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
package org.modeshape.jcr.cache.change;

/**
 * 
 */
public class PrintingChangeSetListener implements ChangeSetListener {

    public boolean print = false;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.cache.change.ChangeSetListener#notify(org.modeshape.jcr.cache.change.ChangeSet)
     */
    @Override
    public void notify( ChangeSet changeSet ) {
        if (print) System.out.println(changeSet);
    }

}
