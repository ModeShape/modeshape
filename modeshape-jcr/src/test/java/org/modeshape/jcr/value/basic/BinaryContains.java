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
package org.modeshape.jcr.value.basic;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.jcr.RepositoryException;
import junit.framework.AssertionFailedError;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.value.BinaryValue;

/**
 * @author Randall Hauch
 */
public class BinaryContains extends TypeSafeMatcher<BinaryValue> {

    private byte[] expectedContent;

    public BinaryContains( byte[] expectedContent ) {
        this.expectedContent = expectedContent;
    }

    public BinaryContains( BinaryValue expectedContent ) {
        try {
            this.expectedContent = IoUtil.readBytes(expectedContent.getStream());
        } catch (RepositoryException e) {
            throw new AssertionFailedError(e.getMessage());
        } catch (IOException e) {
            throw new AssertionFailedError(e.getMessage());
        } finally {
            expectedContent.dispose();
        }
    }

    @Override
    public boolean matchesSafely( BinaryValue content ) {
        try {
            byte[] actualContents = IoUtil.readBytes(content.getStream());
            if (actualContents.length != expectedContent.length) return false;
            for (int i = 0, len = actualContents.length; i != len; ++i) {
                if (actualContents[i] != expectedContent[i]) return false;
            }
            return true;
        } catch (RepositoryException e) {
            throw new AssertionFailedError(e.getMessage());
        } catch (IOException e) {
            throw new AssertionFailedError(e.getMessage());
        } finally {
            content.dispose();
        }
    }

    protected String getDisplayableString( byte[] values ) {
        StringBuilder sb = new StringBuilder("byte[]: ");
        sb.append(" len=").append(values.length).append("; [");
        int len = (int)Math.min(values.length, 20l);
        for (int i = 0; i != len; ++i) {
            if (i != 0) sb.append(',');
            sb.append(values[i]);
        }
        return sb.toString();
    }

    @Override
    public void describeTo( Description description ) {
        description.appendText("a binary containing ").appendValue(getDisplayableString(expectedContent));
    }

    @Factory
    public static Matcher<BinaryValue> hasContent( BinaryValue expectedContent ) {
        return new BinaryContains(expectedContent);
    }

    @Factory
    public static Matcher<BinaryValue> hasContent( String expectedContent ) throws UnsupportedEncodingException {
        return new BinaryContains(expectedContent.getBytes("UTF-8"));
    }

    @Factory
    public static Matcher<BinaryValue> hasContent( byte[] expectedContent ) {
        return new BinaryContains(expectedContent);
    }

    @Factory
    public static Matcher<BinaryValue> hasNoContent() {
        return new BinaryContains(new byte[0]);
    }

}
