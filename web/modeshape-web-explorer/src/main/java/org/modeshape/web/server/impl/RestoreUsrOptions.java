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
package org.modeshape.web.server.impl;

import org.modeshape.jcr.api.RestoreOptions;
import org.modeshape.web.shared.RestoreParams;

/**
 *
 * @author kulikov
 */
public class RestoreUsrOptions extends RestoreOptions {

    private RestoreParams params;

    public RestoreUsrOptions(RestoreParams params) {
        this.params = params;
    }

    @Override
    public boolean includeBinaries() {
        return params.isIncludeBinaries();
    }

    @Override
    public boolean reindexContentOnFinish() {
        return params.isReindexOnFinish();
    }
}
