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
import org.modeshape.web.jcr.rest.model.RestException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extension of {@link JSONBodyWriter} which produces HTML output for {@link JSONAble} objects.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@Provider
@Produces( { MediaType.TEXT_HTML } )
public class HtmlBodyWriter extends TextBodyWriter {

    @Override
    protected String getString( JSONAble jsonAble ) throws JSONException {
        String text = super.getString(jsonAble);
        return jsonAble instanceof RestException ? replaceSpaces(replaceLineTerminators(text)) : htmlString(text);
    }

    @Override
    protected String getString( JSONArray array ) throws JSONException {
        return htmlString(super.getString(array));
    }

    private String htmlString( String jsonString ) {
        jsonString = replaceLineTerminators(jsonString);
        jsonString = replaceSpaces(jsonString);
        jsonString = createURLs(jsonString);
        return "<code>" + jsonString + "</code>";
    }

    //    private String addColor( String jsonString ) {
    //        jsonString = jsonString.replaceAll("(\\{?\")([A-Z\\:a-z_]+)(\"\\:)", "$1<font color=\"#0000FF;\">$2</font>$3");
    //        jsonString = jsonString.replaceAll("(\\:\\s*)([A-Za-z_&&[^https?|file]]+)(\\,?)", "$1<font color=\"#008000;\">$2</font>$3");
    //        return jsonString;
    //    }

    private String createURLs( String jsonString ) {
        String urlPattern = "(?:(?:https?|file)://)[^\"\\r\\n]+";
        jsonString = jsonString.replaceAll(urlPattern, "<a href=$0>$0</a>");
        return jsonString;
    }

    private String replaceSpaces( String jsonString ) {
        Pattern pattern = Pattern.compile("(\\s*)");
        Matcher matcher = pattern.matcher(jsonString);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String spaces = matcher.group(1);
            StringBuilder nbspBuffer = new StringBuilder(spaces.length());
            for (int i = 0; i < spaces.length(); i++) {
                nbspBuffer.append("&nbsp;");
            }
            matcher.appendReplacement(buffer, nbspBuffer.toString());
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String replaceLineTerminators( String jsonString ) {
        jsonString = jsonString.replaceAll("\\\\r\\\\n\\\\t", "<br/>")
                               .replaceAll("\r\n\t", "<br/>")
                               .replaceAll("\r\n", "<br/>")
                               .replaceAll("\n", "<br/>")
                               .replaceAll("\r", "<br/>")
                               .replaceAll("\t", "<br/>")
                               .replaceAll("\\\\", "");
        return jsonString;
    }
}
