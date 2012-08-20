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

package org.modeshape.web.jcr.rest.output;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.modeshape.web.jcr.rest.model.JSONAble;

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
        return jsonAble.toJSON().toString(TEXT_INDENT_FACTOR);
    }

    @Override
    protected String getString( JSONArray array ) throws JSONException {
        return array.toString(2);
    }
}
