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
