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

package org.modeshape.web.jcr.rest.output;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.modeshape.web.jcr.rest.model.JSONAble;
import org.modeshape.web.jcr.rest.model.Stringable;

/**
 * Extension of {@link JSONBodyWriter} which produces text output for {@link JSONAble} objects.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Provider
@Produces( {MediaType.TEXT_PLAIN} )
public class TextBodyWriter extends JSONBodyWriter {

    private static final int TEXT_INDENT_FACTOR = 2;

    @Override
    protected String getString( JSONAble jsonAble ) throws JSONException {
        if (jsonAble instanceof Stringable) {
            return ((Stringable)jsonAble).asString();
        }
        return jsonAble.toJSON().toString(TEXT_INDENT_FACTOR);
    }

    @Override
    protected String getString( JSONArray array ) throws JSONException {
        return array.toString(2);
    }
}
