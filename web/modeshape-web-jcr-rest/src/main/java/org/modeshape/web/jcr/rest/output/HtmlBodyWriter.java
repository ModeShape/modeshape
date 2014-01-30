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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.modeshape.web.jcr.rest.model.JSONAble;
import org.modeshape.web.jcr.rest.model.RestException;

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
        // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
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
