/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.eclipse.jcr.rest.client;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.PLUGIN_ID;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;
import java.util.TreeSet;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;

public final class Utils {

    // ===========================================================================================================================
    // Class Methods
    // ===========================================================================================================================

    /**
     * @param tokens the tokens being combined into one value
     * @param delimiter the character inserted to separate each token
     * @return the tokens separated by the delimiter
     */
    public static String combineTokens( String[] tokens,
                                        char delimiter ) {
        CheckArg.isNotNull(tokens, "tokens"); //$NON-NLS-1$
        StringBuilder value = new StringBuilder();

        for (String token : tokens) {
            value.append(token).append(delimiter);
        }

        return value.toString();
    }

    /**
     * Sizes the shell to the minimum of it's current size or the width and height display percentages.
     * 
     * @param shell the shell being resized (if necessary) and located
     * @param widthPercentage a number between 1 and 100 indicating a percentage of the screen size (defaults to 50 if bad value)
     * @param heightPercentage a number between 1 and 100 indicating a percentage of the screen size (defaults to 50 if bad value)
     */
    public static void centerAndSizeShellRelativeToDisplay( Shell shell,
                                                            int widthPercentage,
                                                            int heightPercentage ) {
        if ((widthPercentage < 1) || (widthPercentage > 100)) {
            widthPercentage = 50;
        }

        if ((heightPercentage < 1) || (heightPercentage > 100)) {
            heightPercentage = 50;
        }

        // size
        Rectangle shellBounds = shell.getBounds();
        Rectangle displayBounds = shell.getDisplay().getClientArea();
        int scaledWidth = displayBounds.width * widthPercentage / 100;
        int scaledHeight = displayBounds.height * heightPercentage / 100;
        shell.setSize(Math.min(scaledWidth, shellBounds.width), Math.min(scaledHeight, shellBounds.height));
        Point size = shell.getSize();

        // center
        int excessX = displayBounds.width - size.x;
        int excessY = displayBounds.height - size.y;
        int x = displayBounds.x + (excessX / 2);
        int y = displayBounds.y + (excessY / 2);

        shell.setLocation(x, y);
    }

    /**
     * Converts the non-Eclipse status severity to an Eclipse severity level. An {@link Status.Severity#UNKNOWN unknown status} is
     * converted to {@link IStatus#CANCEL cancel}.
     * 
     * @param severity the eclipse status severity level
     * @return the converted severity level (never <code>null</code>)
     * @see IStatus
     */
    public static int convertSeverity( Severity severity ) {
        if (severity == Severity.OK) return IStatus.OK;
        if (severity == Severity.ERROR) return IStatus.ERROR;
        if (severity == Severity.WARNING) return IStatus.WARNING;
        if (severity == Severity.INFO) return IStatus.INFO;
        return IStatus.CANCEL;
    }

    /**
     * Converts the Eclipse status severity level to a non-Eclipse severity.
     * 
     * @param severity the eclipse status severity level
     * @return the converted severity level (never <code>null</code>)
     * @see IStatus
     */
    public static Severity convertSeverity( int severity ) {
        if (severity == IStatus.OK) return Severity.OK;
        if (severity == IStatus.ERROR) return Severity.ERROR;
        if (severity == IStatus.WARNING) return Severity.WARNING;
        if (severity == IStatus.INFO) return Severity.INFO;
        return Severity.UNKNOWN;
    }

    /**
     * @param status the status being converted (never <code>null</code>)
     * @return the Eclipse status object (never <code>null</code>)
     */
    public static IStatus convert( Status status ) {
        CheckArg.isNotNull(status, "status"); //$NON-NLS-1$
        return new org.eclipse.core.runtime.Status(convertSeverity(status.getSeverity()), PLUGIN_ID, status.getMessage(),
                                                   status.getException());
    }

    /**
     * The OK status does not have an image.
     * 
     * @param status the status whose image is being requested (never <code>null</code>)
     * @return the image or <code>null</code> if no associated image for the status severity
     */
    public static Image getImage( Status status ) {
        CheckArg.isNotNull(status, "status"); //$NON-NLS-1$
        String imageId = null;

        if (status.isError()) {
            imageId = ISharedImages.IMG_OBJS_ERROR_TSK;
        } else if (status.isInfo()) {
            imageId = ISharedImages.IMG_OBJS_INFO_TSK;
        } else if (status.isWarning()) {
            imageId = ISharedImages.IMG_OBJS_WARN_TSK;
        }

        if (imageId != null) {
            return Activator.getDefault().getSharedImage(imageId);
        }

        return null;
    }

    /**
     * @param string the string whose tokens are being requested (may be <code>null</code>)
     * @param delimiters the delimiters that separate the tokens (never <code>null</code>)
     * @param removeDuplicates a flag indicating if duplicate tokens should be removed
     * @return the tokens (never <code>null</code>)
     */
    public static String[] getTokens( String string,
                                      String delimiters,
                                      boolean removeDuplicates ) {
        CheckArg.isNotNull(delimiters, "delimiters"); //$NON-NLS-1$

        if (string == null) {
            return new String[0];
        }

        Collection<String> tokens = removeDuplicates ? new TreeSet<String>() : new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(string, delimiters);

        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }

        return tokens.toArray(new String[tokens.size()]);
    }

    /**
     * The image can be used to decorate an existing image.
     * 
     * @param status the status whose image overlay is being requested (never <code>null</code>)
     * @return the image descriptor or <code>null</code> if none found for the status severity
     */
    public static ImageDescriptor getOverlayImage( Status status ) {
        CheckArg.isNotNull(status, "status"); //$NON-NLS-1$
        String imageId = null;

        if (status.isError()) {
            imageId = IUiConstants.ERROR_OVERLAY_IMAGE;
        }

        if (imageId != null) {
            return Activator.getDefault().getImageDescriptor(imageId);
        }

        return null;
    }

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * Don't allow construction.
     */
    public Utils() {
        // nothing to do
    }

}
