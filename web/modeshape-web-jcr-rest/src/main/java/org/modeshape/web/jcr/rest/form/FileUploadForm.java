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

package org.modeshape.web.jcr.rest.form;

import java.io.InputStream;
import javax.ws.rs.FormParam;

/**
 * POJO which leverages RestEasy's support for HTML forms, containing one element {@link FileUploadForm#fileData}, which is
 * populated by RestEasy when an HTML form with the html element with the name {@code file} is submitted.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class FileUploadForm {

    public static final String PARAM_NAME = "file";

    private InputStream fileData;

    /**
     * Returns the input stream of the file submitted from an HTML form, or null.
     * @return an {@link InputStream} instance, or {@code null}
     */
    public InputStream getFileData() {
        return fileData;
    }

    /**
     * Sets the {@link InputStream} which corresponds to the HTML element named {@code file}. RestEASY will call this method.
     *
     * @param fileData a {@link InputStream} or {@code null} if there isn't an HTML field with the {@code file} name.
     */
    @FormParam(PARAM_NAME)
    public void setFileData( InputStream fileData ) {
        this.fileData = fileData;
    }

    /**
     * Validates that the {@link FileUploadForm#fileData} field is not null.
     *
     * @throws IllegalArgumentException if the fileData field is null.
     */
    public void validate() {
        if (fileData == null) {
            throw new IllegalArgumentException(
                    "Please make sure the file is uploaded from an HTML element with the name \"file\"");
        }
    }
}
